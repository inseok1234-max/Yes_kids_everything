package com.ois.stickymemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD60A),
    onPrimary = Color(0xFF1C1C1E),
    primaryContainer = Color(0xFF3A3100),
    onPrimaryContainer = Color(0xFFFFE680),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color(0xFF0F0F10),
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color(0xFFF2F2F7),
    background = Color(0xFF0F0F10),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF38383A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFCC00),
    onPrimary = Color(0xFF1C1C1E),
    primaryContainer = Color(0xFFFFF2B3),
    onPrimaryContainer = Color(0xFF1C1C1E),
    secondary = Color(0xFF6E6E73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8F8FA),
    onSecondaryContainer = Color(0xFF1C1C1E),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF8F8FA),
    onSurfaceVariant = Color(0xFF6E6E73),
    outline = Color(0xFFE5E5EA),
    outlineVariant = Color(0xFFE5E5EA)
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
