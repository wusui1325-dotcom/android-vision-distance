package com.wusper.vision.distance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wusper.vision.distance.ui.theme.MonoBlack
import com.wusper.vision.distance.ui.theme.MonoWhite

@Composable
fun StreamlinedPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    inverted: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 40.dp,
    horizontalPadding: Dp = 12.dp
) {
    val scheme = MaterialTheme.colorScheme
    val isFilled = selected || inverted
    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.surfaceVariant.copy(alpha = 0.45f)
            isFilled -> scheme.primary
            else -> scheme.surface.copy(alpha = 0.88f)
        },
        label = "pillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.onSurface.copy(alpha = 0.35f)
            isFilled -> scheme.onPrimary
            else -> scheme.onSurface
        },
        label = "pillFg"
    )
    val borderColor = if (isFilled) scheme.primary else scheme.outline

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = CircleShape,
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isFilled) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StreamlinedCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp
) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (active) scheme.primary else scheme.surface.copy(alpha = 0.84f),
        label = "circleBg"
    )
    val fg by animateColorAsState(
        targetValue = if (active) scheme.onPrimary else scheme.onSurface,
        label = "circleFg"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (active) scheme.primary else scheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun StreamlinedPrimaryActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentWhite: Boolean = true
) {
    val outerRing = if (accentWhite) MonoWhite else MonoBlack
    val innerFill = if (accentWhite) MonoWhite else MonoBlack
    val iconTint = if (accentWhite) MonoBlack else MonoWhite

    Box(
        modifier = modifier
            .size(66.dp)
            .clip(CircleShape)
            .border(2.dp, outerRing, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(innerFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun StreamlinedItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceVariant)
            .border(1.dp, scheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(scheme.surface)
                .border(1.dp, scheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = scheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun TelemetryPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
