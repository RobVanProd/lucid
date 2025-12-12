package com.lucid.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// LUCID Mode Colors - E-ink aesthetic, monochrome serenity
private val LucidBackground = Color(0xFFFAFAFA)
private val LucidSurface = Color(0xFFF5F5F5)
private val LucidTextPrimary = Color(0xFF1A1A1A)
private val LucidTextSecondary = Color(0xFF666666)
private val LucidAccent = Color(0xFF2D2D2D)
private val LucidDivider = Color(0xFFE0E0E0)

// EXPLORE Mode Colors - Raw internet
private val ExploreBackground = Color(0xFF121212)
private val ExploreSurface = Color(0xFF1E1E1E)
private val ExploreTextPrimary = Color(0xFFFFFFFF)
private val ExploreTextSecondary = Color(0xFFB3B3B3)
private val ExploreAccent = Color(0xFFBB86FC)

// Status Colors
val UrgentColor = Color(0xFFD32F2F)
val ImportantColor = Color(0xFFF57C00)
val LowPriorityColor = Color(0xFF757575)

private val LucidColorScheme = lightColorScheme(
    primary = LucidAccent,
    onPrimary = LucidBackground,
    secondary = LucidTextSecondary,
    onSecondary = LucidBackground,
    tertiary = LucidDivider,
    background = LucidBackground,
    onBackground = LucidTextPrimary,
    surface = LucidSurface,
    onSurface = LucidTextPrimary,
    surfaceVariant = LucidSurface,
    onSurfaceVariant = LucidTextSecondary,
    outline = LucidDivider
)

private val ExploreColorScheme = darkColorScheme(
    primary = ExploreAccent,
    onPrimary = ExploreBackground,
    secondary = ExploreTextSecondary,
    onSecondary = ExploreBackground,
    background = ExploreBackground,
    onBackground = ExploreTextPrimary,
    surface = ExploreSurface,
    onSurface = ExploreTextPrimary,
    surfaceVariant = ExploreSurface,
    onSurfaceVariant = ExploreTextSecondary
)

@Composable
fun LucidTheme(
    isLucidMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isLucidMode) LucidColorScheme else ExploreColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LucidTypography,
        content = content
    )
}
