package com.cutenotes.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import kotlinx.coroutines.delay

/** The fireworks "tab": pick a shell type to send. */
@Composable
fun FireworksScreen(onSend: (FireworkType) -> Unit, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text("🎆 Fireworks", style = MaterialTheme.typography.title3, color = Color.White)
            }

            items(fireworkTypes) { type ->
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSend(type) },
                    colors = ChipDefaults.secondaryChipColors(),
                    icon = { Text("🎆", fontSize = 20.sp) },
                    label = { Text(type.label) },
                    secondaryLabel = { Text(type.tagline) },
                )
            }

            item {
                Chip(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    onClick = onBack,
                    colors = ChipDefaults.secondaryChipColors(),
                    label = { Text("← Back") },
                )
            }
        }
    }
}

/** Full-screen firework show on a night sky, the way a sent shell will look. */
@Composable
fun FireworkPlayer(type: FireworkType, incoming: Boolean, onDismiss: () -> Unit) {
    LaunchedEffect(type) {
        delay(6000)
        onDismiss()
    }

    val nightSky = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B1026), Color(0xFF05060F), Color.Black),
    )

    Box(
        modifier = Modifier.fillMaxSize().background(nightSky).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        FireworkEffect(type = type, modifier = Modifier.fillMaxSize())

        Text(
            text = if (incoming) "From Alex 🎆" else "Sent to Alex 🎆",
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}
