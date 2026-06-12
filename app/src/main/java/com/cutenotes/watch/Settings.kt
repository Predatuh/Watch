package com.cutenotes.watch

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * How hard the watch buzzes when a note arrives. Each option is a vibration
 * pattern (alternating off/on millisecond durations) plus an amplitude (0..255).
 * "Default" is the requested 5 fast ~0.5s pulses.
 */
enum class VibrationStrength(
    val label: String,
    val pattern: LongArray,
    val amplitude: Int,
) {
    GENTLE("Gentle", longArrayOf(0, 220, 140, 220, 140, 220), 110),
    DEFAULT("Default", longArrayOf(0, 500, 150, 500, 150, 500, 150, 500, 150, 500), 200),
    STRONG("Strong", longArrayOf(0, 600, 120, 600, 120, 600, 120, 600, 120, 600), 255),
}

/**
 * App preferences backed by SharedPreferences so they survive restarts.
 * The properties are Compose state, so screens reading them update live.
 */
class AppSettings(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("cute_notes", Context.MODE_PRIVATE)

    var vibrationEnabled by mutableStateOf(prefs.getBoolean(KEY_ENABLED, true))
        private set

    var vibrationStrength by mutableStateOf(readStrength())
        private set

    fun updateVibrationEnabled(enabled: Boolean) {
        vibrationEnabled = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun updateVibrationStrength(strength: VibrationStrength) {
        vibrationStrength = strength
        prefs.edit().putString(KEY_STRENGTH, strength.name).apply()
    }

    private fun readStrength(): VibrationStrength =
        runCatching { VibrationStrength.valueOf(prefs.getString(KEY_STRENGTH, null) ?: "DEFAULT") }
            .getOrDefault(VibrationStrength.DEFAULT)

    private companion object {
        const val KEY_ENABLED = "vibration_enabled"
        const val KEY_STRENGTH = "vibration_strength"
    }
}
