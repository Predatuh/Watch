package com.cutenotes.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CuteNotesApp() }
    }
}

/** Everything the screen can show. */
private sealed interface Screen {
    data object Home : Screen
    data object Draw : Screen
    data object Settings : Screen
    data object Fireworks : Screen
    data class Playing(val expression: Expression, val incoming: Boolean) : Screen
    data class PlayingDraw(val strokes: List<DrawnStroke>, val incoming: Boolean) : Screen
    data class PlayingFirework(val type: FireworkType, val incoming: Boolean) : Screen
}

@Composable
fun CuteNotesApp() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // A pretend note "from Alex" so you can see the receiving experience.
    val incomingNote = remember { expressions.first { it.id == "miss" } }

    MaterialTheme {
        // Flip your wrist up to auto-open the latest note — only while on home.
        RaiseToWakeEffect(enabled = screen is Screen.Home) {
            screen = Screen.Playing(incomingNote, incoming = true)
        }

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 1.05f, animationSpec = tween(150)))
            },
            label = "screens",
        ) { current ->
            when (current) {
                is Screen.Home -> HomeScreen(
                    incomingNote = incomingNote,
                    onOpenIncoming = { screen = Screen.Playing(incomingNote, incoming = true) },
                    onSend = { screen = Screen.Playing(it, incoming = false) },
                    onOpenDraw = { screen = Screen.Draw },
                    onOpenFireworks = { screen = Screen.Fireworks },
                    onOpenSettings = { screen = Screen.Settings },
                )

                is Screen.Fireworks -> FireworksScreen(
                    onSend = { type -> screen = Screen.PlayingFirework(type, incoming = false) },
                    onBack = { screen = Screen.Home },
                )

                is Screen.PlayingFirework -> FireworkPlayer(
                    type = current.type,
                    incoming = current.incoming,
                    onDismiss = { screen = Screen.Home },
                )

                is Screen.Draw -> DrawScreen(
                    onSend = { strokes -> screen = Screen.PlayingDraw(strokes, incoming = false) },
                    onCancel = { screen = Screen.Home },
                )

                is Screen.Settings -> SettingsScreen(
                    settings = settings,
                    onBack = { screen = Screen.Home },
                )

                is Screen.Playing -> PlayingScreen(
                    expression = current.expression,
                    incoming = current.incoming,
                    settings = settings,
                    onDismiss = { screen = Screen.Home },
                )

                is Screen.PlayingDraw -> DrawnNotePlayer(
                    strokes = current.strokes,
                    incoming = current.incoming,
                    onDismiss = { screen = Screen.Home },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    incomingNote: Expression,
    onOpenIncoming: () -> Unit,
    onSend: (Expression) -> Unit,
    onOpenDraw: () -> Unit,
    onOpenFireworks: () -> Unit,
    onOpenSettings: () -> Unit,
) {
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The "you've got a note" banner at the top.
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenIncoming,
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = incomingNote.accent,
                        endBackgroundColor = incomingNote.accent.copy(alpha = 0.5f),
                    ),
                    label = { Text("💌  Alex sent you a note") },
                    secondaryLabel = { Text("Tap to open") },
                )
            }

            // Fireworks "tab".
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenFireworks,
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = Color(0xFF7C3AED),
                        endBackgroundColor = Color(0xFF2563EB),
                    ),
                    icon = { Text("🎆", fontSize = 22.sp) },
                    label = { Text("Fireworks") },
                    secondaryLabel = { Text("Peony, Crackle, Willow…") },
                )
            }

            item {
                Text(
                    text = "Send a note",
                    style = MaterialTheme.typography.title3,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }

            // One tappable chip per expression you can send.
            items(expressions) { expression ->
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSend(expression) },
                    colors = ChipDefaults.secondaryChipColors(),
                    icon = { Text(expression.emoji, fontSize = 22.sp) },
                    label = { Text(expression.label) },
                )
            }

            // Draw-your-own note.
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    onClick = onOpenDraw,
                    colors = ChipDefaults.primaryChipColors(),
                    icon = { Text("✏️", fontSize = 22.sp) },
                    label = { Text("Draw a note") },
                )
            }

            // Settings.
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSettings,
                    colors = ChipDefaults.secondaryChipColors(),
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Settings") },
                )
            }
        }
    }
}

@Composable
private fun PlayingScreen(
    expression: Expression,
    incoming: Boolean,
    settings: AppSettings,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // When a note arrives, buzz the watch, then auto-close after a few seconds.
    LaunchedEffect(expression.id, incoming) {
        if (incoming) Haptics.playNoteBuzz(context, settings)
        delay(4500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
    ) {
        ExpressionPlayer(expression = expression)

        Text(
            text = if (incoming) "From Alex 💕" else "Sent to Alex 💌",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 26.dp),
        )

        Text(
            text = expression.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}
