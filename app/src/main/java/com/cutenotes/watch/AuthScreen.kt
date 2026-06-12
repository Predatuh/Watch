package com.cutenotes.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch

/** Sign up or log in with an email + password so your account persists. */
@Composable
fun AuthScreen() {
    val scope = rememberCoroutineScope()
    var signUp by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                if (signUp) "Create account" else "Log in",
                style = MaterialTheme.typography.title3,
                color = Color.White,
            )
        }
        item {
            Text(
                if (signUp) "So your account is saved" else "Welcome back",
                color = Color(0xFFAAAAB2),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        item { WearTextInput(email, "email", "Email") { email = it.trim() } }
        item { WearTextInput(password, "password", "Password", isPassword = true) { password = it } }

        if (message.isNotEmpty()) {
            item { Text(message, color = Color(0xFFFF8FA3), fontSize = 12.sp, textAlign = TextAlign.Center) }
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                onClick = {
                    if (busy) return@Chip
                    scope.launch {
                        busy = true
                        message = ""
                        val err = if (signUp) {
                            transport.signUp(email, password)
                        } else {
                            transport.signIn(email, password)
                        }
                        busy = false
                        if (err != null) message = err
                        // On success, the app reacts to transport.isSignedIn turning true.
                    }
                },
                colors = ChipDefaults.primaryChipColors(),
                label = { Text(if (busy) "Please wait…" else if (signUp) "Sign up" else "Log in") },
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { signUp = !signUp; message = "" },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(if (signUp) "Have an account? Log in" else "New here? Sign up") },
            )
        }
    }
}
