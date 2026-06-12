package com.cutenotes.watch

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibration for when a note arrives. Default pattern: 5 quick ~0.5s buzzes.
 *
 * The pattern is a list of millisecond durations that alternate OFF, ON, OFF,
 * ON ... so we start with 0ms off, then 500 on / 150 off, five times.
 */
object Haptics {

    // off, (buzz, gap) x5  ->  five ~half-second buzzes in fast succession.
    private val NOTE_PATTERN = longArrayOf(0, 500, 150, 500, 150, 500, 150, 500, 150, 500)

    fun playNoteBuzz(context: Context) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        // -1 means "do not repeat" — play the pattern once.
        val effect = VibrationEffect.createWaveform(NOTE_PATTERN, -1)
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
