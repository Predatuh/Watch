package com.cutenotes.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CuteNotesApp() }
    }
}

/** The two things the screen can show: the home list, or one note playing full-screen. */
private sealed interface Screen {
    data object Home : Screen
    data class Playing(val expression: Expression, val incoming: Boolean) : Screen
}

@Composable
fun CuteNotesApp() {
    // Current screen. Starts on Home; tapping a note switches to Playing.
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // A pretend note "from Alex" so you can see the receiving experience.
    val incomingNote = remember { expressions.first { it.id == "miss" } }

    MaterialTheme {
        when (val s = screen) {
            is Screen.Home -> HomeScreen(
                incomingNote = incomingNote,
                onOpenIncoming = { screen = Screen.Playing(incomingNote, incoming = true) },
                onSend = { screen = Screen.Playing(it, incoming = false) },
            )

            is Screen.Playing -> PlayingScreen(
                expression = s.expression,
                incoming = s.incoming,
                onDismiss = { screen = Screen.Home },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    incomingNote: Expression,
    onOpenIncoming: () -> Unit,
    onSend: (Expression) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() }) {
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
        }
    }
}

@Composable
private fun PlayingScreen(
    expression: Expression,
    incoming: Boolean,
    onDismiss: () -> Unit,
) {
    // Auto-close after a few seconds, or the user can tap to close sooner.
    LaunchedEffect(expression.id) {
        delay(4500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
    ) {
        // The animated expression fills the screen.
        ExpressionPlayer(expression = expression)

        // Caption: who it's from / who it's going to.
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
