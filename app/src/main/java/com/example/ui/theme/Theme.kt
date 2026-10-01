package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TacticalPrimaryGreen,
    onPrimary = Color.Black,
    primaryContainer = TacticalPulseBg,
    onPrimaryContainer = TacticalPrimaryGreen,
    secondary = TacticalMutedText,
    onSecondary = TacticalSurface,
    tertiary = TacticalAmberWarning,
    onTertiary = Color.Black,
    background = TacticalBackground,
    onBackground = TacticalTextOnDark,
    surface = TacticalSurface,
    onSurface = TacticalTextOnDark,
    surfaceVariant = TacticalCard,
    onSurfaceVariant = TacticalMutedText,
    outline = TacticalBorder,
    error = TacticalAccentRed,
    onError = Color.White
)

// Standard light color scheme matching the brand theme if system is forced into light mode
private val LightColorScheme = lightColorScheme(
    primary = TacticalPrimaryGreen,
    onPrimary = Color.Black,
    primaryContainer = TacticalPulseBg,
    onPrimaryContainer = TacticalPrimaryGreen,
    secondary = TacticalMutedText,
    onSecondary = TacticalSurface,
    tertiary = TacticalAmberWarning,
    onTertiary = Color.Black,
    background = TacticalBackground,
    onBackground = TacticalTextOnDark,
    surface = TacticalSurface,
    onSurface = TacticalTextOnDark,
    surfaceVariant = TacticalCard,
    onSurfaceVariant = TacticalMutedText,
    outline = TacticalBorder,
    error = TacticalAccentRed,
    onError = Color.White
)

// Internal helper to avoid unresolved reference Color in LightColorScheme
private object ColorLookup {
    val LightSurface = androidx.compose.ui.graphics.Color(0xFFF0F5F2)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors by default to retain the iconic military/watch brand design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Force dark visual color scheme to keep military tactical dark appearance optimal
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
