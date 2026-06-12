package com.watchie.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cutenotes.watch.DrawnStroke

private class LiveStroke(val color: Color, val width: Float) {
    val points = mutableStateListOf<Offset>()
}

private val INK = listOf(
    Color(0xFFFF4D8D), Color(0xFF9B5DE5), Color(0xFF34E0EA),
    Color(0xFF6BCB77), Color(0xFFFFD166), Color.White,
)
private val BRUSHES = listOf(8f, 16f, 26f)

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

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp)).background(Color(0xFF0E0A18))
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .pointerInput(color, width) {
                        detectDragGestures(
                            onDragStart = { o -> strokes.add(LiveStroke(color, width).also { it.points.add(o) }) },
                            onDrag = { ch, _ -> strokes.lastOrNull()?.points?.add(ch.position); ch.consume() },
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    strokes.forEach { s ->
                        if (s.points.size > 1) {
                            val path = Path().apply {
                                moveTo(s.points.first().x, s.points.first().y)
                                for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                            }
                            drawPath(path, s.color, style = Stroke(s.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        } else if (s.points.size == 1) {
                            drawCircle(s.color, s.width / 2f, s.points.first())
                        }
                    }
                }
            }

            // Colors
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                INK.forEachIndexed { i, c ->
                    Box(
                        modifier = Modifier.size(if (i == colorIndex) 34.dp else 28.dp).clip(CircleShape).background(c)
                            .border(if (i == colorIndex) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { colorIndex = i },
                    )
                }
            }
            // Tools
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
