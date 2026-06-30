package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkAppPrimary,
    secondary = DarkAppSecondary,
    background = DarkAppBackground,
    surface = DarkAppBackground,
    onPrimary = DarkAppBackground,
    onSecondary = DarkAppBackground,
    onBackground = DarkAppPrimary,
    onSurface = DarkAppPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    background = AppBackground,
    surface = AppSurfaceSolid,
    onPrimary = AppSurfaceSolid,
    onSecondary = AppPrimary,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
