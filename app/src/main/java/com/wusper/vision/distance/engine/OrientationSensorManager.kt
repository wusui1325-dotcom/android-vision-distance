package com.wusper.vision.distance.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt

class OrientationSensorManager(
    context: Context,
    private val onOrientationChanged: (pitchDeg: Float, rollDeg: Float, azimuthDeg: Float) -> Unit
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
    private var smoothedAzimuth = 0f
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
        val rawAzimuth: Float

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedMatrix
                )
                SensorManager.getOrientation(remappedMatrix, orientationAngles)
                rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                rawPitch = -Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            }

            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                val horiz = sqrt((gx * gx + gy * gy).toDouble()).toFloat().coerceAtLeast(0.001f)
                rawAzimuth = 0f
                rawPitch = -Math.toDegrees(atan2(gz.toDouble(), horiz.toDouble())).toFloat()
                rawRoll = Math.toDegrees(atan2(gx.toDouble(), gy.toDouble())).toFloat()
            }

            else -> return
        }

        val alpha = smoothingAlpha.coerceIn(0.05f, 0.85f)
        if (!initialized) {
            smoothedPitch = rawPitch
            smoothedRoll = rawRoll
            smoothedAzimuth = rawAzimuth
            initialized = true
        } else {
            smoothedPitch += alpha * (rawPitch - smoothedPitch)
            smoothedRoll += alpha * (rawRoll - smoothedRoll)
            // Wrap-safe shortest angular delta smoothing for azimuth across +/-180 deg
            val azDelta = DistanceCalculator.shortestAngleDeltaDeg(smoothedAzimuth, rawAzimuth)
            smoothedAzimuth = ((smoothedAzimuth + alpha * azDelta + 540f) % 360f) - 180f
        }

        onOrientationChanged(smoothedPitch, smoothedRoll, smoothedAzimuth)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
