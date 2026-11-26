package com.wusper.vision.distance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wusper.vision.distance.data.MeasurementStore
import com.wusper.vision.distance.engine.CameraIntrinsicsHelper
import com.wusper.vision.distance.engine.DistanceCalculator
import com.wusper.vision.distance.model.DistanceUnit
import com.wusper.vision.distance.model.GroundAnchorPoint
import com.wusper.vision.distance.model.MeasurementMode
import com.wusper.vision.distance.model.MeasurementRecord
import com.wusper.vision.distance.model.RangefinderUiState
import com.wusper.vision.distance.model.ReferencePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class RangefinderViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MeasurementStore(application.applicationContext)

    private val _uiState = MutableStateFlow(
        RangefinderUiState(
            unit = store.loadUnit(),
            darkMonoTheme = store.loadDarkMono(),
            hapticEnabled = store.loadHaptics(),
            cameraHeightMeters = store.loadCameraHeight(),
            focalCalibrationFactor = store.loadFocalFactor(),
            pitchOffsetDeg = store.loadPitchOffset(),
            smoothingAlpha = store.loadSmoothing(),
            intrinsics = CameraIntrinsicsHelper.readBackCameraIntrinsics(application.applicationContext),
            history = store.loadHistory()
        )
    )
    val uiState: StateFlow<RangefinderUiState> = _uiState.asStateFlow()

    fun onSensorUpdate(rawPitchDeg: Float, rawRollDeg: Float, rawAzimuthDeg: Float) {
        _uiState.update { state ->
            if (state.isLocked) return@update state
            val adjustedPitch = rawPitchDeg - state.pitchOffsetDeg
            val updated = state.copy(
                pitchDeg = adjustedPitch,
                rollDeg = rawRollDeg,
                azimuthDeg = rawAzimuthDeg
            )
            recomputeMeasurements(updated)
        }
    }

    fun selectMode(mode: MeasurementMode) {
        _uiState.update { state ->
            val reset = state.copy(
                mode = mode,
                isLocked = false,
                baseLockedDistanceMeters = if (mode == MeasurementMode.HEIGHT_MEASURE) {
                    state.baseLockedDistanceMeters
                } else {
                    null
                },
                statusBanner = null
            )
            recomputeMeasurements(reset)
        }
    }

    fun cycleUnit() {
        val units = DistanceUnit.entries
        _uiState.update { state ->
            val next = units[(units.indexOf(state.unit) + 1) % units.size]
            store.saveUnit(next)
            state.copy(unit = next)
        }
    }

    fun setUnit(unit: DistanceUnit) {
        store.saveUnit(unit)
        _uiState.update { it.copy(unit = unit) }
    }

    fun toggleDarkMonoTheme() {
        _uiState.update { state ->
            val next = !state.darkMonoTheme
            store.saveDarkMono(next)
            state.copy(darkMonoTheme = next)
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    fun toggleLock() {
        _uiState.update { state ->
            val nextLocked = !state.isLocked
            val updated = state.copy(
                isLocked = nextLocked,
                statusBanner = if (nextLocked) "已鎖定當前讀數" else null
            )
            if (!nextLocked) recomputeMeasurements(updated) else updated
        }
    }

    fun adjustCameraHeight(deltaMeters: Float) {
        _uiState.update { state ->
            val clamped = (state.cameraHeightMeters + deltaMeters).coerceIn(0.30f, 3.50f)
            store.saveCameraHeight(clamped)
            recomputeMeasurements(state.copy(cameraHeightMeters = clamped))
        }
    }

    fun setCameraHeight(meters: Float) {
        val clamped = meters.coerceIn(0.30f, 3.50f)
        store.saveCameraHeight(clamped)
        _uiState.update { state ->
            recomputeMeasurements(state.copy(cameraHeightMeters = clamped))
        }
    }

    fun setFocalCalibrationFactor(factor: Float) {
        val clamped = factor.coerceIn(0.60f, 1.50f)
        store.saveFocalFactor(clamped)
        _uiState.update { state ->
            recomputeMeasurements(state.copy(focalCalibrationFactor = clamped))
        }
    }

    fun calibrateHorizonZero() {
        _uiState.update { state ->
            val newOffset = state.pitchOffsetDeg + state.pitchDeg
            store.savePitchOffset(newOffset)
            recomputeMeasurements(
                state.copy(
                    pitchOffsetDeg = newOffset,
                    pitchDeg = 0f,
                    statusBanner = "水平零點已校準"
                )
            )
        }
    }

    fun resetPitchCalibration() {
        store.savePitchOffset(0f)
        _uiState.update { state ->
            recomputeMeasurements(
                state.copy(
                    pitchOffsetDeg = 0f,
                    statusBanner = "已重置水平校準"
                )
            )
        }
    }

    fun setSmoothingAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0.06f, 0.50f)
        store.saveSmoothing(clamped)
        _uiState.update { it.copy(smoothingAlpha = clamped) }
    }

    fun toggleHaptics() {
        _uiState.update { state ->
            val next = !state.hapticEnabled
            store.saveHaptics(next)
            state.copy(hapticEnabled = next)
        }
    }

    fun selectPreset(preset: ReferencePreset) {
        _uiState.update { state ->
            recomputeMeasurements(state.copy(selectedPreset = preset))
        }
    }

    fun setCustomObjectSize(meters: Float) {
        val clamped = meters.coerceIn(0.02f, 20.0f)
        _uiState.update { state ->
            recomputeMeasurements(
                state.copy(
                    selectedPreset = ReferencePreset.CUSTOM,
                    customObjectSizeMeters = clamped
                )
            )
        }
    }

    fun updateCaliper(topFraction: Float, bottomFraction: Float) {
        val safeTop = topFraction.coerceIn(0.08f, 0.86f)
        val safeBottom = bottomFraction.coerceIn(safeTop + 0.06f, 0.94f)
        _uiState.update { state ->
            recomputeMeasurements(
                state.copy(
                    caliperTopFraction = safeTop,
                    caliperBottomFraction = safeBottom
                )
            )
        }
    }

    /**
     * Two-point distance mode: manually set Start Point A from camera center crosshair.
     */
    fun setStartPointA() {
        _uiState.update { state ->
            val radial = DistanceCalculator.calculateGroundDistance(
                cameraHeightMeters = state.cameraHeightMeters,
                pitchFromHorizontalDeg = state.pitchDeg
            ) ?: return@update state.copy(statusBanner = "請將相機中心向下瞄準地面起點 A")

            val anchorA = GroundAnchorPoint(
                radialDistanceMeters = radial,
                azimuthDeg = state.azimuthDeg,
                pitchDeg = state.pitchDeg
            )
            recomputeMeasurements(
                state.copy(
                    startPointA = anchorA,
                    endPointB = null,
                    isLocked = false,
                    statusBanner = "已設定起點 A，請移動準星瞄準終點 B"
                )
            )
        }
    }

    /**
     * Two-point distance mode: manually set End Point B from camera center crosshair.
     */
    fun setEndPointB() {
        _uiState.update { state ->
            val anchorA = state.startPointA
                ?: return@update state.copy(statusBanner = "請先設定起始點 A")
            val radial = DistanceCalculator.calculateGroundDistance(
                cameraHeightMeters = state.cameraHeightMeters,
                pitchFromHorizontalDeg = state.pitchDeg
            ) ?: return@update state.copy(statusBanner = "請將相機中心向下瞄準地面終點 B")

            val anchorB = GroundAnchorPoint(
                radialDistanceMeters = radial,
                azimuthDeg = state.azimuthDeg,
                pitchDeg = state.pitchDeg
            )
            val abDist = DistanceCalculator.calculateTwoPointGroundDistance(anchorA, anchorB)
            val deltaAng = abs(DistanceCalculator.shortestAngleDeltaDeg(anchorA.azimuthDeg, anchorB.azimuthDeg))
            state.copy(
                endPointB = anchorB,
                liveDistanceMeters = abDist,
                twoPointDeltaAngleDeg = deltaAng,
                statusBanner = "已鎖定 A → B 兩點距離，可點擊儲存"
            )
        }
    }

    fun clearTwoPoints() {
        _uiState.update { state ->
            recomputeMeasurements(
                state.copy(
                    startPointA = null,
                    endPointB = null,
                    twoPointDeltaAngleDeg = null,
                    isLocked = false,
                    statusBanner = "已清除起點 A 與終點 B"
                )
            )
        }
    }

    fun lockBaseForHeight() {
        _uiState.update { state ->
            val groundDist = DistanceCalculator.calculateGroundDistance(
                cameraHeightMeters = state.cameraHeightMeters,
                pitchFromHorizontalDeg = state.pitchDeg
            )
            if (groundDist != null) {
                recomputeMeasurements(
                    state.copy(
                        baseLockedDistanceMeters = groundDist,
                        statusBanner = "底部已鎖定，請瞄準目標頂端"
                    )
                )
            } else {
                state.copy(statusBanner = "請向下瞄準目標底部與地面接觸點")
            }
        }
    }

    fun resetMeasurement() {
        _uiState.update { state ->
            recomputeMeasurements(
                state.copy(
                    isLocked = false,
                    baseLockedDistanceMeters = null,
                    startPointA = null,
                    endPointB = null,
                    twoPointDeltaAngleDeg = null,
                    caliperTopFraction = 0.28f,
                    caliperBottomFraction = 0.72f,
                    statusBanner = null
                )
            )
        }
    }

    fun saveCurrentMeasurement() {
        _uiState.update { state ->
            val dist = state.liveDistanceMeters ?: return@update state.copy(
                statusBanner = "目前無有效測距數值可儲存"
            )
            val record = MeasurementRecord(
                id = System.currentTimeMillis(),
                timestampMs = System.currentTimeMillis(),
                mode = state.mode,
                distanceMeters = dist,
                heightMeters = if (state.mode == MeasurementMode.HEIGHT_MEASURE) {
                    state.liveHeightMeters
                } else null,
                pitchDeg = state.pitchDeg,
                note = when (state.mode) {
                    MeasurementMode.GROUND_PITCH -> "機高 ${String.format("%.2f", state.cameraHeightMeters)}m"
                    MeasurementMode.TWO_POINT -> {
                        val a = state.startPointA?.radialDistanceMeters
                        val b = state.endPointB?.radialDistanceMeters ?: state.liveCursorGroundDistanceMeters
                        val ang = state.twoPointDeltaAngleDeg
                        if (a != null && b != null && ang != null) {
                            String.format("A %.2fm → B %.2fm (Δ%.1f°)", a, b, ang)
                        } else {
                            "兩點間測距"
                        }
                    }
                    MeasurementMode.OBJECT_REF -> "參考：${state.selectedPreset.label}"
                    MeasurementMode.HEIGHT_MEASURE -> "兩點測高"
                }
            )
            val updatedList = listOf(record) + state.history
            store.saveHistory(updatedList)
            state.copy(
                history = updatedList,
                statusBanner = "已儲存至測距紀錄"
            )
        }
    }

    fun deleteRecord(id: Long) {
        _uiState.update { state ->
            val updated = state.history.filterNot { it.id == id }
            store.saveHistory(updated)
            state.copy(history = updated)
        }
    }

    fun clearHistory() {
        store.saveHistory(emptyList())
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    private fun recomputeMeasurements(state: RangefinderUiState): RangefinderUiState {
        return when (state.mode) {
            MeasurementMode.GROUND_PITCH -> {
                val dist = DistanceCalculator.calculateGroundDistance(
                    cameraHeightMeters = state.cameraHeightMeters,
                    pitchFromHorizontalDeg = state.pitchDeg
                )
                state.copy(liveDistanceMeters = dist, liveHeightMeters = null)
            }

            MeasurementMode.TWO_POINT -> {
                val cursorDist = DistanceCalculator.calculateGroundDistance(
                    cameraHeightMeters = state.cameraHeightMeters,
                    pitchFromHorizontalDeg = state.pitchDeg
                )
                val anchorA = state.startPointA
                val anchorB = state.endPointB
                when {
                    anchorA != null && anchorB != null -> {
                        val ab = DistanceCalculator.calculateTwoPointGroundDistance(anchorA, anchorB)
                        val deltaAng = abs(DistanceCalculator.shortestAngleDeltaDeg(anchorA.azimuthDeg, anchorB.azimuthDeg))
                        state.copy(
                            liveCursorGroundDistanceMeters = cursorDist,
                            liveDistanceMeters = ab,
                            twoPointDeltaAngleDeg = deltaAng,
                            liveHeightMeters = null
                        )
                    }
                    anchorA != null && cursorDist != null -> {
                        val liveB = GroundAnchorPoint(
                            radialDistanceMeters = cursorDist,
                            azimuthDeg = state.azimuthDeg,
                            pitchDeg = state.pitchDeg
                        )
                        val liveAB = DistanceCalculator.calculateTwoPointGroundDistance(anchorA, liveB)
                        val deltaAng = abs(DistanceCalculator.shortestAngleDeltaDeg(anchorA.azimuthDeg, state.azimuthDeg))
                        state.copy(
                            liveCursorGroundDistanceMeters = cursorDist,
                            liveDistanceMeters = liveAB,
                            twoPointDeltaAngleDeg = deltaAng,
                            liveHeightMeters = null
                        )
                    }
                    else -> {
                        state.copy(
                            liveCursorGroundDistanceMeters = cursorDist,
                            liveDistanceMeters = cursorDist,
                            twoPointDeltaAngleDeg = null,
                            liveHeightMeters = null
                        )
                    }
                }
            }

            MeasurementMode.OBJECT_REF -> {
                val dist = DistanceCalculator.calculatePinholeDistance(
                    realSizeMeters = state.activeReferenceMeters,
                    objectFractionOfViewHeight = state.caliperSpanFraction,
                    verticalFovDeg = state.intrinsics.verticalFovDeg,
                    calibrationFactor = state.focalCalibrationFactor
                )
                state.copy(liveDistanceMeters = dist, liveHeightMeters = null)
            }

            MeasurementMode.HEIGHT_MEASURE -> {
                val lockedBase = state.baseLockedDistanceMeters
                if (lockedBase == null) {
                    val dist = DistanceCalculator.calculateGroundDistance(
                        cameraHeightMeters = state.cameraHeightMeters,
                        pitchFromHorizontalDeg = state.pitchDeg
                    )
                    state.copy(liveDistanceMeters = dist, liveHeightMeters = null)
                } else {
                    val height = DistanceCalculator.calculateObjectHeight(
                        cameraHeightMeters = state.cameraHeightMeters,
                        horizontalDistanceMeters = lockedBase,
                        topPitchFromHorizontalDeg = state.pitchDeg
                    )
                    state.copy(liveDistanceMeters = lockedBase, liveHeightMeters = height)
                }
            }
        }
    }
}
