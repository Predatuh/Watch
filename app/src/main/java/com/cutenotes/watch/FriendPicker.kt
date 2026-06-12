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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
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

/** "Send to…" chooser shown when you have more than one friend. */
@Composable
fun FriendPicker(friends: List<Friend>, onPick: (Friend) -> Unit, onCancel: () -> Unit) {
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
            item { Text("Send to…", style = MaterialTheme.typography.title3, color = Color.White) }
            items(friends) { friend ->
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPick(friend) },
                    colors = ChipDefaults.primaryChipColors(),
                    icon = { Text("👤", fontSize = 18.sp) },
                    label = { Text(friend.name) },
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    onClick = onCancel,
                    colors = ChipDefaults.secondaryChipColors(),
                    label = { Text("Cancel") },
                )
            }
        }
    }
}
