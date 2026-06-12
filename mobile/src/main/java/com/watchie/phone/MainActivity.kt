package com.watchie.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private val WatchieColors = lightColorScheme(
    primary = Color(0xFFFF4D7E),
    onPrimary = Color.White,
    secondary = Color(0xFF7C6BFF),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = WatchieColors) {
                Surface(modifier = Modifier.fillMaxSize()) { PhoneApp() }
            }
        }
    }
}

@Composable
fun PhoneApp() {
    val context = LocalContextSafe()
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var incoming by remember { mutableStateOf<IncomingNote?>(null) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) { transport.initialize() }
    LaunchedEffect(Unit) {
        val info = UpdateChecker.check(BuildConfig.VERSION_CODE)
        if (info.available) updateInfo = info
    }
    LaunchedEffect(Unit) {
        transport.incoming.collect { incoming = it }
    }

    // Dialogs that float over everything
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Update available") },
            text = { Text(info.message) },
            confirmButton = { TextButton(onClick = { openPlayStore(context); updateInfo = null }) { Text("Update") } },
            dismissButton = { TextButton(onClick = { updateInfo = null }) { Text("Not now") } },
        )
    }
    incoming?.let { note ->
        IncomingDialog(note) { incoming = null }
    }

    when {
        !transport.initialized -> Centered { CircularProgressIndicator() }
        !transport.isSignedIn -> AuthScreen()
        transport.myUsername == null -> UsernameScreen()
        else -> HomeScreen()
    }
}

@Composable
private fun LocalContextSafe(): Context = androidx.compose.ui.platform.LocalContext.current

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun AuthScreen() {
    val scope = rememberCoroutineScope()
    var signUp by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Watchie", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(if (signUp) "Create your account" else "Welcome back", color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it.trim() },
            label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (busy) return@Button
                scope.launch {
                    busy = true; message = ""
                    val err = if (signUp) transport.signUp(email, password) else transport.signIn(email, password)
                    busy = false
                    if (err != null) message = err
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (busy) "Please wait…" else if (signUp) "Sign up" else "Log in") }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { signUp = !signUp; message = "" }) {
            Text(if (signUp) "Have an account? Log in" else "New here? Sign up")
        }
    }
}

@Composable
private fun UsernameScreen() {
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Choose a username", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("This is how friends add you", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = entry,
            onValueChange = { entry = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(15) },
            label = { Text("username") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Text("3–15 letters, numbers, _", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                scope.launch {
                    when (transport.setUsername(entry)) {
                        UsernameResult.TAKEN -> message = "@${normalizeUsername(entry)} is taken"
                        UsernameResult.INVALID -> message = "3–15 letters, numbers, _ only"
                        UsernameResult.ERROR -> message = "Something went wrong"
                        UsernameResult.OK -> {}
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text("Claim it") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen() {
    val scope = rememberCoroutineScope()
    val friends = transport.friends
    var showAdd by remember { mutableStateOf(false) }
    var sendTo by remember { mutableStateOf<Friend?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Watchie", fontWeight = FontWeight.Bold)
                    Text("@${transport.myUsername ?: ""}", fontSize = 12.sp, color = Color.Gray)
                }
            })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Friends", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Button(onClick = { showAdd = true }) { Text("+ Add") }
            }

            if (friends.isEmpty()) {
                Centered {
                    Text("No friends yet.\nTap + Add and enter their username.", textAlign = TextAlign.Center, color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    items(friends) { friend ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { sendTo = friend },
                            colors = CardDefaults.cardColors(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("@${friend.username}", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                if (friend.streak > 0) {
                                    Text("🔥 ${friend.streak}", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddFriendDialog(onDismiss = { showAdd = false })
    sendTo?.let { friend -> SendDialog(friend = friend, onDismiss = { sendTo = null }) }
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
                Text("You are @${transport.myUsername ?: ""}", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = entry,
                    onValueChange = { entry = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(15) },
                    label = { Text("their username") }, singleLine = true,
                )
                if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (transport.addFriend(entry) != null) onDismiss() else message = transport.statusText
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SendDialog(friend: Friend, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send to @${friend.username}") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(280.dp),
            ) {
                items(expressions) { expr ->
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch { transport.send(friend.uid, NotePayload.ExpressionNote(expr.id)) }
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(expr.emoji, fontSize = 30.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun IncomingDialog(note: IncomingNote, onDismiss: () -> Unit) {
    val emoji = when (val p = note.payload) {
        is NotePayload.ExpressionNote -> expressionById(p.expressionId).emoji
        is NotePayload.FireworkNote -> "🎆"
        is NotePayload.DrawingNote -> "✏️"
    }
    val label = when (val p = note.payload) {
        is NotePayload.ExpressionNote -> expressionById(p.expressionId).label
        is NotePayload.FireworkNote -> p.type.label
        is NotePayload.DrawingNote -> "A drawing"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("From @${note.from}", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text(emoji, fontSize = 72.sp)
                Spacer(Modifier.height(8.dp))
                Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun openPlayStore(context: Context) {
    val pkg = context.packageName
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
