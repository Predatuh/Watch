package com.cutenotes.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
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
import kotlinx.coroutines.launch

@Composable
fun UsernameScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val existing = transport.myUsername
    var entry by remember { mutableStateOf(existing ?: "") }
    var message by remember { mutableStateOf("") }
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    if (existing == null) "Choose a username" else "Your username",
                    style = MaterialTheme.typography.title3,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    "This is how friends add you",
                    color = Color(0xFFAAAAB2),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
            item { UsernameField(entry) { entry = it } }
            item {
                Text(
                    "3–15 letters, numbers, _",
                    color = Color(0xFF888890),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
            if (message.isNotEmpty()) {
                item { Text(message, color = Color(0xFFFF8FA3), fontSize = 12.sp, textAlign = TextAlign.Center) }
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            message = ""
                            when (transport.setUsername(entry)) {
                                UsernameResult.OK -> onDone()
                                UsernameResult.TAKEN -> message = "@${normalizeUsername(entry)} is taken"
                                UsernameResult.INVALID -> message = "3–15 letters, numbers, _ only"
                                UsernameResult.ERROR -> message = "Something went wrong"
                            }
                        }
                    },
                    colors = ChipDefaults.primaryChipColors(),
                    label = { Text(if (existing == null) "Claim it" else "Save") },
                )
            }
            if (existing != null) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDone,
                        colors = ChipDefaults.secondaryChipColors(),
                        label = { Text("← Back") },
                    )
                }
            }
        }
    }
}

@Composable
private fun UsernameField(value: String, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(Color(0xFF26262E))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                onChange(raw.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(15))
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 19.sp, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text("username", color = Color(0xFF666670), fontSize = 19.sp)
                    }
                    inner()
                }
            },
        )
    }
}
