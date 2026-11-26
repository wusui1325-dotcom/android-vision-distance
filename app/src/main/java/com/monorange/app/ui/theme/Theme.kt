package com.monorange.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkMonoScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoGraphite,
    onPrimaryContainer = MonoWhite,
    secondary = MonoSilver,
    onSecondary = MonoBlack,
    background = MonoBlack,
    onBackground = MonoWhite,
    surface = MonoObsidian,
    onSurface = MonoWhite,
    surfaceVariant = MonoCharcoal,
    onSurfaceVariant = MonoSilver,
    outline = MonoSteel,
    outlineVariant = MonoGraphite
)

private val LightMonoScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoMist,
    onPrimaryContainer = MonoBlack,
    secondary = MonoGraphite,
    onSecondary = MonoWhite,
    background = MonoWhite,
    onBackground = MonoBlack,
    surface = MonoPaper,
    onSurface = MonoBlack,
    surfaceVariant = MonoMist,
    onSurfaceVariant = MonoGraphite,
    outline = MonoSilver,
    outlineVariant = MonoMist
)

@Composable
fun MonoRangeTheme(
    darkMono: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkMono) DarkMonoScheme else LightMonoScheme,
        typography = MonoTypography,
        content = content
    )
}
