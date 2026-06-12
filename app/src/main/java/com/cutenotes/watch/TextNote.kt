package com.cutenotes.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.wear.compose.material.Text

/** Effect choices for a written note, with friendly labels. */
val effectChoices: List<Pair<Effect, String>> = listOf(
    Effect.FIREWORKS to "🎆 Fireworks",
    Effect.HEARTS to "❤️ Hearts",
    Effect.CONFETTI to "🎉 Confetti",
    Effect.SPARKLE to "✨ Sparkles",
    Effect.PETALS to "🌸 Petals",
    Effect.STARBURST to "☀️ Starburst",
    Effect.BUBBLES to "🫧 Bubbles",
    Effect.SNOW to "❄️ Snow",
    Effect.NONE to "Plain",
)

private val NOTE_ACCENT = Color(0xFFFF6B9D)

/** Type a note, pick an effect, see a live preview, then send. */
@Composable
fun TextNoteScreen(onSend: (String, Effect) -> Unit) {
    var text by remember { mutableStateOf("") }
    var effect by remember { mutableStateOf(Effect.FIREWORKS) }
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text("Write a note", style = MaterialTheme.typography.title3, color = Color.White) }

        // Live preview of the note with the chosen effect.
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(92.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF0E0A12)),
                contentAlignment = Alignment.Center,
            ) {
                EffectLayer(effect = effect, accent = NOTE_ACCENT, modifier = Modifier.fillMaxSize())
                Text(
                    text = text.ifEmpty { "Your note…" },
                    color = if (text.isEmpty()) Color(0xFF777780) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        item {
            WearTextInput(value = text, placeholder = "tap to type", label = "Your note") {
                text = it.take(140)
            }
        }

        item { Text("Effect", color = Color(0xFFBBBBC4), fontSize = 12.sp) }

        items(effectChoices) { (e, label) ->
            val selected = e == effect
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { effect = e },
                colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                label = { Text(label) },
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                onClick = { if (text.isNotBlank()) onSend(text, effect) },
                colors = ChipDefaults.primaryChipColors(),
                label = { Text("Send 💌") },
            )
        }
    }
}

/** Full-screen playback of a written note: the text over its animated effect. */
@Composable
fun TextNotePlayer(text: String, effect: Effect, incoming: Boolean, peer: String, onDismiss: () -> Unit) {
    LaunchedEffect(text, incoming) {
        if (!incoming) {
            kotlinx.coroutines.delay(5000)
            onDismiss()
        }
    }

    val fontSize = when {
        text.length < 20 -> 26.sp
        text.length < 60 -> 19.sp
        else -> 15.sp
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0810)), contentAlignment = Alignment.Center) {
        EffectLayer(effect = effect, accent = NOTE_ACCENT, modifier = Modifier.fillMaxSize())

        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 26.dp),
        )

        Text(
            text = if (incoming) "From $peer 💕" else "Sent to $peer 💌",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
        )

        DismissButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            onClick = onDismiss,
        )
    }
}
