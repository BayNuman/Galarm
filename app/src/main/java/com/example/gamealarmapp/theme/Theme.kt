package com.example.gamealarmapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HotPink,
    secondary = SoftRose,
    tertiary = NeonPinkGlow,
    background = MidnightPlum,
    surface = DeepPlumSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = HotPink,
    secondary = SoftRose,
    tertiary = NeonPinkGlow,
    background = Color(0xFFFFF0F5), // Lavender Blush
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF13061A),
    onBackground = Color(0xFF13061A),
    onSurface = Color(0xFF13061A)
)

@Composable
fun GameAlarmAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force our custom pink styling
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
