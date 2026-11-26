package com.wusper.vision.distance.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.wusper.vision.distance.model.RotationMatrix3x3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OrientationSensorManager(
    context: Context,
    private val onOrientationChanged: (
        pitchDeg: Float,
        rollDeg: Float,
        azimuthDeg: Float,
        rotationMatrix: RotationMatrix3x3
    ) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Prefer GAME_ROTATION_VECTOR (gyro + accelerometer only, ZERO magnetometer indoor drift!)
    private val gameRotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
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
        val primary = gameRotationSensor ?: rotationSensor ?: gravitySensor
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
        val matrix3x3: RotationMatrix3x3

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                matrix3x3 = RotationMatrix3x3.fromFloatArray(rotationMatrix)

                // Remapped Euler angles for telemetry HUD readout
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

                // Construct synthetic rotation matrix from pitch alone when gyro is unavailable
                val pRad = Math.toRadians(rawPitch.toDouble())
                val cp = cos(pRad).toFloat()
                val sp = sin(pRad).toFloat()
                matrix3x3 = RotationMatrix3x3(
                    1f, 0f, 0f,
                    0f, -sp, -cp,
                    0f, cp, -sp
                )
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
            val azDelta = DistanceCalculator.shortestAngleDeltaDeg(smoothedAzimuth, rawAzimuth)
            smoothedAzimuth = ((smoothedAzimuth + alpha * azDelta + 540f) % 360f) - 180f
        }

        // Deliver unlagged 3x3 rotation matrix for zero-drift 3D visual tracking
        onOrientationChanged(smoothedPitch, smoothedRoll, smoothedAzimuth, matrix3x3)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
