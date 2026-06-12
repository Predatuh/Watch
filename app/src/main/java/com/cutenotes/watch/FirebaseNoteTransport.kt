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
 * Real delivery via Firebase. Anonymous auth gives each watch a stable id and a
 * unique friend code. Adding a friend by code links you both; sending writes a
 * note to that friend's inbox; a live listener delivers notes that arrive.
 *
 * Firestore layout:
 *   users/{uid}                   -> { code }
 *   users/{uid}/friends/{friendUid} -> { code, name }
 *   codes/{CODE}                  -> { uid }
 *   notes/{uid}/inbox/{auto}      -> { kind, ..., fromName, createdAt }
 */
object FirebaseNoteTransport : NoteTransport {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _incoming = MutableSharedFlow<IncomingNote>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<IncomingNote> = _incoming.asSharedFlow()
    override val isDemo: Boolean = false

    private var uid: String? = null
    override var myCode by mutableStateOf<String?>(null)
        private set
    override var statusText by mutableStateOf("Connecting…")
        private set
    override var friends by mutableStateOf<List<Friend>>(emptyList())
        private set

    private var inboxListener: ListenerRegistration? = null
    private var friendsListener: ListenerRegistration? = null

    override suspend fun initialize() {
        try {
            val user = auth.currentUser ?: auth.signInAnonymously().await().user
            val id = user?.uid ?: run { statusText = "Sign-in failed"; return }
            uid = id
            ensureProfile(id)
            startInboxListener(id)
            startFriendsListener(id)
            statusText = "Connected"
        } catch (e: Exception) {
            statusText = "Offline: ${e.message?.take(40) ?: "error"}"
        }
    }

    private suspend fun ensureProfile(id: String) {
        val userRef = db.collection("users").document(id)
        val snap = userRef.get().await()
        var code = snap.getString("code")
        if (code == null) {
            code = claimUniqueCode(id)
            userRef.set(mapOf("code" to code), SetOptions.merge()).await()
        }
        myCode = code
    }

    /** Generate a code and atomically claim it; retry on the rare collision. */
    private suspend fun claimUniqueCode(id: String): String {
        repeat(6) {
            val candidate = generateCode()
            val ref = db.collection("codes").document(candidate)
            val claimed = try {
                db.runTransaction { tx ->
                    if (tx.get(ref).exists()) {
                        false
                    } else {
                        tx.set(ref, mapOf("uid" to id))
                        true
                    }
                }.await()
            } catch (e: Exception) {
                false
            }
            if (claimed == true) return candidate
        }
        return generateCode()
    }

    private fun startFriendsListener(id: String) {
        friendsListener?.remove()
        friendsListener = db.collection("users").document(id).collection("friends")
            .addSnapshotListener { snaps, _ ->
                if (snaps == null) return@addSnapshotListener
                friends = snaps.documents.map { d ->
                    val code = d.getString("code") ?: d.id
                    Friend(uid = d.id, code = code, name = d.getString("name") ?: code)
                }
            }
    }

    private fun startInboxListener(id: String) {
        inboxListener?.remove()
        inboxListener = db.collection("notes").document(id).collection("inbox")
            .addSnapshotListener { snaps, _ ->
                if (snaps == null) return@addSnapshotListener
                for (change in snaps.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val doc = change.document
                    val payload = mapToPayload(doc.data) ?: continue
                    val from = doc.getString("fromName") ?: "Friend"
                    scope.launch { _incoming.emit(IncomingNote(from, payload)) }
                    doc.reference.delete()
                }
            }
    }

    override suspend fun addFriend(code: String): Friend? {
        val me = uid ?: return null
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) {
            statusText = "Enter a code"
            return null
        }
        return try {
            val theirUid = db.collection("codes").document(clean).get().await().getString("uid")
            if (theirUid == null) {
                statusText = "Code not found"
                return null
            }
            val myName = myCode ?: "Friend"
            // Add them to my friends...
            db.collection("users").document(me).collection("friends").document(theirUid)
                .set(mapOf("code" to clean, "name" to clean), SetOptions.merge()).await()
            // ...and add me to theirs, so they can send back.
            db.collection("users").document(theirUid).collection("friends").document(me)
                .set(mapOf("code" to myName, "name" to myName), SetOptions.merge()).await()
            statusText = "Added $clean"
            Friend(theirUid, clean, clean)
        } catch (e: Exception) {
            statusText = "Add failed: ${e.message?.take(30) ?: "error"}"
            null
        }
    }

    override suspend fun send(toUid: String, payload: NotePayload) {
        try {
            val data = payloadToMap(payload).toMutableMap()
            data["fromName"] = myCode ?: "Friend"
            data["createdAt"] = FieldValue.serverTimestamp()
            db.collection("notes").document(toUid).collection("inbox").add(data).await()
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
