package com.watchie.phone

import com.cutenotes.watch.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NOTE_ACCENT = Color(0xFFFF4D8D)

private val effectChoices = listOf(
    Effect.FIREWORKS to "🎆", Effect.HEARTS to "❤️", Effect.CONFETTI to "🎉",
    Effect.SPARKLE to "✨", Effect.PETALS to "🌸", Effect.STARBURST to "☀️",
    Effect.BUBBLES to "🫧", Effect.SNOW to "❄️", Effect.NONE to "—",
)

private enum class Tab(val label: String) { NOTES("Notes"), FIREWORKS("Fireworks"), WRITE("Write"), DRAW("Draw") }

/** Send anything to a friend: expressions, fireworks, a written note, or a drawing. */
@Composable
fun ComposeScreen(
    peer: String,
    onSend: (NotePayload) -> Unit,
    onOpenDraw: () -> Unit,
    onBack: () -> Unit,
) {
    var tab by remember { mutableStateOf(Tab.NOTES) }

    ScreenBg {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("←", color = TextHi, fontSize = 22.sp) }
                Avatar(peer, size = 34)
                Spacer(Modifier.size(10.dp))
                Text("@$peer", color = TextHi, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))

            // Segmented selector
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Tab.entries.forEach { t ->
                    val sel = t == tab
                    Box(
                        modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Brush.linearGradient(listOf(AccentPink, AccentViolet)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                            .clickable { tab = t },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(t.label, color = if (sel) Color.White else TextLo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            when (tab) {
                Tab.NOTES -> NotesPane { onSend(NotePayload.ExpressionNote(it)) }
                Tab.FIREWORKS -> FireworksPane { onSend(NotePayload.FireworkNote(it)) }
                Tab.WRITE -> WritePane { text, effect -> onSend(NotePayload.TextNote(text, effect)) }
                Tab.DRAW -> DrawPane(onOpenDraw)
            }
        }
    }
}

@Composable
private fun NotesPane(onPick: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(expressions) { e ->
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(CardBg)
                    .border(1.dp, CardBorder, CircleShape).clickable { onPick(e.id) },
                contentAlignment = Alignment.Center,
            ) { Text(e.emoji, fontSize = 30.sp) }
        }
    }
}

@Composable
private fun FireworksPane(onPick: (FireworkType) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(fireworkTypes) { t ->
            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp)).background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).clickable { onPick(t) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🎆", fontSize = 22.sp)
                Spacer(Modifier.size(8.dp))
                Text(t.label, color = TextHi, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun WritePane(onSend: (String, Effect) -> Unit) {
    var text by remember { mutableStateOf("") }
    var effect by remember { mutableStateOf(Effect.FIREWORKS) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF0E0A18))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            EffectLayer(effect = effect, accent = NOTE_ACCENT, modifier = Modifier.fillMaxSize())
            Text(
                text.ifEmpty { "Your note…" },
                color = if (text.isEmpty()) TextLo else Color.White,
                fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        GlassField(value = text, onValueChange = { text = it.take(160) }, label = "Type your note (emojis too!)")
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            effectChoices.forEach { (e, glyph) ->
                val sel = e == effect
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                        .background(if (sel) AccentGradient else Brush.linearGradient(listOf(CardBg, CardBg)))
                        .border(1.dp, if (sel) Color.Transparent else CardBorder, CircleShape)
                        .clickable { effect = e },
                    contentAlignment = Alignment.Center,
                ) { Text(glyph, fontSize = 20.sp) }
            }
        }
        Spacer(Modifier.height(14.dp))
        GradientButton("Send 💌", enabled = text.isNotBlank()) { if (text.isNotBlank()) onSend(text, effect) }
    }
}

@Composable
private fun DrawPane(onOpenDraw: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Text("✏️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Doodle something", color = TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        GradientButton("Open canvas", modifier = Modifier.fillMaxWidth()) { onOpenDraw() }
    }
}

/** Full-screen playback of a note (used for the "sent" confirmation). */
@Composable
fun PhoneNotePlayer(payload: NotePayload, peer: String, onDismiss: () -> Unit) {
    LaunchedEffect(payload) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }
    val night = Brush.verticalGradient(listOf(Color(0xFF0B1026), Color(0xFF05060F), Color.Black))
    Box(
        modifier = Modifier.fillMaxSize().background(if (payload is NotePayload.FireworkNote) night else Brush.verticalGradient(listOf(Color(0xFF120A1E), Color(0xFF07040D))))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        when (payload) {
            is NotePayload.ExpressionNote -> {
                val e = expressionById(payload.expressionId)
                EffectLayer(e.effect, e.accent, Modifier.fillMaxSize())
                Text(e.emoji, fontSize = 96.sp)
            }
            is NotePayload.FireworkNote -> FireworkEffect(payload.type, Modifier.fillMaxSize())
            is NotePayload.TextNote -> {
                EffectLayer(payload.effect, NOTE_ACCENT, Modifier.fillMaxSize())
                Text(payload.text, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(28.dp))
            }
            is NotePayload.DrawingNote -> {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    drawNormalizedStrokes(payload.strokes)
                }
            }
        }
        Text("Sent to @$peer ✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp))
    }
}
