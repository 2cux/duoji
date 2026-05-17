package com.duoji.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WarmColorScheme = lightColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnPrimary,
    primaryContainer = WarmSecondary,
    secondary = WarmAccent,
    onSecondary = WarmOnPrimary,
    secondaryContainer = WarmSecondary,
    tertiary = WarmIncome,
    background = WarmBackground,
    onBackground = WarmTextPrimary,
    surface = WarmCard,
    onSurface = WarmTextPrimary,
    surfaceVariant = WarmCardAlt,
    onSurfaceVariant = WarmTextSecondary,
    outline = WarmSecondary,
    error = WarmAccent,
    onError = WarmOnPrimary
)

@Composable
fun DuoJiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmColorScheme,
        typography = DuoJiTypography,
        content = content
    )
}
