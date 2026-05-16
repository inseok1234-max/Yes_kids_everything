package com.ois.stickymemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE9B44C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4B3A16),
    onPrimaryContainer = Color(0xFFFFE4A3),
    secondary = Color(0xFF5FA8A5),
    secondaryContainer = Color(0xFF173F3D),
    background = Color(0xFF171612),
    onBackground = Color(0xFFF1EDE1),
    surface = Color(0xFF211F1A),
    onSurface = Color(0xFFF1EDE1),
    surfaceVariant = Color(0xFF343028),
    onSurfaceVariant = Color(0xFFCFC6B7)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD38428),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2AD),
    onPrimaryContainer = Color(0xFF3E2C08),
    secondary = Color(0xFF2D7F7B),
    secondaryContainer = Color(0xFFD7F1EE),
    onSecondaryContainer = Color(0xFF083331),
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF242118),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF242118),
    surfaceVariant = Color(0xFFF2E8D6),
    onSurfaceVariant = Color(0xFF625B4C)
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
