package com.studyplanner.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppTheme = staticCompositionLocalOf<AppTheme> { AppThemes.default }
val LocalBgStyle = staticCompositionLocalOf<BgAnimationStyle> { BgAnimationStyle.GRADIENT_FLOW }

@Composable
fun StudyPlannerTheme(
    appTheme: AppTheme = AppThemes.default,
    bgStyle: BgAnimationStyle = BgAnimationStyle.GRADIENT_FLOW,
    content: @Composable () -> Unit
) {
    val c = appTheme.colors
    val colorScheme = if (appTheme.isDark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary, onSecondary = c.onSecondary,
            secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary, onTertiary = c.onTertiary,
            background = c.background, onBackground = c.onBackground,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline, error = c.error, onError = c.onError,
            errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary, onSecondary = c.onSecondary,
            secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary, onTertiary = c.onTertiary,
            background = c.background, onBackground = c.onBackground,
            surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline, error = c.error, onError = c.onError,
            errorContainer = c.errorContainer, onErrorContainer = c.onErrorContainer,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = c.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !appTheme.isDark
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalBgStyle provides bgStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StudyPlannerTypography,
            shapes = StudyPlannerShapes,
            content = content
        )
    }
}
