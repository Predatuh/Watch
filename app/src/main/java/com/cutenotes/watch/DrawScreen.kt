package com.cutenotes.watch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

/** A finished stroke: its color, thickness, and the points it traced. */
data class DrawnStroke(val color: Color, val width: Float, val points: List<Offset>)

/** While drawing, a stroke whose point list grows as the finger moves. */
private class LiveStroke(val color: Color, val width: Float) {
    val points = mutableStateListOf<Offset>()
}

private val INK_COLORS = listOf(
    Color(0xFFFF6B9D), // pink
    Color(0xFFFF4D4D), // red
    Color(0xFFFFD166), // gold
    Color(0xFF6BCB77), // green
    Color(0xFF4D96FF), // blue
    Color(0xFFFFFFFF), // white
)

// label + stroke width for the three brush sizes
private val BRUSHES = listOf(6f, 11f, 17f)

/**
 * Finger-painting screen, clipped to a circle so it matches the round watch
 * face. Pick a color and brush size, undo, clear, or tap ✓ to send the drawing
 * (which then replays as a note).
 */
@Composable
fun DrawScreen(onSend: (List<DrawnStroke>) -> Unit, onCancel: () -> Unit) {
    val strokes = remember { mutableStateListOf<LiveStroke>() }
    var colorIndex by remember { mutableIntStateOf(0) }
    var brushIndex by remember { mutableIntStateOf(1) }

    val activeColor = INK_COLORS[colorIndex]
    val activeWidth = BRUSHES[brushIndex]

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // Round drawing surface.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color(0xFF101015))
                .pointerInput(activeColor, activeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            strokes.add(LiveStroke(activeColor, activeWidth).also { it.points.add(offset) })
                        },
                        onDrag = { change, _ ->
                            strokes.lastOrNull()?.points?.add(change.position)
                            change.consume()
                        },
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF2E2E3A),
                    radius = size.minDimension / 2f - 1f,
                    style = Stroke(width = 2f),
                )
                strokes.forEach { drawStroke(it.color, it.width, it.points) }
            }
        }

        // Color swatches across the top.
        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            INK_COLORS.forEachIndexed { i, color ->
                Box(
                    modifier = Modifier
                        .size(if (i == colorIndex) 20.dp else 16.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (i == colorIndex) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape,
                        )
                        .clickable { colorIndex = i },
                )
            }
        }

        // Tools across the bottom: undo, brush size, clear, done. Compact custom
        // buttons + a higher position so they stay inside the round screen.
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolButton("↶") { strokes.removeLastOrNull() }
            ToolButton(when (brushIndex) { 0 -> "·"; 1 -> "•"; else -> "●" }) {
                brushIndex = (brushIndex + 1) % BRUSHES.size
            }
            ToolButton("🗑") { strokes.clear() }
            ToolButton("✓", highlight = true) {
                if (strokes.isEmpty()) {
                    onCancel()
                } else {
                    onSend(strokes.map { DrawnStroke(it.color, it.width, it.points.toList()) })
                }
            }
        }
    }
}

/** A small round icon button sized to fit comfortably on a watch face. */
@Composable
private fun ToolButton(label: String, highlight: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (highlight) Color(0xFFFF6B9D) else Color(0xFF26262E))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 16.sp, color = Color.White)
    }
}

/**
 * Replays a sent drawing: the strokes draw themselves on in order, then hold.
 * This is how a hand-drawn note will look when it arrives on a partner's watch.
 */
@Composable
fun DrawnNotePlayer(strokes: List<DrawnStroke>, incoming: Boolean, peer: String, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1500))
        delay(2500)
        onDismiss()
    }

    val totalPoints = remember(strokes) { strokes.sumOf { it.points.size }.coerceAtLeast(1) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0E0A12)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var allowed = (progress.value * totalPoints).toInt()
            for (stroke in strokes) {
                if (allowed <= 0) break
                val shown = minOf(allowed, stroke.points.size)
                drawStroke(stroke.color, stroke.width, stroke.points.subList(0, shown))
                allowed -= stroke.points.size
            }
        }

        Text(
            text = if (incoming) "From $peer 💕" else "Sent to $peer 💌",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
        )
    }
}

/** Draws one stroke as a smooth rounded poly-line. */
private fun DrawScope.drawStroke(color: Color, width: Float, points: List<Offset>) {
    when {
        points.size > 1 -> {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        points.size == 1 -> drawCircle(color, radius = width / 2f, center = points.first())
    }
}
