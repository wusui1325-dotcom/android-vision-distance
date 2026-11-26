package com.wusper.vision.distance.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ScreenRotationAlt
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wusper.vision.distance.engine.DistanceCalculator
import com.wusper.vision.distance.engine.OrientationSensorManager
import com.wusper.vision.distance.model.MeasurementMode
import com.wusper.vision.distance.model.RangefinderUiState
import com.wusper.vision.distance.model.ReferencePreset
import com.wusper.vision.distance.ui.components.CaliperOverlay
import com.wusper.vision.distance.ui.components.ReticleOverlay
import com.wusper.vision.distance.ui.components.StreamlinedCircleIconButton
import com.wusper.vision.distance.ui.components.StreamlinedPillButton
import com.wusper.vision.distance.ui.components.StreamlinedPrimaryActionButton
import com.wusper.vision.distance.ui.components.TelemetryPill
import com.wusper.vision.distance.viewmodel.RangefinderViewModel
import kotlinx.coroutines.delay

@Composable
fun RangefinderScreen(
    viewModel: RangefinderViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val sensorManager = remember(context) {
        OrientationSensorManager(context) { pitch, roll, azimuth ->
            viewModel.onSensorUpdate(pitch, roll, azimuth)
        }
    }
    LaunchedEffect(state.smoothingAlpha) {
        sensorManager.smoothingAlpha = state.smoothingAlpha
    }
    DisposableEffect(sensorManager) {
        sensorManager.start()
        onDispose { sensorManager.stop() }
    }

    LaunchedEffect(state.statusBanner) {
        if (state.statusBanner != null) {
            delay(2200)
            viewModel.clearBanner()
        }
    }

    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCustomSizeDialog by remember { mutableStateOf(false) }

    fun hapticClick() {
        if (state.hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    if (!hasCameraPermission) {
        PermissionRequestSurface(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    val twoPointStageLabel = if (state.mode == MeasurementMode.TWO_POINT) {
        when {
            state.startPointA == null -> "瞄準起始點 A"
            state.endPointB == null -> "瞄準結束點 B"
            else -> "A→B 已鎖定"
        }
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CameraPreviewSurface(
            torchEnabled = state.torchEnabled,
            modifier = Modifier.fillMaxSize()
        )

        if (state.mode == MeasurementMode.OBJECT_REF) {
            CaliperOverlay(
                topFraction = state.caliperTopFraction,
                bottomFraction = state.caliperBottomFraction,
                onUpdateCaliper = { top, bottom -> viewModel.updateCaliper(top, bottom) }
            )
        } else {
            ReticleOverlay(
                rollDeg = state.rollDeg,
                showGrid = state.showGrid,
                isLocked = state.isLocked || (state.mode == MeasurementMode.TWO_POINT && state.endPointB != null),
                pointStageLabel = twoPointStageLabel
            )
        }

        // Top HUD Toolbar + Readout Card
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TopToolbarRow(
                state = state,
                onToggleTorch = { hapticClick(); viewModel.toggleTorch() },
                onCycleUnit = { hapticClick(); viewModel.cycleUnit() },
                onToggleGrid = { hapticClick(); viewModel.toggleGrid() },
                onToggleTheme = { hapticClick(); viewModel.toggleDarkMonoTheme() },
                onOpenHistory = { hapticClick(); showHistory = true },
                onOpenSettings = { hapticClick(); showSettings = true }
            )

            DistanceReadoutCard(state = state)

            state.statusBanner?.let { msg ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TelemetryPill(
                        icon = Icons.Rounded.Info,
                        text = msg,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Bottom Streamlined Controls Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state.mode) {
                MeasurementMode.OBJECT_REF -> {
                    ReferencePresetStrip(
                        selected = state.selectedPreset,
                        customMeters = state.customObjectSizeMeters,
                        onSelectPreset = { preset ->
                            hapticClick()
                            if (preset == ReferencePreset.CUSTOM) {
                                showCustomSizeDialog = true
                            } else {
                                viewModel.selectPreset(preset)
                            }
                        }
                    )
                }

                MeasurementMode.TWO_POINT -> {
                    TwoPointControlStrip(
                        state = state,
                        onSetPointA = { hapticClick(); viewModel.setStartPointA() },
                        onSetPointB = { hapticClick(); viewModel.setEndPointB() },
                        onClearPoints = { hapticClick(); viewModel.clearTwoPoints() }
                    )
                }

                else -> {
                    CameraHeightQuickStrip(
                        heightMeters = state.cameraHeightMeters,
                        onAdjust = { delta -> hapticClick(); viewModel.adjustCameraHeight(delta) },
                        onQuickSet = { h -> hapticClick(); viewModel.setCameraHeight(h) }
                    )
                }
            }

            ModeSwitcherCapsule(
                currentMode = state.mode,
                onSelectMode = { mode -> hapticClick(); viewModel.selectMode(mode) }
            )

            BottomActionDock(
                state = state,
                onToggleLock = { hapticClick(); viewModel.toggleLock() },
                onPrimaryAction = {
                    hapticClick()
                    when (state.mode) {
                        MeasurementMode.TWO_POINT -> {
                            when {
                                state.startPointA == null -> viewModel.setStartPointA()
                                state.endPointB == null -> viewModel.setEndPointB()
                                else -> viewModel.saveCurrentMeasurement()
                            }
                        }
                        MeasurementMode.HEIGHT_MEASURE -> {
                            if (state.baseLockedDistanceMeters == null) {
                                viewModel.lockBaseForHeight()
                            } else {
                                viewModel.saveCurrentMeasurement()
                            }
                        }
                        else -> viewModel.saveCurrentMeasurement()
                    }
                },
                onReset = { hapticClick(); viewModel.resetMeasurement() }
            )
        }
    }

    if (showHistory) {
        HistorySheet(
            records = state.history,
            unit = state.unit,
            onDelete = { viewModel.deleteRecord(it) },
            onClearAll = { viewModel.clearHistory() },
            onDismiss = { showHistory = false }
        )
    }

    if (showSettings) {
        SettingsSheet(
            state = state,
            onCameraHeightChange = { viewModel.setCameraHeight(it) },
            onFocalFactorChange = { viewModel.setFocalCalibrationFactor(it) },
            onSmoothingChange = { viewModel.setSmoothingAlpha(it) },
            onCalibrateHorizon = { hapticClick(); viewModel.calibrateHorizonZero() },
            onResetHorizon = { hapticClick(); viewModel.resetPitchCalibration() },
            onSelectUnit = { hapticClick(); viewModel.setUnit(it) },
            onToggleTheme = { hapticClick(); viewModel.toggleDarkMonoTheme() },
            onToggleHaptics = { hapticClick(); viewModel.toggleHaptics() },
            onDismiss = { showSettings = false }
        )
    }

    if (showCustomSizeDialog) {
        CustomSizeDialog(
            currentMeters = state.customObjectSizeMeters,
            onConfirm = { meters ->
                viewModel.setCustomObjectSize(meters)
                showCustomSizeDialog = false
            },
            onDismiss = { showCustomSizeDialog = false }
        )
    }
}

@Composable
private fun CameraPreviewSurface(
    torchEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(cameraRef, torchEnabled) {
        cameraRef?.cameraControl?.enableTorch(torchEnabled)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        cameraProvider.unbindAll()
                        cameraRef = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview
                        )
                    } catch (_: Exception) {
                        // Graceful fallback if camera unavailable
                    }
                },
                ContextCompat.getMainExecutor(ctx)
            )
            previewView
        }
    )
}

@Composable
private fun TopToolbarRow(
    state: RangefinderUiState,
    onToggleTorch: () -> Unit,
    onCycleUnit: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StreamlinedCircleIconButton(
                icon = if (state.torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                contentDescription = "補光燈",
                active = state.torchEnabled,
                onClick = onToggleTorch
            )
            StreamlinedPillButton(
                icon = Icons.Rounded.Straighten,
                label = state.unit.symbol.uppercase(),
                height = 42.dp,
                onClick = onCycleUnit
            )
            StreamlinedCircleIconButton(
                icon = if (state.showGrid) Icons.Rounded.GridOn else Icons.Rounded.GridOff,
                contentDescription = "參考格線",
                active = state.showGrid,
                onClick = onToggleGrid
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StreamlinedCircleIconButton(
                icon = Icons.Rounded.Contrast,
                contentDescription = "黑白主題切換",
                onClick = onToggleTheme
            )
            StreamlinedCircleIconButton(
                icon = Icons.Rounded.History,
                contentDescription = "測距紀錄",
                onClick = onOpenHistory
            )
            StreamlinedCircleIconButton(
                icon = Icons.Rounded.Tune,
                contentDescription = "校準與設定",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun DistanceReadoutCard(state: RangefinderUiState) {
    val scheme = MaterialTheme.colorScheme
    val (distStr, distUnit) = DistanceCalculator.formatDistance(state.liveDistanceMeters, state.unit)
    val shape = RoundedCornerShape(26.dp)

    val headerHint = if (state.mode == MeasurementMode.TWO_POINT) {
        when {
            state.startPointA == null -> "兩點測距 · 瞄準起點 A 後點擊設定"
            state.endPointB == null -> "兩點測距 · 移動準星瞄準終點 B（即時 A→B）"
            else -> "兩點測距 · A → B 距離已鎖定"
        }
    } else {
        "${state.mode.label} · ${state.mode.shortHint}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.84f))
            .border(1.dp, scheme.outline, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val modeIcon = when (state.mode) {
                    MeasurementMode.GROUND_PITCH -> Icons.Rounded.Terrain
                    MeasurementMode.TWO_POINT -> Icons.Rounded.LinearScale
                    MeasurementMode.OBJECT_REF -> Icons.Rounded.AspectRatio
                    MeasurementMode.HEIGHT_MEASURE -> Icons.Rounded.Height
                }
                Icon(
                    imageVector = modeIcon,
                    contentDescription = state.mode.label,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = headerHint,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }

            if (state.isLocked || (state.mode == MeasurementMode.TWO_POINT && state.endPointB != null)) {
                TelemetryPill(
                    icon = Icons.Rounded.Lock,
                    text = "LOCK",
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = distStr,
                    style = MaterialTheme.typography.displayLarge,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = distUnit,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (state.mode == MeasurementMode.HEIGHT_MEASURE && state.baseLockedDistanceMeters != null) {
                val (hStr, hUnit) = DistanceCalculator.formatDistance(state.liveHeightMeters, state.unit)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Height,
                            contentDescription = "目標高度",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "目標高度",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$hStr $hUnit",
                        style = MaterialTheme.typography.headlineMedium,
                        color = scheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (state.mode == MeasurementMode.TWO_POINT) {
            val aText = state.startPointA?.let {
                val (v, u) = DistanceCalculator.formatDistance(it.radialDistanceMeters, state.unit)
                "A $v$u"
            } ?: "A 未設"
            val bDist = state.endPointB?.radialDistanceMeters ?: state.liveCursorGroundDistanceMeters
            val bText = if (state.endPointB != null) {
                val (v, u) = DistanceCalculator.formatDistance(bDist, state.unit)
                "B $v$u"
            } else if (state.startPointA != null && bDist != null) {
                val (v, u) = DistanceCalculator.formatDistance(bDist, state.unit)
                "B~ $v$u"
            } else {
                "B 未設"
            }
            val angText = state.twoPointDeltaAngleDeg?.let { String.format("Δ%.1f°", it) }
                ?: String.format("%+.1f°", state.pitchDeg)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryPill(icon = Icons.Rounded.TripOrigin, text = aText)
                TelemetryPill(icon = Icons.Rounded.Adjust, text = bText)
                TelemetryPill(icon = Icons.Rounded.Explore, text = angText)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryPill(
                    icon = Icons.Rounded.Explore,
                    text = String.format("%+.1f°", state.pitchDeg)
                )
                TelemetryPill(
                    icon = Icons.Rounded.PersonPin,
                    text = String.format("H %.2fm", state.cameraHeightMeters)
                )
                TelemetryPill(
                    icon = Icons.Rounded.ScreenRotationAlt,
                    text = String.format("R %+.1f°", state.rollDeg)
                )
            }
        }
    }
}

@Composable
private fun TwoPointControlStrip(
    state: RangefinderUiState,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearPoints: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamlinedPillButton(
            icon = Icons.Rounded.TripOrigin,
            label = if (state.startPointA == null) "設起點 A" else "重設 A",
            selected = state.startPointA != null,
            height = 38.dp,
            modifier = Modifier.weight(1f),
            onClick = onSetPointA
        )
        StreamlinedPillButton(
            icon = Icons.Rounded.Adjust,
            label = if (state.endPointB == null) "設終點 B" else "重設 B",
            selected = state.endPointB != null,
            enabled = state.startPointA != null,
            height = 38.dp,
            modifier = Modifier.weight(1f),
            onClick = onSetPointB
        )
        StreamlinedCircleIconButton(
            icon = Icons.Rounded.RestartAlt,
            contentDescription = "清除起終點",
            size = 38.dp,
            iconSize = 18.dp,
            onClick = onClearPoints
        )
    }
}

@Composable
private fun CameraHeightQuickStrip(
    heightMeters: Float,
    onAdjust: (Float) -> Unit,
    onQuickSet: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamlinedCircleIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "降低鏡頭高度",
            size = 36.dp,
            iconSize = 18.dp,
            onClick = { onAdjust(-0.05f) }
        )
        StreamlinedPillButton(
            icon = Icons.Rounded.PersonPin,
            label = String.format("機高 %.2fm", heightMeters),
            height = 36.dp,
            onClick = {}
        )
        StreamlinedCircleIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "提高鏡頭高度",
            size = 36.dp,
            iconSize = 18.dp,
            onClick = { onAdjust(0.05f) }
        )
        listOf(1.20f to "1.2m", 1.50f to "1.5m", 1.70f to "1.7m").forEach { (h, label) ->
            StreamlinedPillButton(
                icon = Icons.Rounded.Height,
                label = label,
                selected = kotlin.math.abs(heightMeters - h) < 0.03f,
                height = 36.dp,
                onClick = { onQuickSet(h) }
            )
        }
    }
}

@Composable
private fun ReferencePresetStrip(
    selected: ReferencePreset,
    customMeters: Float,
    onSelectPreset: (ReferencePreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReferencePreset.entries.forEach { preset ->
            val icon = when (preset) {
                ReferencePreset.PERSON -> Icons.Rounded.Person
                ReferencePreset.DOOR -> Icons.Rounded.DoorFront
                ReferencePreset.CAR -> Icons.Rounded.DirectionsCar
                ReferencePreset.A4_PAPER -> Icons.Rounded.Description
                ReferencePreset.CREDIT_CARD -> Icons.Rounded.CreditCard
                ReferencePreset.CUSTOM -> Icons.Rounded.Edit
            }
            val label = if (preset == ReferencePreset.CUSTOM) {
                String.format("自訂 %.2fm", customMeters)
            } else {
                preset.label
            }
            StreamlinedPillButton(
                icon = icon,
                label = label,
                selected = selected == preset,
                height = 36.dp,
                onClick = { onSelectPreset(preset) }
            )
        }
    }
}

@Composable
private fun ModeSwitcherCapsule(
    currentMode: MeasurementMode,
    onSelectMode: (MeasurementMode) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.86f))
            .border(1.dp, scheme.outline, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MeasurementMode.entries.forEach { mode ->
            val icon = when (mode) {
                MeasurementMode.GROUND_PITCH -> Icons.Rounded.Terrain
                MeasurementMode.TWO_POINT -> Icons.Rounded.LinearScale
                MeasurementMode.OBJECT_REF -> Icons.Rounded.AspectRatio
                MeasurementMode.HEIGHT_MEASURE -> Icons.Rounded.Height
            }
            StreamlinedPillButton(
                icon = icon,
                label = mode.shortLabel,
                selected = currentMode == mode,
                height = 38.dp,
                horizontalPadding = 8.dp,
                modifier = Modifier.weight(1f),
                onClick = { onSelectMode(mode) }
            )
        }
    }
}

@Composable
private fun BottomActionDock(
    state: RangefinderUiState,
    onToggleLock: () -> Unit,
    onPrimaryAction: () -> Unit,
    onReset: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val (primaryIcon, primaryDesc) = when (state.mode) {
        MeasurementMode.TWO_POINT -> when {
            state.startPointA == null -> Icons.Rounded.TripOrigin to "設定起始點 A"
            state.endPointB == null -> Icons.Rounded.Adjust to "設定結束點 B"
            else -> Icons.Rounded.CenterFocusStrong to "儲存兩點測距"
        }
        MeasurementMode.HEIGHT_MEASURE -> if (state.baseLockedDistanceMeters == null) {
            Icons.Rounded.MyLocation to "鎖定底部距離"
        } else {
            Icons.Rounded.CenterFocusStrong to "儲存測高"
        }
        else -> Icons.Rounded.CenterFocusStrong to "儲存測距"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(scheme.surface.copy(alpha = 0.88f))
            .border(1.dp, scheme.outline, RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamlinedPillButton(
            icon = if (state.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            label = if (state.isLocked) "解鎖" else "鎖定",
            selected = state.isLocked,
            height = 44.dp,
            onClick = onToggleLock
        )

        StreamlinedPrimaryActionButton(
            icon = primaryIcon,
            contentDescription = primaryDesc,
            accentWhite = state.darkMonoTheme,
            onClick = onPrimaryAction
        )

        StreamlinedPillButton(
            icon = Icons.Rounded.Refresh,
            label = "重置",
            height = 44.dp,
            onClick = onReset
        )
    }
}

@Composable
private fun PermissionRequestSurface(
    onRequestPermission: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceVariant)
                    .border(1.dp, scheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "相機權限",
                    tint = scheme.onSurface,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = "MonoRange 視覺測距",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onBackground
            )
            Text(
                text = "需要相機權限以啟用即時取景器與視覺測距引擎。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            StreamlinedPillButton(
                icon = Icons.Rounded.VerifiedUser,
                label = "授予相機權限",
                inverted = true,
                height = 46.dp,
                onClick = onRequestPermission
            )
        }
    }
}

@Composable
private fun CustomSizeDialog(
    currentMeters: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(String.format("%.2f", currentMeters)) }
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurface,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "自訂參考尺寸"
            )
        },
        title = {
            Text(
                text = "自訂參考物體高度 (公尺)",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                label = { Text("高度 (m)，例如 0.50") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Straighten,
                        contentDescription = null
                    )
                }
            )
        },
        confirmButton = {
            StreamlinedPillButton(
                icon = Icons.Rounded.Check,
                label = "套用",
                inverted = true,
                height = 38.dp,
                onClick = {
                    val parsed = textValue.toFloatOrNull()
                    if (parsed != null && parsed > 0.01f) {
                        onConfirm(parsed)
                    } else {
                        onDismiss()
                    }
                }
            )
        },
        dismissButton = {
            StreamlinedPillButton(
                icon = Icons.Rounded.Close,
                label = "取消",
                height = 38.dp,
                onClick = onDismiss
            )
        }
    )
}
