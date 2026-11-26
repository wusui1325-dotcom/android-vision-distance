package com.wusper.vision.distance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wusper.vision.distance.engine.DistanceCalculator
import com.wusper.vision.distance.engine.ProjectedPoint2D
import com.wusper.vision.distance.model.CameraIntrinsics
import com.wusper.vision.distance.model.RotationMatrix3x3
import com.wusper.vision.distance.model.Vec3
import com.wusper.vision.distance.ui.theme.MonoBlack
import com.wusper.vision.distance.ui.theme.MonoWhite
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Precision monochrome crosshair, horizon level wings, optional grid,
 * and 3D world-locked dashed line + A/B anchor markers (with FILL_CENTER FOV compensation,
 * 3D near-plane segment clipping, and off-screen edge direction indicators).
 */
@Composable
fun ReticleOverlay(
    rollDeg: Float,
    showGrid: Boolean,
    isLocked: Boolean,
    pointStageLabel: String? = null,
    worldPointA: Vec3? = null,
    worldPointB: Vec3? = null,
    liveCursorWorldPos: Vec3? = null,
    rotationMatrix: RotationMatrix3x3 = RotationMatrix3x3(),
    intrinsics: CameraIntrinsics = CameraIntrinsics(),
    focalCalibrationFactor: Float = 1.0f,
    pitchOffsetDeg: Float = 0f,
    twoPointDistanceLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val isLevel = abs(rollDeg) < 1.8f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val projA: ProjectedPoint2D? = worldPointA?.let {
            DistanceCalculator.projectWorldPointToScreen(
                worldPos = it,
                rotationMatrix = rotationMatrix,
                intrinsics = intrinsics,
                viewWidthPx = widthPx,
                viewHeightPx = heightPx,
                focalCalibrationFactor = focalCalibrationFactor,
                pitchOffsetDeg = pitchOffsetDeg
            )
        }
        val projB: ProjectedPoint2D? = worldPointB?.let {
            DistanceCalculator.projectWorldPointToScreen(
                worldPos = it,
                rotationMatrix = rotationMatrix,
                intrinsics = intrinsics,
                viewWidthPx = widthPx,
                viewHeightPx = heightPx,
                focalCalibrationFactor = focalCalibrationFactor,
                pitchOffsetDeg = pitchOffsetDeg
            )
        }

        val segmentEndWorld: Vec3? = worldPointB ?: liveCursorWorldPos
        val clippedLine = if (worldPointA != null && segmentEndWorld != null) {
            DistanceCalculator.projectWorldSegmentClipped(
                worldA = worldPointA,
                worldB = segmentEndWorld,
                rotationMatrix = rotationMatrix,
                intrinsics = intrinsics,
                viewWidthPx = widthPx,
                viewHeightPx = heightPx,
                focalCalibrationFactor = focalCalibrationFactor,
                pitchOffsetDeg = pitchOffsetDeg
            )
        } else null

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            if (showGrid) {
                val gridColor = MonoWhite.copy(alpha = 0.16f)
                val strokePx = 0.75.dp.toPx()
                for (i in 1..2) {
                    val x = size.width * i / 3f
                    val y = size.height * i / 3f
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokePx)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokePx)
                }
            }

            val haloColor = MonoBlack.copy(alpha = 0.58f)
            val lineColor = if (isLocked) MonoWhite else MonoWhite.copy(alpha = 0.94f)

            // 3D near-plane-clipped monochrome dashed line
            if (clippedLine != null) {
                val start = Offset(clippedLine.startX, clippedLine.startY)
                val end = Offset(clippedLine.endX, clippedLine.endY)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                drawLine(
                    color = haloColor,
                    start = start,
                    end = end,
                    strokeWidth = 4.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = MonoWhite,
                    start = start,
                    end = end,
                    strokeWidth = 2.0.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect
                )
            }

            // Point A anchor ring + solid dot (only when in front of camera)
            if (projA != null && projA.isInFront) {
                val ptA = Offset(projA.normX * size.width, projA.normY * size.height)
                drawCircle(color = haloColor, radius = 11.dp.toPx(), center = ptA, style = Stroke(width = 3.5.dp.toPx()))
                drawCircle(color = MonoWhite, radius = 11.dp.toPx(), center = ptA, style = Stroke(width = 1.8.dp.toPx()))
                drawCircle(color = MonoWhite, radius = 4.5.dp.toPx(), center = ptA)
            }

            // Point B anchor ring + solid dot (only when in front of camera)
            if (projB != null && projB.isInFront) {
                val ptB = Offset(projB.normX * size.width, projB.normY * size.height)
                drawCircle(color = haloColor, radius = 11.dp.toPx(), center = ptB, style = Stroke(width = 3.5.dp.toPx()))
                drawCircle(color = MonoWhite, radius = 11.dp.toPx(), center = ptB, style = Stroke(width = 1.8.dp.toPx()))
                drawCircle(color = MonoWhite, radius = 4.5.dp.toPx(), center = ptB)
            }

            val outerRadius = 34.dp.toPx()
            val innerGap = 10.dp.toPx()
            val tickLen = 22.dp.toPx()
            val mainStroke = 1.6.dp.toPx()
            val haloStroke = 3.6.dp.toPx()

            drawCircle(
                color = haloColor,
                radius = outerRadius,
                center = Offset(cx, cy),
                style = Stroke(width = haloStroke)
            )
            drawCircle(
                color = lineColor,
                radius = outerRadius,
                center = Offset(cx, cy),
                style = Stroke(width = mainStroke)
            )

            val segments = listOf(
                Offset(cx - outerRadius - tickLen, cy) to Offset(cx - innerGap, cy),
                Offset(cx + innerGap, cy) to Offset(cx + outerRadius + tickLen, cy),
                Offset(cx, cy - outerRadius - tickLen) to Offset(cx, cy - innerGap),
                Offset(cx, cy + innerGap) to Offset(cx, cy + outerRadius + tickLen)
            )
            segments.forEach { (start, end) ->
                drawLine(haloColor, start, end, strokeWidth = haloStroke, cap = StrokeCap.Round)
                drawLine(lineColor, start, end, strokeWidth = mainStroke, cap = StrokeCap.Round)
            }

            drawCircle(color = haloColor, radius = 4.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = lineColor, radius = 2.4.dp.toPx(), center = Offset(cx, cy))

            val wingHalf = 74.dp.toPx()
            val wingInner = 46.dp.toPx()
            val levelStroke = if (isLevel) 2.2.dp.toPx() else 1.2.dp.toPx()
            val levelColor = if (isLevel) MonoWhite else MonoWhite.copy(alpha = 0.58f)

            rotate(degrees = -rollDeg.coerceIn(-45f, 45f), pivot = Offset(cx, cy)) {
                drawLine(haloColor, Offset(cx - wingHalf, cy), Offset(cx - wingInner, cy), levelStroke + 2.dp.toPx(), StrokeCap.Round)
                drawLine(haloColor, Offset(cx + wingInner, cy), Offset(cx + wingHalf, cy), levelStroke + 2.dp.toPx(), StrokeCap.Round)
                drawLine(levelColor, Offset(cx - wingHalf, cy), Offset(cx - wingInner, cy), levelStroke, StrokeCap.Round)
                drawLine(levelColor, Offset(cx + wingInner, cy), Offset(cx + wingHalf, cy), levelStroke, StrokeCap.Round)
            }

            val bracketMargin = 24.dp.toPx()
            val bracketLen = 18.dp.toPx()
            val bColor = Color.White.copy(alpha = 0.32f)
            val bStroke = 1.2.dp.toPx()

            drawLine(bColor, Offset(bracketMargin, bracketMargin), Offset(bracketMargin + bracketLen, bracketMargin), bStroke)
            drawLine(bColor, Offset(bracketMargin, bracketMargin), Offset(bracketMargin, bracketMargin + bracketLen), bStroke)
            drawLine(bColor, Offset(size.width - bracketMargin, bracketMargin), Offset(size.width - bracketMargin - bracketLen, bracketMargin), bStroke)
            drawLine(bColor, Offset(size.width - bracketMargin, bracketMargin), Offset(size.width - bracketMargin, bracketMargin + bracketLen), bStroke)
        }

        // Point A badge (on-screen) or edge direction indicator (off-screen)
        if (projA != null) {
            AnchorOrEdgeIndicator(
                label = "A",
                projected = projA,
                widthPx = widthPx,
                heightPx = heightPx
            )
        }

        // Point B badge (on-screen) or edge direction indicator (off-screen)
        if (projB != null) {
            AnchorOrEdgeIndicator(
                label = "B",
                projected = projB,
                widthPx = widthPx,
                heightPx = heightPx
            )
        }

        // Midpoint distance pill on the visible portion of the dashed line
        if (clippedLine != null && !twoPointDistanceLabel.isNullOrBlank()) {
            val midX = ((clippedLine.startX + clippedLine.endX) / 2f).coerceIn(widthPx * 0.18f, widthPx * 0.82f)
            val midY = ((clippedLine.startY + clippedLine.endY) / 2f).coerceIn(heightPx * 0.30f, heightPx * 0.66f)
            Row(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (midX - with(density) { 44.dp.toPx() }).roundToInt(),
                            (midY - with(density) { 14.dp.toPx() }).roundToInt()
                        )
                    }
                    .clip(CircleShape)
                    .background(MonoBlack.copy(alpha = 0.88f))
                    .border(1.dp, MonoWhite, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LinearScale,
                    contentDescription = null,
                    tint = MonoWhite,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = twoPointDistanceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MonoWhite
                )
            }
        }

        if (!pointStageLabel.isNullOrBlank()) {
            val badgeIcon = when {
                pointStageLabel.contains("A→B") -> Icons.Rounded.CheckCircle
                pointStageLabel.contains("B") -> Icons.Rounded.Adjust
                else -> Icons.Rounded.TripOrigin
            }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 58.dp)
                    .clip(CircleShape)
                    .background(MonoBlack.copy(alpha = 0.86f))
                    .border(1.dp, MonoWhite, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = pointStageLabel,
                    tint = MonoWhite,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = pointStageLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MonoWhite
                )
            }
        }
    }
}

@Composable
private fun AnchorOrEdgeIndicator(
    label: String,
    projected: ProjectedPoint2D,
    widthPx: Float,
    heightPx: Float
) {
    val density = LocalDensity.current
    if (projected.isOnScreen) {
        val x = projected.normX * widthPx
        val y = projected.normY * heightPx
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (x - with(density) { 13.dp.toPx() }).roundToInt(),
                        (y - with(density) { 36.dp.toPx() }).roundToInt()
                    )
                }
                .size(26.dp)
                .clip(CircleShape)
                .background(MonoWhite)
                .border(1.5.dp, MonoBlack, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MonoBlack
            )
        }
    } else {
        // Off-screen: clamp badge cleanly inside viewport margins and rotate arrow toward anchor
        val dx = projected.normX - 0.5f
        val dy = projected.normY - 0.5f
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
        val clampedX = (projected.normX * widthPx).coerceIn(widthPx * 0.08f, widthPx * 0.84f)
        val clampedY = (projected.normY * heightPx).coerceIn(heightPx * 0.28f, heightPx * 0.68f)

        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (clampedX - with(density) { 20.dp.toPx() }).roundToInt(),
                        (clampedY - with(density) { 14.dp.toPx() }).roundToInt()
                    )
                }
                .clip(CircleShape)
                .background(MonoBlack.copy(alpha = 0.88f))
                .border(1.dp, MonoWhite, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = "錨點 $label 方向",
                tint = MonoWhite,
                modifier = Modifier
                    .size(13.dp)
                    .rotate(angleDeg)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MonoWhite
            )
        }
    }
}
