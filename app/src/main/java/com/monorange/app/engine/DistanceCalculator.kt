package com.monorange.app.engine

import com.monorange.app.model.DistanceUnit
import kotlin.math.abs
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
     * Two-point object height calculation:
     * Given camera height H, locked horizontal distance D to base,
     * and pitch angle alpha to object top (positive = above horizon, negative = below horizon):
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
     * Full vertical scene height at distance D is: 2 * D * tan(verticalFov / 2)
     * Therefore: D = (realSize / (2 * fraction * tan(verticalFov / 2))) * calibrationFactor
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
