package com.wusper.vision.distance.engine

import com.wusper.vision.distance.model.CameraIntrinsics
import com.wusper.vision.distance.model.DistanceUnit
import com.wusper.vision.distance.model.GroundAnchorPoint
import com.wusper.vision.distance.model.RotationMatrix3x3
import com.wusper.vision.distance.model.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class DistanceCalculatorTest {

    private fun rotationMatrixForPitchYaw(pitchDeg: Float, yawDeg: Float): RotationMatrix3x3 {
        // Upright phone facing +Y (North) has R0 = [[1,0,0],[0,0,-1],[0,1,0]]
        // Apply pitch around device X, then yaw clockwise around world Z
        val p = Math.toRadians(pitchDeg.toDouble())
        val y = Math.toRadians(yawDeg.toDouble())
        val cp = cos(p).toFloat()
        val sp = sin(p).toFloat()
        val cy = cos(y).toFloat()
        val sy = sin(y).toFloat()

        // Device axes in world coords when yaw=0, pitch=p:
        // X_dev = (1, 0, 0), Y_dev = (0, -sp, cp), Z_dev = (0, -cp, -sp)
        // Rotate around world Z by -y (clockwise azimuth):
        // x' = x*cy + y*sy, y' = -x*sy + y*cy
        val xx = cy; val xy = -sp * sy; val xz = -cp * sy
        val yx = -sy; val yy = -sp * cy; val yz = -cp * cy
        val zx = 0f; val zy = cp; val zz = -sp
        return RotationMatrix3x3(xx, xy, xz, yx, yy, yz, zx, zy, zz)
    }

    @Test
    fun groundDistance_at45Degrees_equalsCameraHeight() {
        val dist = DistanceCalculator.calculateGroundDistance(
            cameraHeightMeters = 1.50f,
            pitchFromHorizontalDeg = -45.0f
        )
        assertNotNull(dist)
        assertEquals(1.50f, dist!!, 0.02f)
    }

    @Test
    fun groundDistance_aboveHorizon_returnsNull() {
        val dist = DistanceCalculator.calculateGroundDistance(
            cameraHeightMeters = 1.50f,
            pitchFromHorizontalDeg = 5.0f
        )
        assertNull(dist)
    }

    @Test
    fun raycastGroundPoint3D_andReprojectBackToCenter_isExact() {
        val rot = rotationMatrixForPitchYaw(pitchDeg = -45f, yawDeg = 30f)
        val worldPt = DistanceCalculator.raycastGroundPoint3D(rot, cameraHeightMeters = 1.50f)
        assertNotNull(worldPt)
        assertEquals(-1.50f, worldPt!!.z, 0.001f)

        val proj = DistanceCalculator.projectWorldPointToScreen(
            worldPos = worldPt,
            rotationMatrix = rot,
            intrinsics = CameraIntrinsics(),
            viewWidthPx = 1080f,
            viewHeightPx = 2400f
        )
        assertTrue(proj.isInFront)
        assertTrue(proj.isOnScreen)
        assertEquals(0.5f, proj.normX, 0.002f)
        assertEquals(0.5f, proj.normY, 0.002f)
    }

    @Test
    fun projectWorldPoint_afterTurningAwayAndReturning_returnsToExactCenter() {
        val rotA = rotationMatrixForPitchYaw(pitchDeg = -35f, yawDeg = 0f)
        val worldA = DistanceCalculator.raycastGroundPoint3D(rotA, cameraHeightMeters = 1.50f)!!

        // Turn 120 deg away so point A is behind the camera
        val rotAway = rotationMatrixForPitchYaw(pitchDeg = -35f, yawDeg = 120f)
        val projAway = DistanceCalculator.projectWorldPointToScreen(
            worldPos = worldA,
            rotationMatrix = rotAway,
            intrinsics = CameraIntrinsics(),
            viewWidthPx = 1080f,
            viewHeightPx = 2400f
        )
        assertFalse(projAway.isInFront)
        assertFalse(projAway.isOnScreen)

        // Turn back to rotA: must return to exact (0.5, 0.5)
        val projBack = DistanceCalculator.projectWorldPointToScreen(
            worldPos = worldA,
            rotationMatrix = rotA,
            intrinsics = CameraIntrinsics(),
            viewWidthPx = 1080f,
            viewHeightPx = 2400f
        )
        assertTrue(projBack.isInFront)
        assertEquals(0.5f, projBack.normX, 0.002f)
        assertEquals(0.5f, projBack.normY, 0.002f)
    }

    @Test
    fun effectiveHalfFovTangents_tallScreenCropsHorizontalFov() {
        val intrinsics = CameraIntrinsics(verticalFovDeg = 66f, horizontalFovDeg = 52f)
        val (tanH43, tanV43) = DistanceCalculator.effectiveHalfFovTangents(intrinsics, 900f, 1200f)
        val (tanH209, tanV209) = DistanceCalculator.effectiveHalfFovTangents(intrinsics, 1080f, 2400f)
        // Vertical FOV stays constant on tall screen, horizontal FOV shrinks due to FILL_CENTER crop
        assertEquals(tanV43, tanV209, 0.01f)
        assertTrue(tanH209 < tanH43)
    }

    @Test
    fun projectWorldSegmentClipped_whenOneEndpointBehindCamera_clipsCleanlyWithoutNull() {
        val rotA = rotationMatrixForPitchYaw(pitchDeg = -30f, yawDeg = 0f)
        val rotB = rotationMatrixForPitchYaw(pitchDeg = -30f, yawDeg = 110f)
        val worldA = DistanceCalculator.raycastGroundPoint3D(rotA, 1.50f)!!
        val worldB = DistanceCalculator.raycastGroundPoint3D(rotB, 1.50f)!!

        // Looking at B, A is >90 deg off-axis (behind near plane), clipped segment should still project cleanly
        val clipped = DistanceCalculator.projectWorldSegmentClipped(
            worldA = worldA,
            worldB = worldB,
            rotationMatrix = rotB,
            intrinsics = CameraIntrinsics(),
            viewWidthPx = 1080f,
            viewHeightPx = 2400f
        )
        assertNotNull(clipped)
        assertEquals(540f, clipped!!.endX, 2f)
        assertEquals(1200f, clipped.endY, 2f)
    }

    @Test
    fun twoPointGroundDistance_rightAngle_matchesPythagorean345() {
        val pointA = GroundAnchorPoint(Vec3(3.0f, 0f, -1.5f), 3.0f, 0f, -26f)
        val pointB = GroundAnchorPoint(Vec3(0f, 4.0f, -1.5f), 4.0f, 90f, -20f)
        val ab = DistanceCalculator.calculateTwoPointGroundDistance(pointA, pointB)
        assertEquals(5.0f, ab, 0.02f)
    }

    @Test
    fun objectHeight_aboveHorizon_addsVerticalOffset() {
        val height = DistanceCalculator.calculateObjectHeight(
            cameraHeightMeters = 1.50f,
            horizontalDistanceMeters = 3.0f,
            topPitchFromHorizontalDeg = 45.0f
        )
        assertNotNull(height)
        assertEquals(4.50f, height!!, 0.02f)
    }

    @Test
    fun pinholeDistance_knownFovAndHalfFrame_computesAccurately() {
        val dist = DistanceCalculator.calculatePinholeDistance(
            realSizeMeters = 1.70f,
            objectFractionOfViewHeight = 0.50f,
            verticalFovDeg = 90.0f,
            calibrationFactor = 1.0f
        )
        assertNotNull(dist)
        assertEquals(1.70f, dist!!, 0.02f)
    }

    @Test
    fun formatDistance_convertsAllUnitsCorrectly() {
        assertEquals("2.00" to "m", DistanceCalculator.formatDistance(2.0f, DistanceUnit.METERS))
        assertEquals("200.0" to "cm", DistanceCalculator.formatDistance(2.0f, DistanceUnit.CENTIMETERS))
        assertEquals("3.28" to "ft", DistanceCalculator.formatDistance(1.0f, DistanceUnit.FEET))
        assertEquals("39.4" to "in", DistanceCalculator.formatDistance(1.0f, DistanceUnit.INCHES))
        assertEquals("--" to "m", DistanceCalculator.formatDistance(null, DistanceUnit.METERS))
    }
}
