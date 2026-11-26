package com.wusper.vision.distance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.wusper.vision.distance.ui.theme.MonoBlack
import com.wusper.vision.distance.ui.theme.MonoWhite
import kotlin.math.abs

/**
 * Precision monochrome crosshair, horizon level wings, optional grid,
 * and optional active point badge (e.g. "A", "B", or "A→B") for two-point mode.
 */
@Composable
fun ReticleOverlay(
    rollDeg: Float,
    showGrid: Boolean,
    isLocked: Boolean,
    pointStageLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val isLevel = abs(rollDeg) < 1.8f

    Box(modifier = modifier.fillMaxSize()) {
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

            val haloColor = MonoBlack.copy(alpha = 0.55f)
            val lineColor = if (isLocked) MonoWhite else MonoWhite.copy(alpha = 0.94f)
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
