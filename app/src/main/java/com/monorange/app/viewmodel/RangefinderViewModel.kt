package com.monorange.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.monorange.app.data.MeasurementStore
import com.monorange.app.engine.CameraIntrinsicsHelper
import com.monorange.app.engine.DistanceCalculator
import com.monorange.app.model.DistanceUnit
import com.monorange.app.model.MeasurementMode
import com.monorange.app.model.MeasurementRecord
import com.monorange.app.model.RangefinderUiState
import com.monorange.app.model.ReferencePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    fun onSensorUpdate(rawPitchDeg: Float, rawRollDeg: Float) {
        _uiState.update { state ->
            if (state.isLocked) return@update state
            val adjustedPitch = rawPitchDeg - state.pitchOffsetDeg
            val updated = state.copy(
                pitchDeg = adjustedPitch,
                rollDeg = rawRollDeg
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
                    MeasurementMode.GROUND_PITCH -> "鏡頭高度 ${String.format("%.2f", state.cameraHeightMeters)}m"
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
