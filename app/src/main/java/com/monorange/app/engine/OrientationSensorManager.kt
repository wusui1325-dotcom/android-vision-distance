package com.monorange.app.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt

class OrientationSensorManager(
    context: Context,
    private val onOrientationChanged: (pitchDeg: Float, rollDeg: Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gravitySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var smoothedPitch = -25f
    private var smoothedRoll = 0f
    private var initialized = false

    @Volatile
    var smoothingAlpha: Float = 0.18f

    fun start() {
        val primary = rotationSensor ?: gravitySensor
        primary?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rawPitch: Float
        val rawRoll: Float

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                // Remap for portrait camera aiming straight ahead:
                // When device is upright (vertical), pitch = 0 deg.
                // Tilting top forward/downward toward ground yields negative pitch.
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedMatrix
                )
                SensorManager.getOrientation(remappedMatrix, orientationAngles)
                rawPitch = -Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            }

            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                val horiz = sqrt((gx * gx + gy * gy).toDouble()).toFloat().coerceAtLeast(0.001f)
                // Positive gz means screen faces up -> camera points down
                rawPitch = -Math.toDegrees(atan2(gz.toDouble(), horiz.toDouble())).toFloat()
                rawRoll = Math.toDegrees(atan2(gx.toDouble(), gy.toDouble())).toFloat()
            }

            else -> return
        }

        val alpha = smoothingAlpha.coerceIn(0.05f, 0.85f)
        if (!initialized) {
            smoothedPitch = rawPitch
            smoothedRoll = rawRoll
            initialized = true
        } else {
            smoothedPitch += alpha * (rawPitch - smoothedPitch)
            smoothedRoll += alpha * (rawRoll - smoothedRoll)
        }

        onOrientationChanged(smoothedPitch, smoothedRoll)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
