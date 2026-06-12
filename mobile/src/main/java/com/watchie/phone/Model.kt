package com.watchie.phone

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await

// ---- Note payloads (must match the watch app's Firestore format) ----

sealed interface NotePayload {
    data class ExpressionNote(val expressionId: String) : NotePayload
    data class FireworkNote(val type: FireworkType) : NotePayload
    data class DrawingNote(val strokes: List<DrawnStroke>) : NotePayload
}

data class IncomingNote(val from: String, val payload: NotePayload)
data class Friend(val uid: String, val username: String, val streak: Int = 0)
data class DrawnStroke(val color: Color, val width: Float, val points: List<Offset>)
enum class UsernameResult { OK, TAKEN, INVALID, ERROR }

fun normalizeUsername(raw: String): String = raw.trim().lowercase()
fun isValidUsername(raw: String): Boolean =
    normalizeUsername(raw).matches(Regex("^[a-z0-9_]{3,15}$"))

// ---- Expressions (the sendable notes) ----

enum class AnimStyle { PULSE, BOB, WIGGLE, SPIN, FLOAT }
enum class Effect { NONE, FIREWORKS, CONFETTI, HEARTS, PETALS, SPARKLE, BUBBLES, SNOW, STARBURST }

data class Expression(
    val id: String,
    val emoji: String,
    val label: String,
    val accent: Color,
)

val expressions: List<Expression> = listOf(
    Expression("love", "❤️", "Love you", Color(0xFFFF4D7E)),
    Expression("amazing", "😍", "You're amazing", Color(0xFFFF6FA5)),
    Expression("kiss", "😘", "Kiss", Color(0xFFFF7BA9)),
    Expression("excited", "🤩", "So excited!", Color(0xFFB36BFF)),
    Expression("party", "🥳", "Let's celebrate", Color(0xFFFF5DA2)),
    Expression("cheer", "🎉", "You got this", Color(0xFF2EC4B6)),
    Expression("laugh", "😂", "You crack me up", Color(0xFFFFB300)),
    Expression("flower", "🌸", "Thinking of you", Color(0xFFEF6FB3)),
    Expression("rainbow", "🌈", "Brighten your day", Color(0xFF7C6BFF)),
    Expression("proud", "🥹", "So proud of you", Color(0xFF38B6FF)),
    Expression("magic", "✨", "A little magic", Color(0xFF9B5DE5)),
    Expression("star", "⭐", "You're a star", Color(0xFFF6C453)),
    Expression("hug", "🤗", "Sending a hug", Color(0xFFFFA45B)),
    Expression("morning", "☀️", "Good morning", Color(0xFFFFB300)),
    Expression("cool", "😎", "Looking good", Color(0xFF12B5C9)),
    Expression("miss", "🥺", "Miss you", Color(0xFF5B8DEF)),
    Expression("sleepy", "😴", "Goodnight", Color(0xFF6C6BBF)),
    Expression("cozy", "⛄", "Stay cozy", Color(0xFF4F86C6)),
    Expression("adore", "🥰", "Adore you", Color(0xFFFF5C8A)),
    Expression("rose", "🌹", "For you", Color(0xFFE63956)),
    Expression("birthday", "🎂", "Happy birthday", Color(0xFFFF7AC8)),
    Expression("thanks", "🙏", "Thank you", Color(0xFF59C3C3)),
    Expression("music", "🎵", "Our song", Color(0xFF8E7CFF)),
    Expression("moon", "🌙", "Sweet dreams", Color(0xFF3D5A99)),
)

fun expressionById(id: String): Expression =
    expressions.firstOrNull { it.id == id } ?: expressions.first()

enum class FireworkType(val label: String) {
    PEONY("Peony"), CHRYSANTHEMUM("Chrysanthemum"), WILLOW("Willow"),
    CRACKLE("Crackle"), FIRECRACKERS("Firecrackers"), STROBE("Strobe"),
    RING("Ring"), RAINBOW("Rainbow"),
}

val fireworkTypes: List<FireworkType> = FireworkType.entries

// ---- Transport ----

interface NoteTransport {
    val isDemo: Boolean
    val incoming: SharedFlow<IncomingNote>
    suspend fun initialize() {}
    val initialized: Boolean get() = true
    val isSignedIn: Boolean get() = true
    suspend fun signUp(email: String, password: String): String? = null
    suspend fun signIn(email: String, password: String): String? = null
    suspend fun signOut() {}
    val myUsername: String? get() = null
    val statusText: String get() = "Connecting…"
    val friends: List<Friend> get() = emptyList()
    suspend fun setUsername(name: String): UsernameResult
    suspend fun addFriend(username: String): Friend?
    suspend fun send(toUid: String, payload: NotePayload)
}

/** The transport the phone app uses. */
val transport: NoteTransport = FirebaseNoteTransport

// ---- Update check (shared config/app doc with the watch) ----

data class UpdateInfo(val available: Boolean, val message: String)

object UpdateChecker {
    suspend fun check(currentVersionCode: Int): UpdateInfo {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("config").document("app").get().await()
            val latest = (doc.getLong("latestVersionCode") ?: 0L).toInt()
            val message = doc.getString("updateMessage")
                ?: "A new version of Watchie is available."
            UpdateInfo(latest > currentVersionCode, message)
        } catch (e: Exception) {
            UpdateInfo(false, "")
        }
    }
}
