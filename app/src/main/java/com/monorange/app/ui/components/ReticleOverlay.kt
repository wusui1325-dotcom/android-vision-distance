package com.monorange.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.monorange.app.ui.theme.MonoBlack
import com.monorange.app.ui.theme.MonoWhite
import kotlin.math.abs

/**
 * Precision monochrome crosshair, roll horizon level indicator, and optional rule-of-thirds grid.
 */
@Composable
fun ReticleOverlay(
    rollDeg: Float,
    showGrid: Boolean,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val isLevel = abs(rollDeg) < 1.8f

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Optional minimalist architectural grid
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

        // Subtle dark contrast halo so white reticle stays legible on bright surfaces
        val haloColor = MonoBlack.copy(alpha = 0.55f)
        val lineColor = if (isLocked) MonoWhite else MonoWhite.copy(alpha = 0.94f)
        val outerRadius = 34.dp.toPx()
        val innerGap = 10.dp.toPx()
        val tickLen = 22.dp.toPx()
        val mainStroke = 1.6.dp.toPx()
        val haloStroke = 3.6.dp.toPx()

        // Outer streamlined ring
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

        // Four precision crosshair segments
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

        // Center target dot
        drawCircle(color = haloColor, radius = 4.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = lineColor, radius = 2.4.dp.toPx(), center = Offset(cx, cy))

        // Live horizon roll indicator wings
        val wingHalf = 74.dp.toPx()
        val wingInner = 46.dp.toPx()
        val levelStroke = if (isLevel) 2.2.dp.toPx() else 1.2.dp.toPx()
        val levelColor = if (isLevel) MonoWhite else MonoWhite.copy(alpha = 0.58f)

        rotate(degrees = -rollDeg.coerceIn(-45f, 45f), pivot = Offset(cx, cy)) {
            drawLine(
                color = haloColor,
                start = Offset(cx - wingHalf, cy),
                end = Offset(cx - wingInner, cy),
                strokeWidth = levelStroke + 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = haloColor,
                start = Offset(cx + wingInner, cy),
                end = Offset(cx + wingHalf, cy),
                strokeWidth = levelStroke + 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = levelColor,
                start = Offset(cx - wingHalf, cy),
                end = Offset(cx - wingInner, cy),
                strokeWidth = levelStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = levelColor,
                start = Offset(cx + wingInner, cy),
                end = Offset(cx + wingHalf, cy),
                strokeWidth = levelStroke,
                cap = StrokeCap.Round
            )
        }

        // Corner minimalist viewfinder L-brackets
        val bracketMargin = 24.dp.toPx()
        val bracketLen = 18.dp.toPx()
        val bColor = Color.White.copy(alpha = 0.32f)
        val bStroke = 1.2.dp.toPx()

        // Top-left
        drawLine(bColor, Offset(bracketMargin, bracketMargin), Offset(bracketMargin + bracketLen, bracketMargin), bStroke)
        drawLine(bColor, Offset(bracketMargin, bracketMargin), Offset(bracketMargin, bracketMargin + bracketLen), bStroke)
        // Top-right
        drawLine(bColor, Offset(size.width - bracketMargin, bracketMargin), Offset(size.width - bracketMargin - bracketLen, bracketMargin), bStroke)
        drawLine(bColor, Offset(size.width - bracketMargin, bracketMargin), Offset(size.width - bracketMargin, bracketMargin + bracketLen), bStroke)
    }
}
