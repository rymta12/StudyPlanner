package com.studyplanner.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore("app_theme")

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("selected_theme_id")
    private val bgKey = stringPreferencesKey("selected_bg_style")

    val currentTheme: Flow<AppTheme> = context.themeDataStore.data.map { prefs ->
        val id = prefs[themeKey] ?: AppThemes.default.id
        AppThemes.all.find { it.id == id } ?: AppThemes.default
    }

    val currentBgStyle: Flow<BgAnimationStyle> = context.themeDataStore.data.map { prefs ->
        val id = prefs[bgKey] ?: BgAnimationStyle.GRADIENT_FLOW.id
        BgAnimationStyle.entries.find { it.id == id } ?: BgAnimationStyle.GRADIENT_FLOW
    }

    suspend fun setTheme(themeId: String) {
        context.themeDataStore.edit { it[themeKey] = themeId }
    }

    suspend fun setBgStyle(style: BgAnimationStyle) {
        context.themeDataStore.edit { it[bgKey] = style.id }
    }
}

enum class BgAnimationStyle(val id: String, val displayName: String, val emoji: String) {
    GRADIENT_FLOW("gradient_flow", "Gradient Flow", "🌊"),
    PULSE("pulse", "Pulse", "💫"),
    PARTICLES("particles", "Particles", "✨"),
    STATIC("static", "Static", "🔲"),
}
