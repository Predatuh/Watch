package com.cutenotes.watch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Detects the "raise your wrist to look at the watch" gesture and calls [onRaise].
 *
 * How it works: the accelerometer's Z value is roughly how much the screen is
 * facing up. When your wrist is down the screen faces sideways/down (low Z);
 * when you flip it up to read, the screen faces you (high Z). We fire once on
 * that low -> high transition, then "re-arm" only after it drops low again, so
 * holding the watch up doesn't fire repeatedly.
 *
 * @param enabled  only listen while true (e.g. on the home screen)
 * @param onRaise  called on the watch's main thread when a raise is detected
 */
@Composable
fun RaiseToWakeEffect(enabled: Boolean, onRaise: () -> Unit) {
    val context = LocalContext.current
    val currentOnRaise by rememberUpdatedState(onRaise)

    DisposableEffect(enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val sensorManager =
                context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            var armed = true
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val z = event.values[2]
                    if (z < 3f) armed = true            // wrist lowered: ready to fire again
                    if (armed && z > 7f) {              // wrist raised to viewing position
                        armed = false
                        currentOnRaise()
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (accelerometer != null) {
                sensorManager.registerListener(
                    listener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI,
                )
            }
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }
}
