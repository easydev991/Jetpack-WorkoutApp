package com.swparks.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = light_accent,
    onPrimary = light_text_filled_button,
    primaryContainer = light_card_background,
    onPrimaryContainer = light_main_text,
    secondary = light_tinted_button,
    onSecondary = light_accent,
    secondaryContainer = light_card_background,
    onSecondaryContainer = light_main_text,
    tertiary = light_accent,
    onTertiary = light_text_filled_button,
    tertiaryContainer = light_card_background,
    onTertiaryContainer = light_main_text,
    error = light_error,
    errorContainer = light_background,
    onError = light_text_filled_button,
    onErrorContainer = light_main_text,
    background = light_background,
    onBackground = light_onBackground,
    surface = light_background,
    onSurface = light_main_text,
    surfaceVariant = light_background,
    onSurfaceVariant = light_small_elements,
    surfaceTint = light_card_background,
    outline = light_separators,
    inverseOnSurface = light_background,
    inverseSurface = light_main_text,
    inversePrimary = light_background,
    outlineVariant = light_separators,
    scrim = light_main_text
)
private val DarkColors = darkColorScheme(
    primary = dark_accent,
    onPrimary = dark_text_filled_button,
    primaryContainer = dark_card_background,
    onPrimaryContainer = dark_main_text,
    secondary = dark_tinted_button,
    onSecondary = dark_accent,
    secondaryContainer = dark_card_background,
    onSecondaryContainer = dark_main_text,
    tertiary = dark_accent,
    onTertiary = dark_text_filled_button,
    tertiaryContainer = dark_card_background,
    onTertiaryContainer = dark_main_text,
    error = dark_error,
    errorContainer = dark_background,
    onError = dark_text_filled_button,
    onErrorContainer = dark_main_text,
    background = dark_background,
    onBackground = dark_main_text,
    surface = dark_background,
    onSurface = dark_main_text,
    surfaceVariant = dark_background,
    onSurfaceVariant = dark_small_elements,
    surfaceTint = dark_card_background,
    outline = dark_separators,
    inverseOnSurface = dark_background,
    inverseSurface = dark_main_text,
    inversePrimary = dark_background,
    outlineVariant = dark_separators,
    scrim = dark_main_text,
)

@Composable
fun JetpackWorkoutAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(
                window,
                view
            ).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}