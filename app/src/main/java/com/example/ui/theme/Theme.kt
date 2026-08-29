package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SleekPrimaryDark,
    onPrimary = SleekOnPrimaryDark,
    primaryContainer = SleekPrimaryContainerDark,
    onPrimaryContainer = SleekOnPrimaryContainerDark,
    secondary = SleekSecondaryDark,
    onSecondary = SleekOnPrimaryDark,
    secondaryContainer = SleekSecondaryContainerDark,
    onSecondaryContainer = SleekOnSecondaryContainerDark,
    tertiary = SleekTertiary,
    background = SleekBackgroundDark,
    surface = SleekSurfaceDark,
    surfaceVariant = SleekSurfaceVariantDark,
    outline = SleekOutlineDark,
    onBackground = SleekOnSurfaceDark,
    onSurface = SleekOnSurfaceDark,
    onSurfaceVariant = SleekOnSurfaceVariantDark,
    error = ErrorRed,
    errorContainer = ErrorRedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    onSecondary = SleekOnSecondary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    tertiary = SleekTertiary,
    background = SleekBackgroundLight,
    surface = SleekSurfaceLight,
    surfaceVariant = SleekSurfaceVariantLight,
    outline = SleekOutlineLight,
    onBackground = SleekOnSurfaceLight,
    onSurface = SleekOnSurfaceLight,
    onSurfaceVariant = SleekOnSurfaceVariantLight,
    error = ErrorRed,
    errorContainer = ErrorRedContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

