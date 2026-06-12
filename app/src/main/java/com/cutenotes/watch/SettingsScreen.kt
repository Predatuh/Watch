package com.cutenotes.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun SettingsScreen(settings: AppSettings, onBack: () -> Unit) {
    val context = LocalContext.current
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.title3, color = Color.White)
            }

            // Master on/off.
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { settings.updateVibrationEnabled(!settings.vibrationEnabled) },
                    colors = if (settings.vibrationEnabled) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    icon = { Text("📳", fontSize = 20.sp) },
                    label = { Text("Vibration") },
                    secondaryLabel = { Text(if (settings.vibrationEnabled) "On" else "Off") },
                )
            }

            item {
                Text(
                    "Buzz strength",
                    color = Color(0xFFBBBBC4),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // Strength options — tapping selects it and plays a preview buzz.
            VibrationStrength.entries.forEach { strength ->
                item {
                    val selected = settings.vibrationStrength == strength
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            settings.updateVibrationStrength(strength)
                            Haptics.preview(context, strength)
                        },
                        colors = if (selected) {
                            ChipDefaults.primaryChipColors()
                        } else {
                            ChipDefaults.secondaryChipColors()
                        },
                        label = { Text(strength.label) },
                        secondaryLabel = if (selected) {
                            { Text("Selected · tap to test") }
                        } else {
                            null
                        },
                    )
                }
            }

            item {
                Chip(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    onClick = onBack,
                    colors = ChipDefaults.secondaryChipColors(),
                    label = { Text("← Back") },
                )
            }
        }
    }
}
