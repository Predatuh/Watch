package com.cutenotes.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

/** Full-screen firework show on a night sky, the way a sent shell will look. */
@Composable
fun FireworkPlayer(type: FireworkType, incoming: Boolean, peer: String, onDismiss: () -> Unit) {
    LaunchedEffect(type, incoming) {
        if (!incoming) {
            delay(6000)
            onDismiss()
        }
    }

    val nightSky = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B1026), Color(0xFF05060F), Color.Black),
    )

    Box(
        modifier = Modifier.fillMaxSize().background(nightSky),
        contentAlignment = Alignment.Center,
    ) {
        FireworkEffect(type = type, modifier = Modifier.fillMaxSize())

        Text(
            text = if (incoming) "From $peer 🎆" else "Sent to $peer 🎆",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp),
        )

        Text(
            text = type.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp),
        )

        DismissButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            onClick = onDismiss,
        )
    }
}
