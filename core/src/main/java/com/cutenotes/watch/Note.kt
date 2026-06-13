package com.cutenotes.watch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What a note actually contains — the unit that travels between two watches.
 */
sealed interface NotePayload {
    data class ExpressionNote(val expressionId: String) : NotePayload
    data class FireworkNote(val type: FireworkType) : NotePayload
    data class DrawingNote(val strokes: List<DrawnStroke>) : NotePayload
    data class TextNote(val text: String, val effect: Effect) : NotePayload
}

/** A note waiting in the inbox: [id] is the Firestore doc id (delete when seen). */
data class PendingNote(val id: String, val from: String, val payload: NotePayload)

/** A short human-readable summary of a note (used in the push notification). */
fun noteSummary(payload: NotePayload): String = when (payload) {
    is NotePayload.ExpressionNote -> expressionById(payload.expressionId).let { "${it.emoji} ${it.label}" }
    is NotePayload.FireworkNote -> "🎆 Fireworks"
    is NotePayload.TextNote -> payload.text
    is NotePayload.DrawingNote -> "✏️ A drawing"
}

/** Someone on your friends list. [streak] = consecutive days you've both messaged. */
data class Friend(val uid: String, val username: String, val streak: Int = 0)

/** Result of trying to claim a username. */
enum class UsernameResult { OK, TAKEN, INVALID, ERROR }

/** Usernames are lowercase, 3–15 chars, letters/digits/underscore. */
fun normalizeUsername(raw: String): String = raw.trim().lowercase()

fun isValidUsername(raw: String): Boolean =
    normalizeUsername(raw).matches(Regex("^[a-z0-9_]{3,15}$"))

/**
 * Sends notes to friends and reports notes that arrive. The demo implementation
 * works offline; the Firebase one delivers between real watches.
 */
interface NoteTransport {
    val isDemo: Boolean

    /** Notes waiting to be viewed (the inbox). Shown one at a time, then consumed. */
    val pendingNotes: List<PendingNote> get() = emptyList()

    /** Mark a note as seen (deletes it from the inbox). */
    suspend fun consumeNote(id: String) {}

    /** Sign in / connect. No-op for the demo transport. */
    suspend fun initialize() {}

    /** True once initialize() has finished (so the UI knows when to prompt). */
    val initialized: Boolean get() = true

    /** Whether a real (non-anonymous) account is signed in. */
    val isSignedIn: Boolean get() = true

    /** Create an account. Returns null on success, or an error message. */
    suspend fun signUp(email: String, password: String): String? = null

    /** Log into an existing account. Returns null on success, or an error message. */
    suspend fun signIn(email: String, password: String): String? = null

    /** Sign out of the current account. */
    suspend fun signOut() {}

    /** Your unique username (null until you've chosen one). */
    val myUsername: String? get() = null

    /** Short human-readable connection status. */
    val statusText: String get() = if (isDemo) "Demo mode" else "Connecting…"

    /** Everyone on your friends list. */
    val friends: List<Friend> get() = emptyList()

    /** Claim/change your username. */
    suspend fun setUsername(name: String): UsernameResult

    /** Add a friend by their username. Returns the friend on success. */
    suspend fun addFriend(username: String): Friend?

    /** Send a note to a specific friend (by their uid). */
    suspend fun send(toUid: String, payload: NotePayload)
}

/**
 * Demo transport with no backend: each note you send echoes back a few seconds
 * later so you can see the receive experience on a single watch.
 */
object LoopbackTransport : NoteTransport {
    override val isDemo: Boolean = true
    override val initialized: Boolean = true
    override var myUsername: String? = "you"
    override val friends: List<Friend> = listOf(Friend("demo-alex", "alex", streak = 5))

    override var pendingNotes by mutableStateOf<List<PendingNote>>(emptyList())
        private set

    private var counter = 0

    override suspend fun consumeNote(id: String) {
        pendingNotes = pendingNotes.filterNot { it.id == id }
    }

    override suspend fun setUsername(name: String): UsernameResult {
        if (!isValidUsername(name)) return UsernameResult.INVALID
        myUsername = normalizeUsername(name)
        return UsernameResult.OK
    }

    override suspend fun addFriend(username: String): Friend? =
        Friend("demo-${normalizeUsername(username)}", normalizeUsername(username))

    override suspend fun send(toUid: String, payload: NotePayload) {
        kotlinx.coroutines.delay(4000)
        pendingNotes = pendingNotes + PendingNote("demo-${counter++}", "alex", payload)
    }
}

/** The transport the app currently uses. Swap to LoopbackTransport for offline demo. */
val transport: NoteTransport = FirebaseNoteTransport

/**
 * Whether this device receives/displays notes. The watch keeps this true; the
 * phone sets it false (send-only) so notes are only viewable on the watch and
 * the phone doesn't consume the inbox or claim the single push token.
 */
var notesDeliveredHere: Boolean = true
