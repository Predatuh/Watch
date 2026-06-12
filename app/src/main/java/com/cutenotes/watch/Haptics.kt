package com.cutenotes.watch

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibration for when a note arrives. The pattern and strength come from the
 * user's settings (default: 5 fast ~0.5s buzzes).
 */
object Haptics {

    /** Buzz for an incoming note, respecting the on/off + strength settings. */
    fun playNoteBuzz(context: Context, settings: AppSettings) {
        if (!settings.vibrationEnabled) return
        buzz(context, settings.vibrationStrength)
    }

    /** Always buzz once at the given strength — used by the Settings "test" button. */
    fun preview(context: Context, strength: VibrationStrength) {
        buzz(context, strength)
    }

    private fun buzz(context: Context, strength: VibrationStrength) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        // amplitude 0 on the "off" segments (even indices), full strength on "on" segments.
        val amplitudes = IntArray(strength.pattern.size) { i -> if (i % 2 == 0) 0 else strength.amplitude }
        val effect = VibrationEffect.createWaveform(strength.pattern, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    private fun vibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
