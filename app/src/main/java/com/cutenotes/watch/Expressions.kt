package com.cutenotes.watch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlin.math.sin

/**
 * One sendable "cute note". For now these are the building blocks we show on the
 * watch. Later, the exact same data is what one watch will send to another.
 *
 * @param id      stable identifier (used later when sending over the network)
 * @param emoji   the big animated character shown on screen
 * @param label   short caption under the picker tile
 * @param accent  the theme color for this expression's full-screen background
 * @param style   how the emoji should move
 * @param particle optional little emoji that floats in the background
 */
data class Expression(
    val id: String,
    val emoji: String,
    val label: String,
    val accent: Color,
    val style: AnimStyle,
    val particle: String? = null,
)

/** The different ways an expression's emoji can animate. */
enum class AnimStyle { PULSE, BOB, WIGGLE, SPIN, FLOAT }

/** The full set of notes you can send. Add to this list to create new ones. */
val expressions: List<Expression> = listOf(
    Expression("love", "❤️", "Love you", Color(0xFFFF4D7E), AnimStyle.PULSE, particle = "💞"),
    Expression("kiss", "😘", "Kiss", Color(0xFFFF7BA9), AnimStyle.BOB, particle = "💋"),
    Expression("hug", "🤗", "Hug", Color(0xFFFFA45B), AnimStyle.WIGGLE, particle = "✨"),
    Expression("excited", "🤩", "So excited!", Color(0xFFB36BFF), AnimStyle.SPIN, particle = "⭐"),
    Expression("miss", "🥺", "Miss you", Color(0xFF5B8DEF), AnimStyle.BOB, particle = "💧"),
    Expression("sleepy", "😴", "Goodnight", Color(0xFF6C6BBF), AnimStyle.FLOAT, particle = "💤"),
    Expression("cheer", "🎉", "You got this", Color(0xFF2EC4B6), AnimStyle.WIGGLE, particle = "✨"),
    Expression("flower", "🌸", "Thinking of you", Color(0xFFEF6FB3), AnimStyle.PULSE, particle = "🌸"),
)

/**
 * Plays a single expression full-screen: a colored background, floating
 * background particles, and the big animated emoji. This is what a received
 * note will look like when it arrives on the watch.
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

        // Floating particles behind the emoji
        if (expression.particle != null) {
            repeat(5) { i ->
                FloatingParticle(text = expression.particle, index = i)
            }
        }

        // The main animated emoji
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

/** A small emoji that drifts upward and fades, looping forever, staggered by index. */
@Composable
private fun FloatingParticle(text: String, index: Int) {
    val transition = rememberInfiniteTransition(label = "particle$index")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            initialStartOffset = StartOffset(offsetMillis = index * 520),
            repeatMode = RepeatMode.Restart,
        ),
        label = "p$index",
    )

    // Spread the particles horizontally and sway them as they rise.
    val xSpread = (index - 2) * 34f
    val sway = sin((progress + index) * 3.14159f) * 18f

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontSize = 22.sp,
            modifier = Modifier
                .alpha((1f - progress).coerceIn(0f, 1f) * 0.9f)
                .graphicsLayer {
                    translationX = xSpread + sway
                    translationY = 120f - progress * 260f
                },
        )
    }
}
