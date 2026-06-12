package com.cutenotes.watch

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * What a note actually contains — the unit that travels between two watches.
 */
sealed interface NotePayload {
    data class ExpressionNote(val expressionId: String) : NotePayload
    data class FireworkNote(val type: FireworkType) : NotePayload
    data class DrawingNote(val strokes: List<DrawnStroke>) : NotePayload
}

/** A note that arrived from someone (by their username). */
data class IncomingNote(val from: String, val payload: NotePayload)

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
    val incoming: SharedFlow<IncomingNote>

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

    private val _incoming = MutableSharedFlow<IncomingNote>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<IncomingNote> = _incoming.asSharedFlow()

    override suspend fun setUsername(name: String): UsernameResult {
        if (!isValidUsername(name)) return UsernameResult.INVALID
        myUsername = normalizeUsername(name)
        return UsernameResult.OK
    }

    override suspend fun addFriend(username: String): Friend? =
        Friend("demo-${normalizeUsername(username)}", normalizeUsername(username))

    override suspend fun send(toUid: String, payload: NotePayload) {
        delay(5000)
        _incoming.emit(IncomingNote("alex", payload))
    }
}

/** The transport the app currently uses. Swap to LoopbackTransport for offline demo. */
val transport: NoteTransport = FirebaseNoteTransport
