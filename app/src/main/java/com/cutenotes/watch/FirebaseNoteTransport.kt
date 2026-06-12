package com.cutenotes.watch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Real phone-to-phone delivery via Firebase. Anonymous auth gives each watch a
 * stable id; a short pairing code links two of them; notes are written to the
 * recipient's inbox in Firestore and delivered live by a snapshot listener.
 *
 * Firestore layout:
 *   users/{uid}              -> { code, partnerUid }
 *   codes/{CODE}             -> { uid }
 *   notes/{uid}/inbox/{auto} -> { kind, ..., fromName, createdAt }
 */
object FirebaseNoteTransport : NoteTransport {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _incoming = MutableSharedFlow<IncomingNote>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<IncomingNote> = _incoming.asSharedFlow()
    override val isDemo: Boolean = false

    private var uid: String? = null
    private var partnerUid by mutableStateOf<String?>(null)

    override var myCode by mutableStateOf<String?>(null)
        private set
    override var partnerName by mutableStateOf("Partner")
        private set
    override var statusText by mutableStateOf("Connecting…")
        private set
    override val isPaired: Boolean get() = partnerUid != null

    private var listener: ListenerRegistration? = null

    override suspend fun initialize() {
        try {
            val user = auth.currentUser ?: auth.signInAnonymously().await().user
            val id = user?.uid
            if (id == null) {
                statusText = "Sign-in failed"
                return
            }
            uid = id
            ensureProfile(id)
            startListening(id)
            statusText = if (partnerUid != null) "Connected" else "Share your code to pair"
        } catch (e: Exception) {
            statusText = "Offline: ${e.message?.take(40) ?: "error"}"
        }
    }

    private suspend fun ensureProfile(id: String) {
        val userRef = db.collection("users").document(id)
        val snap = userRef.get().await()
        var code = snap.getString("code")
        if (code == null) {
            code = generateCode()
            db.collection("codes").document(code).set(mapOf("uid" to id)).await()
            userRef.set(mapOf("code" to code), SetOptions.merge()).await()
        }
        myCode = code
        partnerUid = snap.getString("partnerUid")
    }

    private fun startListening(id: String) {
        listener?.remove()
        listener = db.collection("notes").document(id).collection("inbox")
            .addSnapshotListener { snaps, _ ->
                if (snaps == null) return@addSnapshotListener
                for (change in snaps.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val doc = change.document
                    val payload = mapToPayload(doc.data) ?: continue
                    val from = doc.getString("fromName") ?: partnerName
                    scope.launch { _incoming.emit(IncomingNote(from, payload)) }
                    doc.reference.delete()
                }
            }
    }

    override suspend fun pairWith(code: String): Boolean {
        val me = uid ?: return false
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) return false
        return try {
            val codeSnap = db.collection("codes").document(clean).get().await()
            val theirUid = codeSnap.getString("uid") ?: run {
                statusText = "Code not found"
                return false
            }
            db.collection("users").document(me)
                .set(mapOf("partnerUid" to theirUid), SetOptions.merge()).await()
            // Mutual link so they can send back to us too.
            db.collection("users").document(theirUid)
                .set(mapOf("partnerUid" to me), SetOptions.merge()).await()
            partnerUid = theirUid
            partnerName = if (theirUid == me) "Yourself (test)" else "Partner"
            statusText = "Connected"
            true
        } catch (e: Exception) {
            statusText = "Pair failed: ${e.message?.take(30) ?: "error"}"
            false
        }
    }

    override suspend fun send(payload: NotePayload) {
        val target = partnerUid ?: run {
            statusText = "Pair with someone first"
            return
        }
        try {
            val data = payloadToMap(payload).toMutableMap()
            data["fromName"] = "Alex"
            data["createdAt"] = FieldValue.serverTimestamp()
            db.collection("notes").document(target).collection("inbox").add(data).await()
        } catch (e: Exception) {
            statusText = "Send failed: ${e.message?.take(30) ?: "error"}"
        }
    }

    // ---- (de)serialization to/from Firestore documents ----

    private fun payloadToMap(payload: NotePayload): Map<String, Any> = when (payload) {
        is NotePayload.ExpressionNote ->
            mapOf("kind" to "expression", "expressionId" to payload.expressionId)
        is NotePayload.FireworkNote ->
            mapOf("kind" to "firework", "fireworkType" to payload.type.name)
        is NotePayload.DrawingNote -> mapOf(
            "kind" to "drawing",
            "strokes" to payload.strokes.map { s ->
                mapOf(
                    "color" to (s.color.toArgb().toLong() and 0xFFFFFFFFL),
                    "width" to s.width.toDouble(),
                    "points" to s.points.flatMap { listOf(it.x.toDouble(), it.y.toDouble()) },
                )
            },
        )
    }

    private fun mapToPayload(data: Map<String, Any?>): NotePayload? = when (data["kind"]) {
        "expression" -> (data["expressionId"] as? String)?.let { NotePayload.ExpressionNote(it) }
        "firework" -> (data["fireworkType"] as? String)?.let { name ->
            fireworkTypes.firstOrNull { it.name == name }?.let { NotePayload.FireworkNote(it) }
        }
        "drawing" -> {
            val rawStrokes = data["strokes"] as? List<*> ?: emptyList<Any>()
            val strokes = rawStrokes.mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                val colorL = (m["color"] as? Number)?.toLong() ?: 0xFFFF6B9DL
                val width = (m["width"] as? Number)?.toFloat() ?: 8f
                val flat = (m["points"] as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toFloat() }
                val points = ArrayList<Offset>()
                var i = 0
                while (i + 1 < flat.size) {
                    points.add(Offset(flat[i], flat[i + 1]))
                    i += 2
                }
                DrawnStroke(Color(colorL.toInt()), width, points)
            }
            NotePayload.DrawingNote(strokes)
        }
        else -> null
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
