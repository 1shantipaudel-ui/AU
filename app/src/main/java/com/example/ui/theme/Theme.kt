package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanMinimalismColorScheme = lightColorScheme(
    primary = CleanPrimary,
    onPrimary = Color.White,
    primaryContainer = CleanPrimaryContainer,
    onPrimaryContainer = CleanOnPrimaryContainer,
    secondary = CleanSecondary,
    onSecondary = Color.White,
    secondaryContainer = CleanSecondaryContainer,
    onSecondaryContainer = CleanOnSecondaryContainer,
    tertiary = CleanTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CleanTertiaryContainer,
    background = CleanBg,
    onBackground = TextPrimary,
    surface = CleanSurface,
    onSurface = TextPrimary,
    surfaceVariant = CleanSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CleanOutline,
    outlineVariant = CleanCardBorder,
    error = CleanRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanMinimalismColorScheme,
        typography = Typography,
        content = content
    )
}

