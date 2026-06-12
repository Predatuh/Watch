package com.watchie.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- Palette: deep space dark with neon pink→violet accents ----
val BgTop = Color(0xFF0A0612)
val BgMid = Color(0xFF160A28)
val BgBottom = Color(0xFF0A0612)
val CardBg = Color(0xFF181226)
val CardBorder = Color(0x1FFFFFFF)
val AccentPink = Color(0xFFFF4D8D)
val AccentViolet = Color(0xFF9B5DE5)
val AccentCyan = Color(0xFF34E0EA)
val TextHi = Color(0xFFF3EFFA)
val TextLo = Color(0xFF9D93B4)

val AccentGradient = Brush.linearGradient(listOf(AccentPink, AccentViolet))

val WatchieDark = darkColorScheme(
    primary = AccentPink,
    onPrimary = Color.White,
    secondary = AccentViolet,
    background = BgTop,
    surface = CardBg,
    onSurface = TextHi,
    onBackground = TextHi,
)

/** Full-screen background: a soft vertical gradient with a glow toward the top. */
@Composable
fun ScreenBg(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom))),
    ) {
        content()
    }
}

/** A premium gradient pill button. */
@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(if (enabled) AccentGradient else Brush.linearGradient(listOf(Color(0xFF3A3348), Color(0xFF302A40))))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** A round avatar showing the first letter, tinted from the username. */
@Composable
fun Avatar(username: String, size: Int = 44) {
    val seed = username.sumOf { it.code }
    val hues = listOf(
        Brush.linearGradient(listOf(AccentPink, AccentViolet)),
        Brush.linearGradient(listOf(AccentViolet, AccentCyan)),
        Brush.linearGradient(listOf(AccentCyan, AccentPink)),
        Brush.linearGradient(listOf(Color(0xFFFF7AA8), Color(0xFFFFB36B))),
    )
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(hues[seed % hues.size]),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            username.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.42f).sp,
        )
    }
}

/** A glassy dark text field. */
@Composable
fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    isEmail: Boolean = false,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isEmail) KeyboardOptions(keyboardType = KeyboardType.Email) else KeyboardOptions.Default,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentPink,
            unfocusedBorderColor = CardBorder,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg,
            focusedLabelColor = AccentPink,
            unfocusedLabelColor = TextLo,
            cursorColor = AccentPink,
            focusedTextColor = TextHi,
            unfocusedTextColor = TextHi,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A dark rounded card with a hairline border. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(CardBg)
        .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
    Box(modifier = if (onClick != null) base.clickable(onClick = onClick) else base) {
        content()
    }
}

/** Brand wordmark with a gradient fill feel (accent color). */
@Composable
fun BrandTitle(size: Int = 40) {
    Text("Watchie", color = AccentPink, fontWeight = FontWeight.Black, fontSize = size.sp)
}
