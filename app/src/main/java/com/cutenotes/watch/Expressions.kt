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
