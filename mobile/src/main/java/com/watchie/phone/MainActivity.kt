package com.watchie.phone

import com.cutenotes.watch.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The phone is send-only; notes are viewable on the watch.
        notesDeliveredHere = false
        setContent {
            MaterialTheme(colorScheme = WatchieDark) { PhoneApp() }
        }
    }
}

@Composable
fun PhoneApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var composeFriend by remember { mutableStateOf<Friend?>(null) }
    var drawFriend by remember { mutableStateOf<Friend?>(null) }
    var sentPayload by remember { mutableStateOf<NotePayload?>(null) }
    var sentPeer by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { transport.initialize() }

    fun deliver(friend: Friend, payload: NotePayload) {
        scope.launch { transport.send(friend.uid, payload) }
        sentPeer = friend.username
        sentPayload = payload
    }

    val sp = sentPayload
    val cf = composeFriend
    val df = drawFriend
    when {
        sp != null -> PhoneNotePlayer(sp, sentPeer) { sentPayload = null }
        !transport.initialized -> ScreenBg { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = AccentPink) } }
        !transport.isSignedIn -> AuthScreen()
        transport.myUsername == null -> UsernameScreen()
        df != null -> PhoneDrawScreen(
            peer = df.username,
            onSend = { strokes -> drawFriend = null; deliver(df, NotePayload.DrawingNote(strokes)) },
            onCancel = { drawFriend = null },
        )
        cf != null -> ComposeScreen(
            peer = cf.username,
            onSend = { payload -> composeFriend = null; deliver(cf, payload) },
            onOpenDraw = { composeFriend = null; drawFriend = cf },
            onBack = { composeFriend = null },
        )
        else -> HomeScreen(onFriendTap = { composeFriend = it })
    }
}

@Composable
private fun AuthScreen() {
    val scope = rememberCoroutineScope()
    var signUp by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    ScreenBg {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandTitle(48)
            Spacer(Modifier.height(6.dp))
            Text(if (signUp) "Create your account" else "Welcome back", color = TextLo)
            Spacer(Modifier.height(28.dp))
            GlassField(email, { email = it.trim() }, "Email", isEmail = true)
            Spacer(Modifier.height(12.dp))
            GlassField(password, { password = it }, "Password", isPassword = true)
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = AccentPink, fontSize = 13.sp)
            }
            Spacer(Modifier.height(22.dp))
            GradientButton(
                text = if (busy) "Please wait…" else if (signUp) "Sign up" else "Log in",
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            ) {
                scope.launch {
                    busy = true; message = ""
                    val err = if (signUp) transport.signUp(email, password) else transport.signIn(email, password)
                    busy = false
                    if (err != null) message = err
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { signUp = !signUp; message = "" }) {
                Text(if (signUp) "Have an account? Log in" else "New here? Sign up", color = TextLo)
            }
        }
    }
}

@Composable
private fun UsernameScreen() {
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    ScreenBg {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Choose a username", color = TextHi, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("This is how friends add you", color = TextLo, fontSize = 13.sp)
            Spacer(Modifier.height(22.dp))
            GlassField(entry, { entry = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(15) }, "username")
            Text("3–15 letters, numbers, _", color = TextLo, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = AccentPink, fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            GradientButton("Claim it", modifier = Modifier.fillMaxWidth()) {
                scope.launch {
                    when (transport.setUsername(entry)) {
                        UsernameResult.TAKEN -> message = "@${normalizeUsername(entry)} is taken"
                        UsernameResult.INVALID -> message = "3–15 letters, numbers, _ only"
                        UsernameResult.ERROR -> message = "Something went wrong"
                        UsernameResult.OK -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onFriendTap: (Friend) -> Unit) {
    val scope = rememberCoroutineScope()
    val friends = transport.friends
    var showAdd by remember { mutableStateOf(false) }

    ScreenBg {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    BrandTitle(30)
                    Text("@${transport.myUsername ?: ""}", color = TextLo, fontSize = 13.sp)
                }
                TextButton(onClick = { scope.launch { transport.signOut() } }) { Text("Log out", color = TextLo, fontSize = 12.sp) }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Friends", color = TextHi, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(width = 88.dp, height = 40.dp),
                ) { GradientButton("+ Add", modifier = Modifier.fillMaxSize()) { showAdd = true } }
            }
            Spacer(Modifier.height(12.dp))

            if (friends.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No friends yet.\nTap + Add and enter their username.", color = TextLo, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(friends) { friend ->
                        GlassCard(onClick = { onFriendTap(friend) }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(friend.username)
                                Spacer(Modifier.size(14.dp))
                                Text("@${friend.username}", color = TextHi, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                if (friend.streak > 0) {
                                    Text("🔥 ${friend.streak}", fontSize = 15.sp, color = TextHi)
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text("›", color = TextLo, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddFriendDialog(onDismiss = { showAdd = false })
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a friend") },
        text = {
            Column {
                Text("You are @${transport.myUsername ?: ""}", color = TextLo, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                GlassField(entry, { entry = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(15) }, "their username")
                if (message.isNotEmpty()) Text(message, color = AccentPink, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { if (transport.addFriend(entry) != null) onDismiss() else message = transport.statusText }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun openPlayStore(context: Context) {
    val pkg = context.packageName
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
