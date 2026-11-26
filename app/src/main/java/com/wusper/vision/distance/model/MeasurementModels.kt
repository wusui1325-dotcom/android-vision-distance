package com.wusper.vision.distance.model

enum class MeasurementMode(
    val shortLabel: String,
    val label: String,
    val shortHint: String
) {
    GROUND_PITCH("地面", "地面測距", "準星對準目標底部觸地點"),
    TWO_POINT("兩點", "兩點測距", "準星對準起點 A 後設定，再瞄準終點 B"),
    OBJECT_REF("比對", "物體比對", "拖曳卡尺框住已知尺寸目標"),
    HEIGHT_MEASURE("測高", "兩點測高", "先鎖定底部，再瞄準頂端")
}

enum class DistanceUnit(
    val symbol: String,
    val label: String
) {
    METERS("m", "公尺 (m)"),
    CENTIMETERS("cm", "公分 (cm)"),
    FEET("ft", "英尺 (ft)"),
    INCHES("in", "英寸 (in)")
}

enum class ReferencePreset(
    val label: String,
    val realSizeMeters: Float,
    val key: String
) {
    PERSON("成人 1.7m", 1.70f, "person"),
    DOOR("房門 2.05m", 2.05f, "door"),
    CAR("汽車 1.8m", 1.80f, "car"),
    A4_PAPER("A4 紙", 0.297f, "a4"),
    CREDIT_CARD("銀行卡", 0.0856f, "card"),
    CUSTOM("自訂尺寸", 1.00f, "custom")
}

data class CameraIntrinsics(
    val focalLengthMm: Float = 4.25f,
    val sensorHeightMm: Float = 5.6f,
    val sensorWidthMm: Float = 4.2f,
    val verticalFovDeg: Float = 66.0f,
    val horizontalFovDeg: Float = 52.0f
)

data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * Immutable 3x3 row-major rotation matrix mapping device coordinates -> world coordinates.
 * Using primitive fields ensures clean structural equality in Compose StateFlow.
 */
data class RotationMatrix3x3(
    val m00: Float = 1f, val m01: Float = 0f, val m02: Float = 0f,
    val m10: Float = 0f, val m11: Float = 0f, val m12: Float = -1f,
    val m20: Float = 0f, val m21: Float = 1f, val m22: Float = 0f
) {
    fun transformDeviceToWorld(v: Vec3): Vec3 = Vec3(
        x = m00 * v.x + m01 * v.y + m02 * v.z,
        y = m10 * v.x + m11 * v.y + m12 * v.z,
        z = m20 * v.x + m21 * v.y + m22 * v.z
    )

    fun transformWorldToDevice(v: Vec3): Vec3 = Vec3(
        x = m00 * v.x + m10 * v.y + m20 * v.z,
        y = m01 * v.x + m11 * v.y + m21 * v.z,
        z = m02 * v.x + m12 * v.y + m22 * v.z
    )

    companion object {
        fun fromFloatArray(a: FloatArray): RotationMatrix3x3 = RotationMatrix3x3(
            a[0], a[1], a[2],
            a[3], a[4], a[5],
            a[6], a[7], a[8]
        )
    }
}

/**
 * A 3D ground-plane anchor point in world coordinates relative to the camera origin (0, 0, 0).
 */
data class GroundAnchorPoint(
    val worldPos: Vec3,
    val radialDistanceMeters: Float,
    val azimuthDeg: Float,
    val pitchDeg: Float
)

data class MeasurementRecord(
    val id: Long,
    val timestampMs: Long,
    val mode: MeasurementMode,
    val distanceMeters: Float,
    val heightMeters: Float? = null,
    val pitchDeg: Float = 0f,
    val note: String = ""
)

data class RangefinderUiState(
    val mode: MeasurementMode = MeasurementMode.GROUND_PITCH,
    val unit: DistanceUnit = DistanceUnit.METERS,
    val darkMonoTheme: Boolean = true,
    val showGrid: Boolean = true,
    val torchEnabled: Boolean = false,
    val isLocked: Boolean = false,
    val hapticEnabled: Boolean = true,

    // Live sensor telemetry (smoothed for display) + unlagged 3D rotation matrix for visual projection
    val pitchDeg: Float = -25f,
    val rollDeg: Float = 0f,
    val azimuthDeg: Float = 0f,
    val rotationMatrix: RotationMatrix3x3 = RotationMatrix3x3(),
    val pitchOffsetDeg: Float = 0f,
    val smoothingAlpha: Float = 0.18f,

    // Calibration & parameters
    val cameraHeightMeters: Float = 1.50f,
    val focalCalibrationFactor: Float = 1.00f,
    val intrinsics: CameraIntrinsics = CameraIntrinsics(),

    // Object reference (Pinhole) mode state
    val selectedPreset: ReferencePreset = ReferencePreset.PERSON,
    val customObjectSizeMeters: Float = 1.00f,
    val caliperTopFraction: Float = 0.28f,
    val caliperBottomFraction: Float = 0.72f,

    // Two-point height measurement state
    val baseLockedDistanceMeters: Float? = null,

    // Two-point ground distance (A -> B) state
    val startPointA: GroundAnchorPoint? = null,
    val endPointB: GroundAnchorPoint? = null,
    val liveCursorWorldPos: Vec3? = null,
    val twoPointDeltaAngleDeg: Float? = null,
    val liveCursorGroundDistanceMeters: Float? = null,

    // Live / locked outputs
    val liveDistanceMeters: Float? = null,
    val liveHeightMeters: Float? = null,

    // Saved history
    val history: List<MeasurementRecord> = emptyList(),
    val statusBanner: String? = null
) {
    val activeReferenceMeters: Float
        get() = if (selectedPreset == ReferencePreset.CUSTOM) {
            customObjectSizeMeters
        } else {
            selectedPreset.realSizeMeters
        }

    val caliperSpanFraction: Float
        get() = (caliperBottomFraction - caliperTopFraction).coerceIn(0.04f, 0.92f)
}
