package com.cutenotes.watch

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/** Prompt shown when a newer Watchie version is available on the Play Store. */
@Composable
fun UpdateScreen(message: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text("🎉", fontSize = 28.sp) }
        item {
            Text(
                "Update available",
                style = MaterialTheme.typography.title3,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Text(
                message,
                color = Color(0xFFBBBBC4),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                onClick = { openPlayStore(context); onDismiss() },
                colors = ChipDefaults.primaryChipColors(),
                label = { Text("Update") },
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss,
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text("Not now") },
            )
        }
    }
}

private fun openPlayStore(context: android.content.Context) {
    val pkg = context.packageName
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
