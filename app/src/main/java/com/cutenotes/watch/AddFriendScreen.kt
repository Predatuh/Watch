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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun AddFriendScreen(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf("") }
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
            item { Text("Add a friend", style = MaterialTheme.typography.title3, color = Color.White) }

            item { Text("Your friend code", color = Color(0xFFAAAAB2), fontSize = 12.sp) }
            item {
                Text(
                    text = transport.myCode ?: "…",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
            }
            item {
                Text(
                    text = transport.statusText,
                    color = Color(0xFF9AA0AA),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }

            item {
                Text(
                    "Enter friend's code",
                    color = Color(0xFFAAAAB2),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            item { CodeField(entry) { entry = it } }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { if (transport.addFriend(entry) != null) onDone() } },
                    colors = ChipDefaults.primaryChipColors(),
                    label = { Text("Add friend") },
                )
            }

            item {
                Chip(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    onClick = { scope.launch { transport.myCode?.let { if (transport.addFriend(it) != null) onDone() } } },
                    colors = ChipDefaults.secondaryChipColors(),
                    label = { Text("Add myself") },
                    secondaryLabel = { Text("for testing on one watch") },
                )
            }

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

@Composable
private fun CodeField(value: String, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(Color(0xFF26262E))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.uppercase().filter(Char::isLetterOrDigit).take(6)) },
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 3.sp,
            ),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text("------", color = Color(0xFF666670), fontSize = 20.sp, letterSpacing = 3.sp)
                    }
                    inner()
                }
            },
        )
    }
}
