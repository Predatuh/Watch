package com.cutenotes.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

private const val PAGE_COUNT = 5

/**
 * The main screen: five swipeable category pages — Inbox, Expressions,
 * Fireworks, Draw, Settings — with a page indicator at the bottom.
 */
@Composable
fun HomePager(
    settings: AppSettings,
    pending: IncomingNote?,
    onOpenIncoming: () -> Unit,
    onSendExpression: (Expression) -> Unit,
    onSendFirework: (FireworkType) -> Unit,
    onOpenDraw: () -> Unit,
    onOpenAddFriend: () -> Unit,
    onOpenUsername: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { PAGE_COUNT })
    val indicatorState = remember(pagerState) {
        object : PageIndicatorState {
            override val pageOffset: Float get() = pagerState.currentPageOffsetFraction
            override val selectedPage: Int get() = pagerState.currentPage
            override val pageCount: Int get() = PAGE_COUNT
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        pageIndicator = { HorizontalPageIndicator(pageIndicatorState = indicatorState) },
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> InboxPage(pending, onOpenIncoming, onOpenAddFriend, onOpenUsername)
                1 -> ExpressionsPage(onSendExpression)
                2 -> FireworksPage(onSendFirework)
                3 -> DrawPage(onOpenDraw)
                else -> SettingsPage(settings)
            }
        }
    }
}

private fun pageColumn() = PaddingValues(horizontal = 8.dp, vertical = 30.dp)

@Composable
private fun InboxPage(
    pending: IncomingNote?,
    onOpenIncoming: () -> Unit,
    onOpenAddFriend: () -> Unit,
    onOpenUsername: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    val friends = transport.friends
    val username = transport.myUsername
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = pageColumn(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text("Inbox", style = MaterialTheme.typography.title3, color = Color.White) }

        item {
            if (pending != null) {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenIncoming,
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = accentFor(pending.payload),
                        endBackgroundColor = accentFor(pending.payload).copy(alpha = 0.5f),
                    ),
                    label = { Text("💌  ${pending.from} sent you a note") },
                    secondaryLabel = { Text("Tap to open") },
                )
            } else {
                Text("No new notes yet", color = Color(0xFFAAAAB2), fontSize = 13.sp)
            }
        }

        // Your username (tap to view/change it).
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenUsername,
                colors = ChipDefaults.secondaryChipColors(),
                icon = { Text("🪪", fontSize = 18.sp) },
                label = { Text(if (username != null) "You: @$username" else "Choose a username") },
                secondaryLabel = { Text(transport.statusText) },
            )
        }

        item {
            Text(
                text = "Friends",
                color = Color(0xFFBBBBC4),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (friends.isEmpty()) {
            item { Text("No friends yet — add one", color = Color(0xFF888890), fontSize = 12.sp) }
        } else {
            items(friends) { friend ->
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenAddFriend,
                    colors = ChipDefaults.secondaryChipColors(),
                    icon = { Text("👤", fontSize = 18.sp) },
                    label = { Text("@${friend.username}") },
                )
            }
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                onClick = onOpenAddFriend,
                colors = ChipDefaults.primaryChipColors(),
                icon = { Text("➕", fontSize = 18.sp) },
                label = { Text("Add friend") },
            )
        }
    }
}

@Composable
private fun ExpressionsPage(onSend: (Expression) -> Unit) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = pageColumn(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text("Send a note", style = MaterialTheme.typography.title3, color = Color.White) }
        items(expressions) { expression ->
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSend(expression) },
                colors = ChipDefaults.secondaryChipColors(),
                icon = { Text(expression.emoji, fontSize = 22.sp) },
                label = { Text(expression.label) },
            )
        }
    }
}

@Composable
private fun FireworksPage(onSend: (FireworkType) -> Unit) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = pageColumn(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text("🎆 Fireworks", style = MaterialTheme.typography.title3, color = Color.White) }
        items(fireworkTypes) { type ->
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSend(type) },
                colors = ChipDefaults.secondaryChipColors(),
                icon = { Text("🎆", fontSize = 20.sp) },
                label = { Text(type.label) },
                secondaryLabel = { Text(type.tagline) },
            )
        }
    }
}

@Composable
private fun DrawPage(onOpenDraw: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("✏️", fontSize = 34.sp)
        Spacer(Modifier.height(6.dp))
        Text("Draw a note", style = MaterialTheme.typography.title3, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Chip(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenDraw,
            colors = ChipDefaults.primaryChipColors(),
            label = { Text("Open drawing pad") },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Doodle something and send it",
            color = Color(0xFF888890),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsPage(settings: AppSettings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = pageColumn(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.title3, color = Color.White) }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { settings.updateVibrationEnabled(!settings.vibrationEnabled) },
                colors = if (settings.vibrationEnabled) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                icon = { Text("📳", fontSize = 20.sp) },
                label = { Text("Vibration") },
                secondaryLabel = { Text(if (settings.vibrationEnabled) "On" else "Off") },
            )
        }

        item {
            Text("Buzz strength", color = Color(0xFFBBBBC4), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }

        VibrationStrength.entries.forEach { strength ->
            item {
                val selected = settings.vibrationStrength == strength
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        settings.updateVibrationStrength(strength)
                        Haptics.preview(context, strength)
                    },
                    colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    label = { Text(strength.label) },
                    secondaryLabel = if (selected) {
                        { Text("Selected · tap to test") }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** A representative color for a pending note, used to tint the inbox banner. */
private fun accentFor(payload: NotePayload): Color = when (payload) {
    is NotePayload.ExpressionNote ->
        expressions.firstOrNull { it.id == payload.expressionId }?.accent ?: Color(0xFFFF6B9D)
    is NotePayload.FireworkNote -> Color(0xFF7C3AED)
    is NotePayload.DrawingNote -> Color(0xFFFF6B9D)
}
