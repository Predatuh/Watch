package com.cutenotes.watch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Text

/** One drawn line: the points the finger moved through. */
private typealias DrawStroke = MutableList<Offset>

/**
 * A finger-painting screen. The drawing area is clipped to a circle so it
 * matches the round watch face — you literally cannot draw in the corners,
 * which is exactly what the user asked for. Later, the captured strokes can be
 * sent to a partner the same way an expression is.
 */
@Composable
fun DrawScreen(onClose: () -> Unit) {
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    val inkColor = Color(0xFFFF6B9D)

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // The round canvas you draw on.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF101015))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> strokes.add(mutableListOf(offset)) },
                        onDrag = { change, _ ->
                            strokes.lastOrNull()?.add(change.position)
                            change.consume()
                        },
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Faint ring showing the round edge.
                drawCircle(
                    color = Color(0xFF2E2E3A),
                    radius = size.minDimension / 2f - 1f,
                    style = Stroke(width = 2f),
                )
                // All the strokes drawn so far.
                strokes.forEach { points ->
                    when {
                        points.size > 1 -> {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = inkColor,
                                style = Stroke(
                                    width = 9f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                        }
                        points.size == 1 -> drawCircle(inkColor, radius = 4.5f, center = points.first())
                    }
                }
            }
        }

        // Hint at the top.
        if (strokes.isEmpty()) {
            Text(
                text = "✏️ Draw a note",
                color = Color(0xFFBBBBC4),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            )
        }

        // Controls at the bottom: clear everything, or finish.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            CompactButton(onClick = { strokes.clear() }) { Text("🗑") }
            CompactButton(onClick = onClose) { Text("✓") }
        }
    }
}
