package com.wusper.vision.distance.engine

import com.wusper.vision.distance.model.CameraIntrinsics
import com.wusper.vision.distance.model.DistanceUnit
import com.wusper.vision.distance.model.GroundAnchorPoint
import com.wusper.vision.distance.model.RotationMatrix3x3
import com.wusper.vision.distance.model.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class ProjectedPoint2D(
    val normX: Float,
    val normY: Float,
    val isInFront: Boolean,
    val isOnScreen: Boolean
)

data class ClippedLine2D(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

object DistanceCalculator {

    private const val MIN_DEPRESSION_DEG = 1.2f
    private const val MAX_DEPRESSION_DEG = 88.5f
    private const val MAX_DISTANCE_METERS = 250.0f
    private const val MIN_DISTANCE_METERS = 0.05f
    private const val NEAR_PLANE_METERS = 0.05f

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
     * Intersects the camera center optical ray with the horizontal ground plane z = -cameraHeightMeters
     * in 3D world coordinates using the full 3x3 IMU rotation matrix.
     */
    fun raycastGroundPoint3D(
        rotationMatrix: RotationMatrix3x3,
        cameraHeightMeters: Float,
        pitchOffsetDeg: Float = 0f
    ): Vec3? {
        if (cameraHeightMeters <= 0.05f) return null
        val offRad = Math.toRadians((-pitchOffsetDeg).toDouble())
        // Camera optical axis in device coords is (0, 0, -1), rotated around X by calibration offset
        val localRay = Vec3(
            x = 0f,
            y = sin(offRad).toFloat(),
            z = (-cos(offRad)).toFloat()
        )
        val worldRay = rotationMatrix.transformDeviceToWorld(localRay)
        // Ray must point below horizon (worldRay.z < -sin(1.2 deg))
        if (worldRay.z >= -0.021f) return null

        val t = cameraHeightMeters / (-worldRay.z)
        val horizDist = hypot((worldRay.x * t).toDouble(), (worldRay.y * t).toDouble()).toFloat()
        if (horizDist < MIN_DISTANCE_METERS || horizDist > MAX_DISTANCE_METERS) return null

        return Vec3(
            x = worldRay.x * t,
            y = worldRay.y * t,
            z = -cameraHeightMeters
        )
    }

    /**
     * Exact 3D Euclidean distance between two ground-plane world points.
     */
    fun calculateWorldDistance3D(a: Vec3, b: Vec3): Float {
        val dx = (b.x - a.x).toDouble()
        val dy = (b.y - a.y).toDouble()
        val dz = (b.z - a.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz).toFloat().coerceAtMost(MAX_DISTANCE_METERS)
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
     * Two-point ground-plane distance: uses exact 3D world positions directly.
     */
    fun calculateTwoPointGroundDistance(
        pointA: GroundAnchorPoint,
        pointB: GroundAnchorPoint
    ): Float = calculateWorldDistance3D(pointA.worldPos, pointB.worldPos)

    /**
     * Computes the effective (tan(HFOV/2), tan(VFOV/2)) visible inside a PreviewView using
     * ScaleType.FILL_CENTER (center-crop). This eliminates horizontal sliding when a 4:3 sensor
     * is center-cropped onto a tall 20:9 smartphone display.
     */
    fun effectiveHalfFovTangents(
        intrinsics: CameraIntrinsics,
        viewWidthPx: Float,
        viewHeightPx: Float,
        focalCalibrationFactor: Float = 1.0f
    ): Pair<Float, Float> {
        val safeW = viewWidthPx.coerceAtLeast(1f)
        val safeH = viewHeightPx.coerceAtLeast(1f)
        val viewAspect = safeW / safeH

        val rawTanHalfV = tan(Math.toRadians((intrinsics.verticalFovDeg.coerceIn(20f, 130f) / 2f).toDouble())).toFloat()
        val rawTanHalfH = tan(Math.toRadians((intrinsics.horizontalFovDeg.coerceIn(15f, 120f) / 2f).toDouble())).toFloat()
        val sensorAspect = (rawTanHalfH / rawTanHalfV).coerceIn(0.3f, 3.0f)

        val (cropTanH, cropTanV) = if (viewAspect < sensorAspect) {
            // Tall portrait display: vertical FOV matches sensor, horizontal FOV is center-cropped
            (rawTanHalfV * viewAspect) to rawTanHalfV
        } else {
            // Wider display: horizontal FOV matches sensor, vertical FOV is center-cropped
            rawTanHalfH to (rawTanHalfH / viewAspect)
        }

        val scale = focalCalibrationFactor.coerceIn(0.60f, 1.50f)
        return (cropTanH / scale) to (cropTanV / scale)
    }

    /**
     * Transforms a 3D world point into current camera/device coordinates, accounting for pitch offset.
     */
    fun worldToCameraSpace(
        worldPos: Vec3,
        rotationMatrix: RotationMatrix3x3,
        pitchOffsetDeg: Float = 0f
    ): Vec3 {
        val dev = rotationMatrix.transformWorldToDevice(worldPos)
        if (abs(pitchOffsetDeg) < 1e-4f) return dev
        val rad = Math.toRadians(pitchOffsetDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return Vec3(
            x = dev.x,
            y = dev.y * c - dev.z * s,
            z = dev.y * s + dev.z * c
        )
    }

    /**
     * Projects a 3D world point onto normalized screen coordinates [0..1] using the full 3x3
     * rotation matrix and FILL_CENTER-corrected FOV. Naturally tracks pitch, yaw, AND roll.
     */
    fun projectWorldPointToScreen(
        worldPos: Vec3,
        rotationMatrix: RotationMatrix3x3,
        intrinsics: CameraIntrinsics,
        viewWidthPx: Float,
        viewHeightPx: Float,
        focalCalibrationFactor: Float = 1.0f,
        pitchOffsetDeg: Float = 0f
    ): ProjectedPoint2D {
        val cam = worldToCameraSpace(worldPos, rotationMatrix, pitchOffsetDeg)
        val depth = -cam.z
        val (tanHalfH, tanHalfV) = effectiveHalfFovTangents(
            intrinsics = intrinsics,
            viewWidthPx = viewWidthPx,
            viewHeightPx = viewHeightPx,
            focalCalibrationFactor = focalCalibrationFactor
        )

        if (depth <= NEAR_PLANE_METERS) {
            // Behind camera: provide edge direction so UI can show off-screen direction indicator
            val dirLen = hypot(cam.x.toDouble(), cam.y.toDouble()).toFloat().coerceAtLeast(1e-4f)
            val edgeX = 0.5f + (cam.x / dirLen) * 1.5f
            val edgeY = 0.5f - (cam.y / dirLen) * 1.5f
            return ProjectedPoint2D(
                normX = edgeX,
                normY = edgeY,
                isInFront = false,
                isOnScreen = false
            )
        }

        val normX = 0.5f + (cam.x / depth) / (2f * tanHalfH)
        val normY = 0.5f - (cam.y / depth) / (2f * tanHalfV)
        val onScreen = normX in 0f..1f && normY in 0f..1f
        return ProjectedPoint2D(
            normX = normX,
            normY = normY,
            isInFront = true,
            isOnScreen = onScreen
        )
    }

    /**
     * Clips a 3D world-space line segment against the camera near plane (depth >= 0.05m)
     * BEFORE perspective projection. Prevents line inversion/explosion when one endpoint
     * rotates behind the camera (>90 deg off-axis).
     */
    fun projectWorldSegmentClipped(
        worldA: Vec3,
        worldB: Vec3,
        rotationMatrix: RotationMatrix3x3,
        intrinsics: CameraIntrinsics,
        viewWidthPx: Float,
        viewHeightPx: Float,
        focalCalibrationFactor: Float = 1.0f,
        pitchOffsetDeg: Float = 0f
    ): ClippedLine2D? {
        var camA = worldToCameraSpace(worldA, rotationMatrix, pitchOffsetDeg)
        var camB = worldToCameraSpace(worldB, rotationMatrix, pitchOffsetDeg)

        val depthA = -camA.z
        val depthB = -camB.z

        if (depthA <= NEAR_PLANE_METERS && depthB <= NEAR_PLANE_METERS) {
            return null
        }

        if (depthA < NEAR_PLANE_METERS) {
            val t = (NEAR_PLANE_METERS - depthA) / (depthB - depthA)
            camA = Vec3(
                x = camA.x + t * (camB.x - camA.x),
                y = camA.y + t * (camB.y - camA.y),
                z = -NEAR_PLANE_METERS
            )
        } else if (depthB < NEAR_PLANE_METERS) {
            val t = (NEAR_PLANE_METERS - depthB) / (depthA - depthB)
            camB = Vec3(
                x = camB.x + t * (camA.x - camB.x),
                y = camB.y + t * (camA.y - camB.y),
                z = -NEAR_PLANE_METERS
            )
        }

        val (tanHalfH, tanHalfV) = effectiveHalfFovTangents(
            intrinsics = intrinsics,
            viewWidthPx = viewWidthPx,
            viewHeightPx = viewHeightPx,
            focalCalibrationFactor = focalCalibrationFactor
        )

        val dA = (-camA.z).coerceAtLeast(NEAR_PLANE_METERS)
        val dB = (-camB.z).coerceAtLeast(NEAR_PLANE_METERS)

        val ax = (0.5f + (camA.x / dA) / (2f * tanHalfH)) * viewWidthPx
        val ay = (0.5f - (camA.y / dA) / (2f * tanHalfV)) * viewHeightPx
        val bx = (0.5f + (camB.x / dB) / (2f * tanHalfH)) * viewWidthPx
        val by = (0.5f - (camB.y / dB) / (2f * tanHalfV)) * viewHeightPx

        return ClippedLine2D(ax, ay, bx, by)
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
