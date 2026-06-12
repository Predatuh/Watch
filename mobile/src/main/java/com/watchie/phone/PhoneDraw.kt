package com.watchie.phone

import com.cutenotes.watch.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class LiveStroke(val color: Color, val width: Float) {
    val points = mutableStateListOf<Offset>()
}

private val INK = listOf(
    Color(0xFFFF4D8D), Color(0xFF9B5DE5), Color(0xFF34E0EA),
    Color(0xFF6BCB77), Color(0xFFFFD166), Color.White,
)

// Brush widths as a fraction of the canvas, so they match on the watch.
private val BRUSHES = listOf(0.02f, 0.04f, 0.07f)

@Composable
fun PhoneDrawScreen(peer: String, onSend: (List<DrawnStroke>) -> Unit, onCancel: () -> Unit) {
    val strokes = remember { mutableStateListOf<LiveStroke>() }
    var colorIndex by remember { mutableIntStateOf(0) }
    var brushIndex by remember { mutableIntStateOf(1) }
    val color = INK[colorIndex]
    val width = BRUSHES[brushIndex]

    ScreenBg {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel) { Text("Cancel", color = TextLo) }
                Text("Draw for @$peer", color = TextHi, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { if (strokes.isNotEmpty()) onSend(strokes.map { DrawnStroke(it.color, it.width, it.points.toList()) }) },
                ) { Text("Send", color = if (strokes.isEmpty()) TextLo else AccentPink, fontWeight = FontWeight.Bold) }
            }

            // Round canvas — matches the watch face, so drawings map exactly.
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 12.dp)
                    .clip(CircleShape).background(Color(0xFF0E0A18))
                    .border(1.dp, CardBorder, CircleShape)
                    .pointerInput(color, width) {
                        val canvas = Size(size.width.toFloat(), size.height.toFloat())
                        detectDragGestures(
                            onDragStart = { o -> strokes.add(LiveStroke(color, width).also { it.points.add(normalizeInSquare(o, canvas)) }) },
                            onDrag = { ch, _ -> strokes.lastOrNull()?.points?.add(normalizeInSquare(ch.position, canvas)); ch.consume() },
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawNormalizedStrokes(strokes.map { DrawnStroke(it.color, it.width, it.points.toList()) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                INK.forEachIndexed { i, c ->
                    Box(
                        modifier = Modifier.size(if (i == colorIndex) 34.dp else 28.dp).clip(CircleShape).background(c)
                            .border(if (i == colorIndex) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { colorIndex = i },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { brushIndex = (brushIndex + 1) % BRUSHES.size }) {
                    Text(when (brushIndex) { 0 -> "Thin"; 1 -> "Medium"; else -> "Thick" }, color = TextHi)
                }
                TextButton(onClick = { strokes.removeLastOrNull() }) { Text("Undo", color = TextHi) }
                TextButton(onClick = { strokes.clear() }) { Text("Clear", color = TextHi) }
            }
        }
    }
}
