package com.wusper.vision.distance.engine

import com.wusper.vision.distance.model.DistanceUnit
import com.wusper.vision.distance.model.GroundAnchorPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan

object DistanceCalculator {

    private const val MIN_DEPRESSION_DEG = 1.2f
    private const val MAX_DEPRESSION_DEG = 88.5f
    private const val MAX_DISTANCE_METERS = 250.0f
    private const val MIN_DISTANCE_METERS = 0.05f

    /**
     * Ground trigonometry:
     * pitchFromHorizontalDeg is negative when aiming downward toward the ground.
     * D = H / tan(depressionAngle)
     */
    fun calculateGroundDistance(
        cameraHeightMeters: Float,
        pitchFromHorizontalDeg: Float
    ): Float? {
        if (cameraHeightMeters <= 0.05f) return null
        val depressionDeg = -pitchFromHorizontalDeg
        if (depressionDeg < MIN_DEPRESSION_DEG || depressionDeg > MAX_DEPRESSION_DEG) {
            return null
        }
        val rad = Math.toRadians(depressionDeg.toDouble())
        val tanVal = tan(rad)
        if (tanVal <= 1e-6) return null
        val distance = (cameraHeightMeters / tanVal).toFloat()
        return distance.coerceIn(MIN_DISTANCE_METERS, MAX_DISTANCE_METERS)
    }

    /**
     * Shortest signed angular difference in degrees in range [-180, 180].
     */
    fun shortestAngleDeltaDeg(fromDeg: Float, toDeg: Float): Float {
        var delta = (toDeg - fromDeg) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    /**
     * Two-point ground-plane distance between Start Point A and End Point B
     * captured from the same standing position using camera center crosshair + azimuth/pitch:
     * D_AB = sqrt(rA^2 + rB^2 - 2 * rA * rB * cos(deltaAzimuth))
     */
    fun calculateTwoPointGroundDistance(
        pointA: GroundAnchorPoint,
        pointB: GroundAnchorPoint
    ): Float {
        val rA = pointA.radialDistanceMeters.toDouble()
        val rB = pointB.radialDistanceMeters.toDouble()
        val deltaDeg = shortestAngleDeltaDeg(pointA.azimuthDeg, pointB.azimuthDeg).toDouble()
        val deltaRad = Math.toRadians(deltaDeg)
        val sq = (rA * rA + rB * rB - 2.0 * rA * rB * cos(deltaRad)).coerceAtLeast(0.0)
        return sqrt(sq).toFloat().coerceAtMost(MAX_DISTANCE_METERS)
    }

    /**
     * Projects a captured GroundAnchorPoint onto normalized camera preview coordinates
     * where (0.5f, 0.5f) is the screen center. Values outside [0f, 1f] indicate the anchor
     * has moved off-screen as the user panned or tilted.
     */
    fun projectAnchorToScreenFraction(
        anchor: GroundAnchorPoint,
        currentAzimuthDeg: Float,
        currentPitchDeg: Float,
        horizontalFovDeg: Float,
        verticalFovDeg: Float
    ): Pair<Float, Float> {
        val deltaAz = shortestAngleDeltaDeg(currentAzimuthDeg, anchor.azimuthDeg).coerceIn(-75f, 75f)
        val deltaPitch = (anchor.pitchDeg - currentPitchDeg).coerceIn(-75f, 75f)

        val halfHFovTan = tan(Math.toRadians((horizontalFovDeg.coerceIn(20f, 130f) / 2f).toDouble()))
        val halfVFovTan = tan(Math.toRadians((verticalFovDeg.coerceIn(20f, 130f) / 2f).toDouble()))

        val normX = (0.5 + 0.5 * (tan(Math.toRadians(deltaAz.toDouble())) / halfHFovTan)).toFloat()
        val normY = (0.5 - 0.5 * (tan(Math.toRadians(deltaPitch.toDouble())) / halfVFovTan)).toFloat()

        return normX.coerceIn(-1.5f, 2.5f) to normY.coerceIn(-1.5f, 2.5f)
    }

    /**
     * Two-point object height calculation:
     * height = H + D * tan(alpha)
     */
    fun calculateObjectHeight(
        cameraHeightMeters: Float,
        horizontalDistanceMeters: Float,
        topPitchFromHorizontalDeg: Float
    ): Float? {
        if (horizontalDistanceMeters <= 0.05f) return null
        if (abs(topPitchFromHorizontalDeg) > 85f) return null
        val rad = Math.toRadians(topPitchFromHorizontalDeg.toDouble())
        val height = (cameraHeightMeters + horizontalDistanceMeters * tan(rad)).toFloat()
        return if (height > 0.02f) height.coerceAtMost(200f) else null
    }

    /**
     * Pinhole camera distance using known real-world object size and vertical caliper fraction.
     */
    fun calculatePinholeDistance(
        realSizeMeters: Float,
        objectFractionOfViewHeight: Float,
        verticalFovDeg: Float,
        calibrationFactor: Float = 1.0f
    ): Float? {
        if (realSizeMeters <= 0.005f) return null
        if (objectFractionOfViewHeight !in 0.02f..0.98f) return null
        if (verticalFovDeg !in 15f..150f) return null
        val halfFovRad = Math.toRadians((verticalFovDeg / 2.0f).toDouble())
        val denom = 2.0 * objectFractionOfViewHeight * tan(halfFovRad)
        if (denom <= 1e-6) return null
        val distance = ((realSizeMeters / denom) * calibrationFactor).toFloat()
        return distance.coerceIn(MIN_DISTANCE_METERS, MAX_DISTANCE_METERS)
    }

    /**
     * Convert meters to display value + unit symbol.
     */
    fun formatDistance(meters: Float?, unit: DistanceUnit): Pair<String, String> {
        if (meters == null || meters.isNaN() || meters.isInfinite()) {
            return "--" to unit.symbol
        }
        return when (unit) {
            DistanceUnit.METERS -> String.format("%.2f", meters) to "m"
            DistanceUnit.CENTIMETERS -> String.format("%.1f", meters * 100f) to "cm"
            DistanceUnit.FEET -> String.format("%.2f", meters * 3.28084f) to "ft"
            DistanceUnit.INCHES -> String.format("%.1f", meters * 39.3701f) to "in"
        }
    }
}
