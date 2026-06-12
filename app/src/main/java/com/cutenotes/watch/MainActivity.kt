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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CuteNotesApp() }
    }
}

/** Everything the screen can show: the home pager, or an overlay playing/drawing. */
private sealed interface Screen {
    data object Home : Screen
    data object Draw : Screen
    data class Playing(val expression: Expression, val incoming: Boolean) : Screen
    data class PlayingDraw(val strokes: List<DrawnStroke>, val incoming: Boolean) : Screen
    data class PlayingFirework(val type: FireworkType, val incoming: Boolean) : Screen
}

private fun expressionById(id: String): Expression =
    expressions.firstOrNull { it.id == id } ?: expressions.first()

@Composable
fun CuteNotesApp() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // The most recent note waiting in the inbox. Seeded so the demo has one.
    var pending by remember {
        mutableStateOf<IncomingNote?>(IncomingNote(transport.partnerName, NotePayload.ExpressionNote("miss")))
    }

    // Listen for notes arriving from the transport: update the inbox and buzz.
    LaunchedEffect(Unit) {
        transport.incoming.collect { note ->
            pending = note
            Haptics.playNoteBuzz(context, settings)
        }
    }

    fun openPending() {
        val note = pending ?: return
        screen = when (val p = note.payload) {
            is NotePayload.ExpressionNote -> Screen.Playing(expressionById(p.expressionId), incoming = true)
            is NotePayload.FireworkNote -> Screen.PlayingFirework(p.type, incoming = true)
            is NotePayload.DrawingNote -> Screen.PlayingDraw(p.strokes, incoming = true)
        }
    }

    MaterialTheme {
        // Flip your wrist up to open the latest note — only while on the pager.
        RaiseToWakeEffect(enabled = screen is Screen.Home) { openPending() }

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 1.05f, animationSpec = tween(150)))
            },
            label = "screens",
        ) { current ->
            when (current) {
                is Screen.Home -> HomePager(
                    settings = settings,
                    pending = pending,
                    onOpenIncoming = { openPending() },
                    onSendExpression = { expr ->
                        scope.launch { transport.send(NotePayload.ExpressionNote(expr.id)) }
                        screen = Screen.Playing(expr, incoming = false)
                    },
                    onSendFirework = { type ->
                        scope.launch { transport.send(NotePayload.FireworkNote(type)) }
                        screen = Screen.PlayingFirework(type, incoming = false)
                    },
                    onOpenDraw = { screen = Screen.Draw },
                )

                is Screen.Draw -> DrawScreen(
                    onSend = { strokes ->
                        scope.launch { transport.send(NotePayload.DrawingNote(strokes)) }
                        screen = Screen.PlayingDraw(strokes, incoming = false)
                    },
                    onCancel = { screen = Screen.Home },
                )

                is Screen.Playing -> PlayingScreen(
                    expression = current.expression,
                    incoming = current.incoming,
                    onDismiss = { screen = Screen.Home },
                )

                is Screen.PlayingDraw -> DrawnNotePlayer(
                    strokes = current.strokes,
                    incoming = current.incoming,
                    onDismiss = { screen = Screen.Home },
                )

                is Screen.PlayingFirework -> FireworkPlayer(
                    type = current.type,
                    incoming = current.incoming,
                    onDismiss = { screen = Screen.Home },
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
    // Auto-close after a few seconds, or tap to close sooner. (The buzz for an
    // incoming note already happened when it arrived in the inbox.)
    LaunchedEffect(expression.id) {
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
            text = if (incoming) "From ${transport.partnerName} 💕" else "Sent to ${transport.partnerName} 💌",
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
