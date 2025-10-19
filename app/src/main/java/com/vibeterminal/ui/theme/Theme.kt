package com.vibeterminal.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark theme colors (terminal-like)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF002C5F),
    primaryContainer = Color(0xFF004686),
    onPrimaryContainer = Color(0xFFD6E3FF),

    secondary = Color(0xFFB8C5D6),
    onSecondary = Color(0xFF23303F),
    secondaryContainer = Color(0xFF394756),
    onSecondaryContainer = Color(0xFFD4E1F3),

    tertiary = Color(0xFFD4BEE6),
    onTertiary = Color(0xFF3A2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF1DAFF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF000000),  // Pure black background
    onBackground = Color(0xFFE3E2E6),

    surface = Color(0xFF000000),     // Pure black surface
    onSurface = Color(0xFFC9D1D9),

    surfaceVariant = Color(0xFF0A0A0A),
    onSurfaceVariant = Color(0xFF8B949E),

    outline = Color(0xFF1A1A1A)
)

// Light theme colors (for readability in bright environments)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0052CC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3F),

    secondary = Color(0xFF555F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF111C2B),

    tertiary = Color(0xFF6F5677),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7D9FF),
    onTertiaryContainer = Color(0xFF281130),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1C1E),

    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),

    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474E),

    outline = Color(0xFF74777F)
)

@Composable
fun VibeTerminalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled for consistent terminal look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
