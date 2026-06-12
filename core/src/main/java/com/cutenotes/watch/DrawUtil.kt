package com.cutenotes.watch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

/**
 * Drawings are stored in a **normalized unit square** (0..1), and stroke width
 * is a fraction of that square. This way a doodle drawn on a big phone screen
 * maps exactly onto the small round watch — same shape, centered, correct scale.
 */

/** Normalize a point captured inside a (possibly non-square) canvas to 0..1. */
fun normalizeInSquare(p: Offset, canvas: Size): Offset {
    val s = min(canvas.width, canvas.height).coerceAtLeast(1f)
    val left = (canvas.width - s) / 2f
    val top = (canvas.height - s) / 2f
    return Offset(((p.x - left) / s).coerceIn(0f, 1f), ((p.y - top) / s).coerceIn(0f, 1f))
}

/** Draw normalized strokes into the centered square of the current canvas. */
fun DrawScope.drawNormalizedStrokes(strokes: List<DrawnStroke>) {
    val s = size.minDimension
    val left = (size.width - s) / 2f
    val top = (size.height - s) / 2f
    strokes.forEach { st ->
        when {
            st.points.size > 1 -> {
                val path = Path()
                st.points.forEachIndexed { i, p ->
                    val x = left + p.x * s
                    val y = top + p.y * s
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, st.color, style = Stroke(st.width * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            st.points.size == 1 -> {
                val p = st.points.first()
                drawCircle(st.color, st.width * s / 2f, Offset(left + p.x * s, top + p.y * s))
            }
        }
    }
}
