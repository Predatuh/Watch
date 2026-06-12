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

/** Everything the screen can show: the home pager, or an overlay. */
private sealed interface Screen {
    data object Home : Screen
    data object Draw : Screen
    data object AddFriend : Screen
    data class FriendPicker(val payload: NotePayload) : Screen
    data class Playing(val expression: Expression, val incoming: Boolean, val peer: String) : Screen
    data class PlayingDraw(val strokes: List<DrawnStroke>, val incoming: Boolean, val peer: String) : Screen
    data class PlayingFirework(val type: FireworkType, val incoming: Boolean, val peer: String) : Screen
}

private fun expressionById(id: String): Expression =
    expressions.firstOrNull { it.id == id } ?: expressions.first()

@Composable
fun CuteNotesApp() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var pending by remember { mutableStateOf<IncomingNote?>(null) }

    // Sign in / connect once on launch.
    LaunchedEffect(Unit) { transport.initialize() }

    // Notes arriving from friends: update the inbox and buzz.
    LaunchedEffect(Unit) {
        transport.incoming.collect { note ->
            pending = note
            Haptics.playNoteBuzz(context, settings)
        }
    }

    fun playerFor(payload: NotePayload, incoming: Boolean, peer: String): Screen = when (payload) {
        is NotePayload.ExpressionNote -> Screen.Playing(expressionById(payload.expressionId), incoming, peer)
        is NotePayload.FireworkNote -> Screen.PlayingFirework(payload.type, incoming, peer)
        is NotePayload.DrawingNote -> Screen.PlayingDraw(payload.strokes, incoming, peer)
    }

    fun deliver(friend: Friend, payload: NotePayload) {
        scope.launch { transport.send(friend.uid, payload) }
        screen = playerFor(payload, incoming = false, peer = friend.name)
    }

    // Choose who to send to: straight through if you have exactly one friend.
    fun startSend(payload: NotePayload) {
        val fs = transport.friends
        when {
            fs.isEmpty() -> screen = Screen.AddFriend
            fs.size == 1 -> deliver(fs[0], payload)
            else -> screen = Screen.FriendPicker(payload)
        }
    }

    fun openPending() {
        val note = pending ?: return
        screen = playerFor(note.payload, incoming = true, peer = note.from)
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
                    onSendExpression = { startSend(NotePayload.ExpressionNote(it.id)) },
                    onSendFirework = { startSend(NotePayload.FireworkNote(it)) },
                    onOpenDraw = { screen = Screen.Draw },
                    onOpenAddFriend = { screen = Screen.AddFriend },
                )

                is Screen.AddFriend -> AddFriendScreen(onDone = { screen = Screen.Home })

                is Screen.FriendPicker -> FriendPicker(
                    friends = transport.friends,
                    onPick = { friend -> deliver(friend, current.payload) },
                    onCancel = { screen = Screen.Home },
                )

                is Screen.Draw -> DrawScreen(
                    onSend = { strokes -> startSend(NotePayload.DrawingNote(strokes)) },
                    onCancel = { screen = Screen.Home },
                )

                is Screen.Playing -> PlayingScreen(
                    expression = current.expression,
                    incoming = current.incoming,
                    peer = current.peer,
                    onDismiss = { screen = Screen.Home },
                )

                is Screen.PlayingDraw -> DrawnNotePlayer(
                    strokes = current.strokes,
                    incoming = current.incoming,
                    peer = current.peer,
                    onDismiss = { screen = Screen.Home },
                )

                is Screen.PlayingFirework -> FireworkPlayer(
                    type = current.type,
                    incoming = current.incoming,
                    peer = current.peer,
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
    peer: String,
    onDismiss: () -> Unit,
) {
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
            text = if (incoming) "From $peer 💕" else "Sent to $peer 💌",
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
