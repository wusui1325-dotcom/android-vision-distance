package com.monorange.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.monorange.app.ui.theme.MonoBlack
import com.monorange.app.ui.theme.MonoWhite
import kotlin.math.roundToInt

/**
 * Interactive streamlined monochrome caliper overlay for Pinhole (Object Reference) mode.
 * Users drag the top and bottom capsule handles to frame the target object.
 */
@Composable
fun CaliperOverlay(
    topFraction: Float,
    bottomFraction: Float,
    onUpdateCaliper: (top: Float, bottom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestTop by rememberUpdatedState(topFraction)
    val latestBottom by rememberUpdatedState(bottomFraction)
    val latestCallback by rememberUpdatedState(onUpdateCaliper)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val totalHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val topYPx = totalHeightPx * topFraction
        val bottomYPx = totalHeightPx * bottomFraction

        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftX = size.width * 0.14f
            val rightX = size.width * 0.86f
            val midX = size.width * 0.5f
            val strokePx = 1.8.dp.toPx()
            val haloPx = 3.6.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

            // Vertical span measurement rail
            drawLine(
                color = MonoBlack.copy(alpha = 0.5f),
                start = Offset(midX, topYPx),
                end = Offset(midX, bottomYPx),
                strokeWidth = haloPx
            )
            drawLine(
                color = MonoWhite.copy(alpha = 0.85f),
                start = Offset(midX, topYPx),
                end = Offset(midX, bottomYPx),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = dashEffect
            )

            // Top horizontal caliper bar
            drawLine(MonoBlack.copy(alpha = 0.6f), Offset(leftX, topYPx), Offset(rightX, topYPx), haloPx)
            drawLine(MonoWhite, Offset(leftX, topYPx), Offset(rightX, topYPx), strokePx)

            // Bottom horizontal caliper bar
            drawLine(MonoBlack.copy(alpha = 0.6f), Offset(leftX, bottomYPx), Offset(rightX, bottomYPx), haloPx)
            drawLine(MonoWhite, Offset(leftX, bottomYPx), Offset(rightX, bottomYPx), strokePx)

            // Architectural end ticks
            val tick = 10.dp.toPx()
            drawLine(MonoWhite, Offset(leftX, topYPx), Offset(leftX, topYPx + tick), strokePx)
            drawLine(MonoWhite, Offset(rightX, topYPx), Offset(rightX, topYPx + tick), strokePx)
            drawLine(MonoWhite, Offset(leftX, bottomYPx), Offset(leftX, bottomYPx - tick), strokePx)
            drawLine(MonoWhite, Offset(rightX, bottomYPx), Offset(rightX, bottomYPx - tick), strokePx)
        }

        // Top streamlined drag handle pill (with icon)
        CaliperHandlePill(
            label = "頂端",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (topYPx - with(density) { 18.dp.toPx() }).roundToInt()) }
                .pointerInput(totalHeightPx) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val nextTop = (latestTop + dragAmount / totalHeightPx)
                            .coerceIn(0.10f, latestBottom - 0.06f)
                        latestCallback(nextTop, latestBottom)
                    }
                }
        )

        // Bottom streamlined drag handle pill (with icon)
        CaliperHandlePill(
            label = "底端",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (bottomYPx - with(density) { 18.dp.toPx() }).roundToInt()) }
                .pointerInput(totalHeightPx) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val nextBottom = (latestBottom + dragAmount / totalHeightPx)
                            .coerceIn(latestTop + 0.06f, 0.88f)
                        latestCallback(latestTop, nextBottom)
                    }
                }
        )
    }
}

@Composable
private fun CaliperHandlePill(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MonoBlack.copy(alpha = 0.88f))
            .border(1.dp, MonoWhite, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = label,
                tint = MonoWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MonoWhite
            )
        }
    }
}
