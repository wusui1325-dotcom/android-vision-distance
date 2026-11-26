package com.monorange.app.model

enum class MeasurementMode(
    val label: String,
    val shortHint: String
) {
    GROUND_PITCH("地面測距", "準星對準目標底部觸地點"),
    OBJECT_REF("物體比對", "拖曳卡尺框住已知尺寸目標"),
    HEIGHT_MEASURE("兩點測高", "先鎖定底部，再瞄準頂端")
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

    // Live sensor telemetry
    val pitchDeg: Float = -25f,
    val rollDeg: Float = 0f,
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
