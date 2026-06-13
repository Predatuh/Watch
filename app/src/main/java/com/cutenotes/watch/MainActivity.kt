package com.cutenotes.watch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
        Notifications.ensureChannel(this) // so FCM notifications use our vibrating channel
        setContent { CuteNotesApp() }
    }
}

/** Everything the screen can show: the home pager, or an overlay. */
private sealed interface Screen {
    data object Home : Screen
    data object Draw : Screen
    data object Write : Screen
    data object AddFriend : Screen
    data object Username : Screen
    data class FriendPicker(val payload: NotePayload) : Screen
    data class Playing(val expression: Expression, val incoming: Boolean, val peer: String) : Screen
    data class PlayingDraw(val strokes: List<DrawnStroke>, val incoming: Boolean, val peer: String) : Screen
    data class PlayingFirework(val type: FireworkType, val incoming: Boolean, val peer: String) : Screen
    data class PlayingText(val text: String, val effect: Effect, val incoming: Boolean, val peer: String) : Screen
}


@Composable
fun CuteNotesApp() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var promptedUsername by remember { mutableStateOf(false) }
    var shownNoteId by remember { mutableStateOf<String?>(null) }

    fun playerFor(payload: NotePayload, incoming: Boolean, peer: String): Screen = when (payload) {
        is NotePayload.ExpressionNote -> Screen.Playing(expressionById(payload.expressionId), incoming, peer)
        is NotePayload.FireworkNote -> Screen.PlayingFirework(payload.type, incoming, peer)
        is NotePayload.DrawingNote -> Screen.PlayingDraw(payload.strokes, incoming, peer)
        is NotePayload.TextNote -> Screen.PlayingText(payload.text, payload.effect, incoming, peer)
    }

    fun openNote(p: PendingNote) {
        shownNoteId = p.id
        screen = playerFor(p.payload, incoming = true, peer = p.from)
    }

    // Dismissing a received note marks it seen (removes it from the inbox).
    fun dismissPlayer(wasIncoming: Boolean) {
        if (wasIncoming) {
            shownNoteId?.let { id -> scope.launch { transport.consumeNote(id) } }
            shownNoteId = null
        }
        screen = Screen.Home
    }

    // Ask for notification permission (Android 13+) so pushes can show.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Sign in / connect once on launch.
    LaunchedEffect(Unit) { transport.initialize() }

    // Once signed in with no username yet: prompt to choose one.
    LaunchedEffect(transport.isSignedIn, transport.myUsername) {
        if (transport.isSignedIn && transport.myUsername == null && !promptedUsername) {
            promptedUsername = true
            screen = Screen.Username
        }
    }

    fun deliver(friend: Friend, payload: NotePayload) {
        scope.launch { transport.send(friend.uid, payload) }
        screen = playerFor(payload, incoming = false, peer = friend.username)
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

    // Buzz when a new note shows up in the inbox queue.
    var lastPendingCount by remember { mutableStateOf(0) }
    LaunchedEffect(transport.pendingNotes.size) {
        if (transport.pendingNotes.size > lastPendingCount) Haptics.playNoteBuzz(context, settings)
        lastPendingCount = transport.pendingNotes.size
    }

    // Pop the next waiting note up automatically while on the home screen — covers
    // arrival and opening the app after missing one. Shown one at a time.
    LaunchedEffect(transport.pendingNotes.firstOrNull()?.id, screen) {
        val p = transport.pendingNotes.firstOrNull()
        if (p != null && screen is Screen.Home) openNote(p)
    }

    MaterialTheme {
        when {
            !transport.initialized -> {
                LoadingScreen()
                return@MaterialTheme
            }
            !transport.isSignedIn -> {
                AuthScreen()
                return@MaterialTheme
            }
        }

        // Flip your wrist up to open the latest waiting note — only while on the pager.
        RaiseToWakeEffect(enabled = screen is Screen.Home) {
            transport.pendingNotes.firstOrNull()?.let { openNote(it) }
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
                is Screen.Home -> HomePager(
                    settings = settings,
                    pending = transport.pendingNotes.firstOrNull(),
                    onOpenIncoming = { transport.pendingNotes.firstOrNull()?.let { openNote(it) } },
                    onSendExpression = { startSend(NotePayload.ExpressionNote(it.id)) },
                    onSendFirework = { startSend(NotePayload.FireworkNote(it)) },
                    onOpenDraw = { screen = Screen.Draw },
                    onOpenWrite = { screen = Screen.Write },
                    onOpenAddFriend = { screen = Screen.AddFriend },
                    onOpenUsername = { screen = Screen.Username },
                )

                is Screen.Write -> TextNoteScreen(
                    onSend = { t, e -> startSend(NotePayload.TextNote(t, e)) },
                )

                is Screen.AddFriend -> AddFriendScreen(onDone = { screen = Screen.Home })

                is Screen.Username -> UsernameScreen(onDone = { screen = Screen.Home })

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
                    onDismiss = { dismissPlayer(current.incoming) },
                )

                is Screen.PlayingDraw -> DrawnNotePlayer(
                    strokes = current.strokes,
                    incoming = current.incoming,
                    peer = current.peer,
                    onDismiss = { dismissPlayer(current.incoming) },
                )

                is Screen.PlayingFirework -> FireworkPlayer(
                    type = current.type,
                    incoming = current.incoming,
                    peer = current.peer,
                    onDismiss = { dismissPlayer(current.incoming) },
                )

                is Screen.PlayingText -> TextNotePlayer(
                    text = current.text,
                    effect = current.effect,
                    incoming = current.incoming,
                    peer = current.peer,
                    onDismiss = { dismissPlayer(current.incoming) },
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Connecting…", color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun PlayingScreen(
    expression: Expression,
    incoming: Boolean,
    peer: String,
    onDismiss: () -> Unit,
) {
    // Received notes stay until you clear them; sent confirmations auto-close.
    LaunchedEffect(expression.id, incoming) {
        if (!incoming) {
            delay(4500)
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                .padding(bottom = 50.dp),
        )

        DismissButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            onClick = onDismiss,
        )
    }
}
