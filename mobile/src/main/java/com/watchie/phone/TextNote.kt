package com.watchie.phone

import com.cutenotes.watch.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private val NOTE_ACCENT = Color(0xFFFF6B9D)

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

@Composable
fun WriteScreen(
    friends: List<Friend>,
    onSend: (String, Effect, Friend) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var effect by remember { mutableStateOf(Effect.FIREWORKS) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("← Back") }
            Text("Write a note", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))

        // Live preview
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(18.dp)).background(Color(0xFF0E0A12)),
            contentAlignment = Alignment.Center,
        ) {
            EffectLayer(effect = effect, accent = NOTE_ACCENT, modifier = Modifier.fillMaxSize())
            Text(
                text = text.ifEmpty { "Your note…" },
                color = if (text.isEmpty()) Color(0xFF777780) else Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(160) },
            label = { Text("Type your note (emojis too!)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        Text("Effect", fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            effectChoices.forEach { (e, label) ->
                FilterChip(selected = e == effect, onClick = { effect = e }, label = { Text(label) })
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Send to", fontWeight = FontWeight.Medium)
        if (friends.isEmpty()) {
            Text("Add a friend first", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(friends) { friend ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable(enabled = text.isNotBlank()) { onSend(text, effect, friend) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("@${friend.username}", fontWeight = FontWeight.Medium)
                        Text(if (text.isBlank()) "type first" else "Send ▶", color = NOTE_ACCENT)
                    }
                }
            }
        }
    }
}

/** Full-screen playback of a text note with its effect. */
@Composable
fun TextNotePlayer(text: String, effect: Effect, incoming: Boolean, peer: String, onDismiss: () -> Unit) {
    LaunchedEffect(text, incoming) {
        if (!incoming) {
            kotlinx.coroutines.delay(5000)
            onDismiss()
        }
    }
    val size = when {
        text.length < 20 -> 40.sp
        text.length < 60 -> 30.sp
        else -> 22.sp
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0810)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        EffectLayer(effect = effect, accent = NOTE_ACCENT, modifier = Modifier.fillMaxSize())
        Text(
            text = text,
            color = Color.White,
            fontSize = size,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
        Text(
            text = if (incoming) "From @$peer" else "Sent to @$peer",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
        )
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
            Text("Close", color = Color.White)
        }
    }
}
