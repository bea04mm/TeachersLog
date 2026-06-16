package pt.ipt.dama.teacherslog.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * ShakeDetector listens for accelerometer changes to identify a device shake gesture.
 * 
 * Logic based on standard gravity acceleration thresholds.
 *
 * @param onShake Callback executed when a shake is detected.
 */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var lastShakeTimestamp: Long = 0

    /**
     * Called when sensor values have changed.
     * Calculates the G-Force to determine if the movement qualifies as a shake.
     */
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // gForce will be close to 1 when there is no movement.
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            
            // Ignore shake events too close to each other (debounce)
            if (lastShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }

            lastShakeTimestamp = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not required for shake detection
    }

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
    }
}
