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

/**
 * Sends notes to your partner and reports notes that arrive. Swapping the demo
 * implementation for a real Firebase one later is just a matter of providing a
 * different object here — the rest of the app is written against this interface.
 */
interface NoteTransport {
    val partnerName: String
    val isDemo: Boolean
    val incoming: SharedFlow<IncomingNote>
    suspend fun send(payload: NotePayload)
}

/**
 * Demo transport with no backend: each note you "send" is echoed back a few
 * seconds later as if your partner received it and replied, so you can see the
 * full receive experience (banner, buzz, full-screen play) on a single watch.
 */
object LoopbackTransport : NoteTransport {
    override val partnerName: String = "Alex"
    override val isDemo: Boolean = true

    private val _incoming = MutableSharedFlow<IncomingNote>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<IncomingNote> = _incoming.asSharedFlow()

    override suspend fun send(payload: NotePayload) {
        delay(5000)
        _incoming.emit(IncomingNote(partnerName, payload))
    }
}

/** The transport the app currently uses. Replace with the Firebase one later. */
val transport: NoteTransport = LoopbackTransport
