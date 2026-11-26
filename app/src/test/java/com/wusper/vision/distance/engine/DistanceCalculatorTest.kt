package com.wusper.vision.distance.engine

import com.wusper.vision.distance.model.DistanceUnit
import com.wusper.vision.distance.model.GroundAnchorPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {

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
    fun twoPointGroundDistance_rightAngle_matchesPythagorean345() {
        val pointA = GroundAnchorPoint(radialDistanceMeters = 3.0f, azimuthDeg = 0f, pitchDeg = -26f)
        val pointB = GroundAnchorPoint(radialDistanceMeters = 4.0f, azimuthDeg = 90f, pitchDeg = -20f)
        val ab = DistanceCalculator.calculateTwoPointGroundDistance(pointA, pointB)
        assertEquals(5.0f, ab, 0.02f)
    }

    @Test
    fun twoPointGroundDistance_collinear_equalsDifference() {
        val pointA = GroundAnchorPoint(radialDistanceMeters = 2.0f, azimuthDeg = 15f, pitchDeg = -37f)
        val pointB = GroundAnchorPoint(radialDistanceMeters = 5.5f, azimuthDeg = 15f, pitchDeg = -15f)
        val ab = DistanceCalculator.calculateTwoPointGroundDistance(pointA, pointB)
        assertEquals(3.5f, ab, 0.02f)
    }

    @Test
    fun twoPointGroundDistance_wrapAroundAzimuth_computesShortestAngle() {
        val delta = DistanceCalculator.shortestAngleDeltaDeg(350f, 10f)
        assertEquals(20f, delta, 0.01f)
        val pointA = GroundAnchorPoint(radialDistanceMeters = 4.0f, azimuthDeg = 350f, pitchDeg = -20f)
        val pointB = GroundAnchorPoint(radialDistanceMeters = 4.0f, azimuthDeg = 10f, pitchDeg = -20f)
        val ab = DistanceCalculator.calculateTwoPointGroundDistance(pointA, pointB)
        assertEquals(1.39f, ab, 0.02f)
    }

    @Test
    fun projectAnchorToScreenFraction_centeredAndPanned_behavesCorrectly() {
        val anchor = GroundAnchorPoint(radialDistanceMeters = 3.0f, azimuthDeg = 0f, pitchDeg = -30f)
        val (cx, cy) = DistanceCalculator.projectAnchorToScreenFraction(
            anchor = anchor,
            currentAzimuthDeg = 0f,
            currentPitchDeg = -30f,
            horizontalFovDeg = 60f,
            verticalFovDeg = 80f
        )
        assertEquals(0.5f, cx, 0.001f)
        assertEquals(0.5f, cy, 0.001f)

        // Pan camera right (+15 deg) and tilt up (-20 deg): anchor should shift left (<0.5) and down (>0.5)
        val (px, py) = DistanceCalculator.projectAnchorToScreenFraction(
            anchor = anchor,
            currentAzimuthDeg = 15f,
            currentPitchDeg = -20f,
            horizontalFovDeg = 60f,
            verticalFovDeg = 80f
        )
        assertTrue(px < 0.5f)
        assertTrue(py > 0.5f)
    }

    @Test
    fun objectHeight_atHorizontalTop_equalsCameraHeight() {
        val height = DistanceCalculator.calculateObjectHeight(
            cameraHeightMeters = 1.50f,
            horizontalDistanceMeters = 4.0f,
            topPitchFromHorizontalDeg = 0.0f
        )
        assertNotNull(height)
        assertEquals(1.50f, height!!, 0.01f)
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
