package com.cutenotes.watch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlin.math.sin

/**
 * One sendable "cute note". The same data is what one watch will eventually
 * send to another.
 *
 * @param id      stable identifier (used later when sending over the network)
 * @param emoji   the big animated character shown on screen
 * @param label   short caption under the picker tile
 * @param accent  the theme color for this note's full-screen background
 * @param style   how the emoji itself moves
 * @param effect  the animated particle effect played behind the emoji
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
)

/**
 * Plays a single expression full-screen: a colored background, an animated
 * particle effect, and the big animated emoji. This is what a received note
 * looks like when it arrives on the watch.
 */
@Composable
fun ExpressionPlayer(expression: Expression, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "player")

    // A single 0f..1f value that loops forever; each style reads it differently.
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val wave = sin(phase * 2f * Math.PI.toFloat()) // smooth -1..1 oscillation

    val background = Brush.radialGradient(
        colors = listOf(expression.accent, expression.accent.copy(alpha = 0.55f), Color.Black),
    )

    Box(
        modifier = modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center,
    ) {
        // Animated particle effect (fireworks, hearts, petals, ...) behind the emoji.
        EffectLayer(effect = expression.effect, accent = expression.accent, modifier = Modifier.fillMaxSize())

        // The main animated emoji.
        val emojiModifier = when (expression.style) {
            AnimStyle.PULSE -> Modifier.graphicsLayer {
                val s = 1f + 0.18f * wave
                scaleX = s; scaleY = s
            }
            AnimStyle.BOB -> Modifier.graphicsLayer {
                translationY = wave * 26f
            }
            AnimStyle.WIGGLE -> Modifier.graphicsLayer {
                rotationZ = wave * 16f
            }
            AnimStyle.SPIN -> Modifier.graphicsLayer {
                rotationZ = phase * 360f
                val s = 1f + 0.12f * wave
                scaleX = s; scaleY = s
            }
            AnimStyle.FLOAT -> Modifier.graphicsLayer {
                translationY = -wave * 14f
                val s = 1f + 0.08f * wave
                scaleX = s; scaleY = s
            }
        }

        Text(
            text = expression.emoji,
            fontSize = 76.sp,
            textAlign = TextAlign.Center,
            modifier = emojiModifier,
        )
    }
}
