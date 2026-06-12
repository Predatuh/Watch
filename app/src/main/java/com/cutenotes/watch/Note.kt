package com.cutenotes.watch

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * What a note actually contains. This is the unit that travels between two
 * watches. Keeping it small and serializable-friendly means the Firebase
 * transport later just has to turn this into JSON and back.
 */
sealed interface NotePayload {
    data class ExpressionNote(val expressionId: String) : NotePayload
    data class FireworkNote(val type: FireworkType) : NotePayload
    data class DrawingNote(val strokes: List<DrawnStroke>) : NotePayload
}

/** A note that arrived from someone. */
data class IncomingNote(val from: String, val payload: NotePayload)

/** Someone on your friends list. [code] is their shareable friend code. */
data class Friend(val uid: String, val code: String, val name: String)

/**
 * Sends notes to your partner and reports notes that arrive. Swapping the demo
 * implementation for a real Firebase one later is just a matter of providing a
 * different object here — the rest of the app is written against this interface.
 */
interface NoteTransport {
    val isDemo: Boolean
    val incoming: SharedFlow<IncomingNote>

    /** Sign in / connect. No-op for the demo transport. */
    suspend fun initialize() {}

    /** Your shareable friend code (null until connected). */
    val myCode: String? get() = null

    /** Short human-readable connection status. */
    val statusText: String get() = if (isDemo) "Demo mode" else "Connecting…"

    /** Everyone on your friends list. */
    val friends: List<Friend> get() = emptyList()

    /** Add a friend by their code. Returns the friend on success, null otherwise. */
    suspend fun addFriend(code: String): Friend?

    /** Send a note to a specific friend (by their uid). */
    suspend fun send(toUid: String, payload: NotePayload)
}

/**
 * Demo transport with no backend: each note you "send" is echoed back a few
 * seconds later as if your partner received it and replied, so you can see the
 * full receive experience (banner, buzz, full-screen play) on a single watch.
 */
object LoopbackTransport : NoteTransport {
    override val isDemo: Boolean = true
    override val myCode: String = "DEMO42"
    override val friends: List<Friend> = listOf(Friend("demo-alex", "ALEX99", "Alex"))

    private val _incoming = MutableSharedFlow<IncomingNote>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<IncomingNote> = _incoming.asSharedFlow()

    override suspend fun addFriend(code: String): Friend? = Friend("demo-$code", code, code)

    override suspend fun send(toUid: String, payload: NotePayload) {
        delay(5000)
        _incoming.emit(IncomingNote("Alex", payload))
    }
}

/** The transport the app currently uses. Swap to LoopbackTransport for offline demo. */
val transport: NoteTransport = FirebaseNoteTransport
