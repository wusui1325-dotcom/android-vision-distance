package com.monorange.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraEnhance
import androidx.compose.material.icons.rounded.CompassCalibration
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monorange.app.model.DistanceUnit
import com.monorange.app.model.RangefinderUiState
import com.monorange.app.ui.components.StreamlinedItemRow
import com.monorange.app.ui.components.StreamlinedPillButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    state: RangefinderUiState,
    onCameraHeightChange: (Float) -> Unit,
    onFocalFactorChange: (Float) -> Unit,
    onSmoothingChange: (Float) -> Unit,
    onCalibrateHorizon: () -> Unit,
    onResetHorizon: () -> Unit,
    onSelectUnit: (DistanceUnit) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleHaptics: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "校準與設定",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "校準與系統設定",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // 1. Camera Height Calibration
            StreamlinedItemRow(
                icon = Icons.Rounded.Height,
                title = "鏡頭離地高度",
                subtitle = "目前：${String.format("%.2f", state.cameraHeightMeters)} m（地面測距基準）"
            )
            Slider(
                value = state.cameraHeightMeters,
                onValueChange = onCameraHeightChange,
                valueRange = 0.50f..2.50f,
                colors = SliderDefaults.colors(
                    thumbColor = scheme.primary,
                    activeTrackColor = scheme.primary,
                    inactiveTrackColor = scheme.outline
                )
            )

            // 2. Focal / FOV Calibration Factor
            StreamlinedItemRow(
                icon = Icons.Rounded.CameraEnhance,
                title = "鏡頭焦距微調係數",
                subtitle = "係數 ${String.format("%.2f", state.focalCalibrationFactor)}x · 偵測焦距 ${
                    String.format("%.1f", state.intrinsics.focalLengthMm)
                }mm / VFOV ${String.format("%.1f", state.intrinsics.verticalFovDeg)}°"
            )
            Slider(
                value = state.focalCalibrationFactor,
                onValueChange = onFocalFactorChange,
                valueRange = 0.70f..1.35f,
                colors = SliderDefaults.colors(
                    thumbColor = scheme.primary,
                    activeTrackColor = scheme.primary,
                    inactiveTrackColor = scheme.outline
                )
            )

            // 3. Horizon Pitch Zero Calibration
            StreamlinedItemRow(
                icon = Icons.Rounded.CompassCalibration,
                title = "水平零點校準",
                subtitle = "偏移量：${String.format("%+.1f°", state.pitchOffsetDeg)}",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StreamlinedPillButton(
                            icon = Icons.Rounded.CompassCalibration,
                            label = "校準",
                            inverted = true,
                            height = 34.dp,
                            onClick = onCalibrateHorizon
                        )
                        StreamlinedPillButton(
                            icon = Icons.Rounded.RestartAlt,
                            label = "重置",
                            height = 34.dp,
                            onClick = onResetHorizon
                        )
                    }
                }
            )

            // 4. Sensor Smoothing
            StreamlinedItemRow(
                icon = Icons.Rounded.GraphicEq,
                title = "感測器平滑濾波",
                subtitle = "靈敏度：${String.format("%.0f%%", state.smoothingAlpha * 100)}（數值越低越穩定）"
            )
            Slider(
                value = state.smoothingAlpha,
                onValueChange = onSmoothingChange,
                valueRange = 0.06f..0.45f,
                colors = SliderDefaults.colors(
                    thumbColor = scheme.primary,
                    activeTrackColor = scheme.primary,
                    inactiveTrackColor = scheme.outline
                )
            )

            // 5. Distance Unit Picker (all with icon)
            StreamlinedItemRow(
                icon = Icons.Rounded.Straighten,
                title = "距離顯示單位",
                subtitle = state.unit.label
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistanceUnit.entries.forEach { u ->
                    StreamlinedPillButton(
                        icon = Icons.Rounded.Straighten,
                        label = u.symbol,
                        selected = state.unit == u,
                        onClick = { onSelectUnit(u) },
                        modifier = Modifier.weight(1f),
                        height = 38.dp
                    )
                }
            }

            // 6. Monochrome Theme Switch
            StreamlinedItemRow(
                icon = if (state.darkMonoTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                title = "純黑 / 純白介面主題",
                subtitle = if (state.darkMonoTheme) "目前：曜石黑平面模式" else "目前：極簡白平面模式",
                trailing = {
                    Switch(
                        checked = state.darkMonoTheme,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onPrimary,
                            checkedTrackColor = scheme.primary
                        )
                    )
                },
                onClick = onToggleTheme
            )

            // 7. Haptics Switch
            StreamlinedItemRow(
                icon = Icons.Rounded.Vibration,
                title = "觸覺震動回饋",
                subtitle = if (state.hapticEnabled) "鎖定與儲存時提供微震動" else "已關閉",
                trailing = {
                    Switch(
                        checked = state.hapticEnabled,
                        onCheckedChange = { onToggleHaptics() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onPrimary,
                            checkedTrackColor = scheme.primary
                        )
                    )
                },
                onClick = onToggleHaptics
            )
        }
    }
}
