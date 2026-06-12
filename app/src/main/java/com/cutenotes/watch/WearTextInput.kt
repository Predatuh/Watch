package com.cutenotes.watch

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper

private const val INPUT_KEY = "watchie_text"

/**
 * A tappable field that uses the watch's native text-input screen. Tapping it
 * opens the full keyboard/voice input and returns the finished text — this
 * avoids the inline-editing glitches you hit when typing directly into a small
 * Compose field on Wear OS.
 */
@Composable
fun WearTextInput(
    value: String,
    placeholder: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val text = RemoteInput.getResultsFromIntent(data)?.getCharSequence(INPUT_KEY)?.toString()
        if (text != null) onValueChange(text)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF26262E))
            .clickable {
                val remoteInputs = listOf(
                    RemoteInput.Builder(INPUT_KEY).setLabel(label).build(),
                )
                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                launcher.launch(intent)
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.ifEmpty { placeholder },
            color = if (value.isEmpty()) Color(0xFF777780) else Color.White,
            fontSize = 19.sp,
            textAlign = TextAlign.Center,
        )
    }
}
