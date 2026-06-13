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
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.tasks.await

/**
 * Real delivery via Firebase. Anonymous auth gives each watch a stable id; the
 * user claims a unique username; adding a friend by username links you both;
 * sending writes a note to that friend's inbox; a live listener delivers notes.
 *
 * Firestore layout:
 *   usernames/{username}            -> { uid }            (claimed, unique)
 *   users/{uid}                     -> { username }
 *   users/{uid}/friends/{friendUid} -> { username }
 *   notes/{uid}/inbox/{auto}        -> { kind, ..., fromName, createdAt }
 */
object FirebaseNoteTransport : NoteTransport {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val isDemo: Boolean = false

    private var uid: String? = null
    override var initialized by mutableStateOf(false)
        private set
    override var pendingNotes by mutableStateOf<List<PendingNote>>(emptyList())
        private set
    override var isSignedIn by mutableStateOf(false)
        private set
    override var myUsername by mutableStateOf<String?>(null)
        private set
    override var statusText by mutableStateOf("Connecting…")
        private set
    override var friends by mutableStateOf<List<Friend>>(emptyList())
        private set

    private var inboxListener: ListenerRegistration? = null
    private var friendsListener: ListenerRegistration? = null

    override suspend fun initialize() {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            onAuthenticated(user.uid)
        } else {
            statusText = "Sign in to start"
        }
        initialized = true
    }

    override suspend fun signUp(email: String, password: String): String? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            result.user?.uid?.let { onAuthenticated(it) }
            null
        } catch (e: Exception) {
            authError(e)
        }
    }

    override suspend fun signIn(email: String, password: String): String? {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            result.user?.uid?.let { onAuthenticated(it) }
            null
        } catch (e: Exception) {
            authError(e)
        }
    }

    override suspend fun signOut() {
        inboxListener?.remove()
        friendsListener?.remove()
        auth.signOut()
        uid = null
        isSignedIn = false
        myUsername = null
        friends = emptyList()
        statusText = "Signed out"
    }

    private suspend fun onAuthenticated(id: String) {
        uid = id
        try {
            myUsername = db.collection("users").document(id).get().await().getString("username")
            startFriendsListener(id)
            if (notesDeliveredHere) {
                startInboxListener(id)
                registerPushToken(id)
            }
            statusText = if (myUsername == null) "Choose a username" else "Connected"
        } catch (e: Exception) {
            statusText = "Offline: ${e.message?.take(40) ?: "error"}"
        }
        isSignedIn = true
    }

    private fun authError(e: Exception): String {
        val m = e.message ?: "Something went wrong"
        return when {
            m.contains("password is invalid", true) || m.contains("INVALID_LOGIN", true) ||
                m.contains("credential is incorrect", true) -> "Wrong email or password"
            m.contains("no user record", true) || m.contains("USER_NOT_FOUND", true) -> "No account for that email"
            m.contains("email address is already", true) || m.contains("EMAIL_EXISTS", true) -> "Email already in use"
            m.contains("badly formatted", true) -> "Enter a valid email"
            m.contains("at least 6", true) || m.contains("WEAK_PASSWORD", true) -> "Password needs 6+ characters"
            m.contains("OPERATION_NOT_ALLOWED", true) || m.contains("not allowed", true) ->
                "Email sign-in isn't enabled yet"
            m.contains("network", true) -> "No internet connection"
            else -> m.take(60)
        }
    }

    override suspend fun setUsername(name: String): UsernameResult {
        val me = uid ?: return UsernameResult.ERROR
        if (!isValidUsername(name)) return UsernameResult.INVALID
        val wanted = normalizeUsername(name)
        if (wanted == myUsername) return UsernameResult.OK
        return try {
            val claimed = db.runTransaction { tx ->
                val ref = db.collection("usernames").document(wanted)
                val snap = tx.get(ref)
                if (snap.exists() && snap.getString("uid") != me) {
                    false
                } else {
                    tx.set(ref, mapOf("uid" to me))
                    tx.set(
                        db.collection("users").document(me),
                        mapOf("username" to wanted),
                        SetOptions.merge(),
                    )
                    true
                }
            }.await()
            if (claimed != true) {
                statusText = "Username taken"
                return UsernameResult.TAKEN
            }
            // Release the old username, if we're changing it.
            val old = myUsername
            if (old != null && old != wanted) {
                runCatching { db.collection("usernames").document(old).delete().await() }
            }
            myUsername = wanted
            statusText = "Connected"
            UsernameResult.OK
        } catch (e: Exception) {
            statusText = "Error: ${e.message?.take(30) ?: "failed"}"
            UsernameResult.ERROR
        }
    }

    /** Store this device's push token so the Cloud Function can reach it. */
    private suspend fun registerPushToken(id: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            db.collection("users").document(id)
                .set(mapOf("fcmToken" to token), SetOptions.merge()).await()
        }
    }

    private fun startFriendsListener(id: String) {
        friendsListener?.remove()
        friendsListener = db.collection("users").document(id).collection("friends")
            .addSnapshotListener { snaps, _ ->
                if (snaps == null) return@addSnapshotListener
                friends = snaps.documents.map { d ->
                    Friend(
                        uid = d.id,
                        username = d.getString("username") ?: "friend",
                        streak = (d.getLong("streak") ?: 0L).toInt(),
                    )
                }
            }
    }

    private fun startInboxListener(id: String) {
        inboxListener?.remove()
        inboxListener = db.collection("notes").document(id).collection("inbox")
            .addSnapshotListener { snaps, _ ->
                if (snaps == null) return@addSnapshotListener
                // The inbox docs ARE the queue of unseen notes. We retain them as
                // state (no fragile flow) and only delete when the watch shows one.
                pendingNotes = snaps.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val payload = mapToPayload(data) ?: return@mapNotNull null
                    PendingNote(doc.id, doc.getString("fromName") ?: "friend", payload)
                }
            }
    }

    override suspend fun consumeNote(id: String) {
        // Remove locally right away (snappy), then delete from the inbox.
        pendingNotes = pendingNotes.filterNot { it.id == id }
        val me = uid ?: return
        runCatching { db.collection("notes").document(me).collection("inbox").document(id).delete().await() }
    }

    override suspend fun addFriend(username: String): Friend? {
        val me = uid ?: return null
        val name = normalizeUsername(username)
        if (name.isEmpty()) {
            statusText = "Enter a username"
            return null
        }
        return try {
            val theirUid = db.collection("usernames").document(name).get().await().getString("uid")
            if (theirUid == null) {
                statusText = "No user @$name"
                return null
            }
            val mine = myUsername ?: "friend"
            // Add them to my friends...
            db.collection("users").document(me).collection("friends").document(theirUid)
                .set(mapOf("username" to name), SetOptions.merge()).await()
            // ...and add me to theirs, so they can send back.
            db.collection("users").document(theirUid).collection("friends").document(me)
                .set(mapOf("username" to mine), SetOptions.merge()).await()
            statusText = "Added @$name"
            Friend(theirUid, name)
        } catch (e: Exception) {
            statusText = "Add failed: ${e.message?.take(30) ?: "error"}"
            null
        }
    }

    override suspend fun send(toUid: String, payload: NotePayload) {
        try {
            val data = payloadToMap(payload).toMutableMap()
            data["fromName"] = myUsername ?: "friend"
            data["fromUid"] = uid ?: ""
            data["summary"] = noteSummary(payload)
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
        is NotePayload.TextNote -> mapOf(
            "kind" to "text",
            "text" to payload.text,
            "effect" to payload.effect.name,
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
        "text" -> {
            val text = data["text"] as? String ?: return null
            val effect = runCatching { Effect.valueOf(data["effect"] as? String ?: "NONE") }.getOrDefault(Effect.NONE)
            NotePayload.TextNote(text, effect)
        }
        else -> null
    }
}
