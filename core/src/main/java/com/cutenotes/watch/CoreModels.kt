package com.cutenotes.watch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * One sendable "cute note" — the data shared by the watch and phone apps.
 *
 * @param id      stable identifier (used when sending over the network)
 * @param emoji   the big character shown on screen
 * @param label   short caption under the picker tile
 * @param accent  the theme color for this note's full-screen background
 * @param style   how the emoji itself moves (watch only)
 * @param effect  the animated particle effect played behind it
 */
data class Expression(
    val id: String,
    val emoji: String,
    val label: String,
    val accent: Color,
    val style: AnimStyle,
    val effect: Effect,
)

/** The different ways an expression's emoji can animate. */
enum class AnimStyle { PULSE, BOB, WIGGLE, SPIN, FLOAT }

/** A finished drawing stroke: its color, thickness, and traced points. */
data class DrawnStroke(val color: Color, val width: Float, val points: List<Offset>)

fun expressionById(id: String): Expression =
    expressions.firstOrNull { it.id == id } ?: expressions.first()

/** The full set of notes you can send. Add to this list to create new ones. */
val expressions: List<Expression> = listOf(
    Expression("love", "❤️", "Love you", Color(0xFFFF4D7E), AnimStyle.PULSE, Effect.HEARTS),
    Expression("amazing", "😍", "You're amazing", Color(0xFFFF6FA5), AnimStyle.PULSE, Effect.HEARTS),
    Expression("kiss", "😘", "Kiss", Color(0xFFFF7BA9), AnimStyle.BOB, Effect.HEARTS),
    Expression("excited", "🤩", "So excited!", Color(0xFFB36BFF), AnimStyle.SPIN, Effect.FIREWORKS),
    Expression("party", "🥳", "Let's celebrate", Color(0xFFFF5DA2), AnimStyle.WIGGLE, Effect.CONFETTI),
    Expression("cheer", "🎉", "You got this", Color(0xFF2EC4B6), AnimStyle.WIGGLE, Effect.CONFETTI),
    Expression("laugh", "😂", "You crack me up", Color(0xFFFFB300), AnimStyle.WIGGLE, Effect.CONFETTI),
    Expression("flower", "🌸", "Thinking of you", Color(0xFFEF6FB3), AnimStyle.PULSE, Effect.PETALS),
    Expression("rainbow", "🌈", "Brighten your day", Color(0xFF7C6BFF), AnimStyle.BOB, Effect.PETALS),
    Expression("proud", "🥹", "So proud of you", Color(0xFF38B6FF), AnimStyle.BOB, Effect.SPARKLE),
    Expression("magic", "✨", "A little magic", Color(0xFF9B5DE5), AnimStyle.PULSE, Effect.SPARKLE),
    Expression("star", "⭐", "You're a star", Color(0xFFF6C453), AnimStyle.SPIN, Effect.SPARKLE),
    Expression("hug", "🤗", "Sending a hug", Color(0xFFFFA45B), AnimStyle.WIGGLE, Effect.SPARKLE),
    Expression("morning", "☀️", "Good morning", Color(0xFFFFB300), AnimStyle.SPIN, Effect.STARBURST),
    Expression("cool", "😎", "Looking good", Color(0xFF12B5C9), AnimStyle.BOB, Effect.STARBURST),
    Expression("miss", "🥺", "Miss you", Color(0xFF5B8DEF), AnimStyle.BOB, Effect.BUBBLES),
    Expression("sleepy", "😴", "Goodnight", Color(0xFF6C6BBF), AnimStyle.FLOAT, Effect.SNOW),
    Expression("cozy", "⛄", "Stay cozy", Color(0xFF4F86C6), AnimStyle.FLOAT, Effect.SNOW),
    Expression("adore", "🥰", "Adore you", Color(0xFFFF5C8A), AnimStyle.PULSE, Effect.HEARTS),
    Expression("rose", "🌹", "For you", Color(0xFFE63956), AnimStyle.PULSE, Effect.PETALS),
    Expression("birthday", "🎂", "Happy birthday", Color(0xFFFF7AC8), AnimStyle.WIGGLE, Effect.CONFETTI),
    Expression("highfive", "🙌", "High five!", Color(0xFFFFC23C), AnimStyle.WIGGLE, Effect.CONFETTI),
    Expression("thanks", "🙏", "Thank you", Color(0xFF59C3C3), AnimStyle.BOB, Effect.SPARKLE),
    Expression("wink", "😉", "Hey you", Color(0xFFEF8354), AnimStyle.BOB, Effect.SPARKLE),
    Expression("music", "🎵", "Our song", Color(0xFF8E7CFF), AnimStyle.WIGGLE, Effect.SPARKLE),
    Expression("coffee", "☕", "Coffee soon?", Color(0xFFB07D56), AnimStyle.PULSE, Effect.STARBURST),
    Expression("sunset", "🌅", "Wish you were here", Color(0xFFFF7E5F), AnimStyle.PULSE, Effect.STARBURST),
    Expression("moon", "🌙", "Sweet dreams", Color(0xFF3D5A99), AnimStyle.FLOAT, Effect.SNOW),
    Expression("cat", "🐱", "Meow", Color(0xFFB5838D), AnimStyle.BOB, Effect.SPARKLE),
)
