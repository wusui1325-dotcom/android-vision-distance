package com.monorange.app.engine

import com.monorange.app.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        // Vertical FOV = 90 deg -> tan(45 deg) = 1.0 -> full frame height = 2*D
        // Object spans 0.5 of frame -> realSize = D -> D = 1.70m
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
