package com.ois.stickymemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8C42),
    onPrimary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF8C42),
    onPrimary = Color.White,
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF3E2723),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFFFF3C4),
    onSurfaceVariant = Color(0xFF5D4037)
)

@Composable
fun StickyMemoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}