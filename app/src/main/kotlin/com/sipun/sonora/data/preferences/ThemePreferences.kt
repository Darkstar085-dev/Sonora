package com.sipun.sonora.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AccentStyle {
    SOLID,
    GRADIENT,
}

enum class AccentGradient {
    SUNSET,
    OCEAN,
    AURORA,
    PURPLE_HAZE,
    PEACH,
    EMERALD,
    CHERRY,
    ROYAL,
    AMBER,
    MINT,
    ROSE,
    INDIGO,
    CUSTOM,
}

enum class DynamicPalette {
    TONAL_SPOT,
    VIBRANT,
    EXPRESSIVE,
    NEUTRAL,
    MONOCHROME,
}

class ThemePreferences private constructor(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _pureBlack = MutableStateFlow(readPureBlack())
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _dynamicColor = MutableStateFlow(readDynamicColor())
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _dynamicPalette = MutableStateFlow(readDynamicPalette())
    val dynamicPalette: StateFlow<DynamicPalette> = _dynamicPalette.asStateFlow()

    private val _customColor = MutableStateFlow(readCustomColor())
    val customColor: StateFlow<Int> = _customColor.asStateFlow()

    private val _accentStyle = MutableStateFlow(readAccentStyle())
    val accentStyle: StateFlow<AccentStyle> = _accentStyle.asStateFlow()
    private val _accentGradient = MutableStateFlow(readAccentGradient())
    val accentGradient: StateFlow<AccentGradient> = _accentGradient.asStateFlow()
    private val _customGradientStart =
        MutableStateFlow(preferences.getInt(KEY_CUSTOM_GRADIENT_START, 0xFFE92DE2.toInt()))
    val customGradientStart: StateFlow<Int> = _customGradientStart.asStateFlow()
    private val _customGradientEnd =
        MutableStateFlow(preferences.getInt(KEY_CUSTOM_GRADIENT_END, 0xFF4A00E0.toInt()))
    val customGradientEnd: StateFlow<Int> = _customGradientEnd.asStateFlow()

    fun setTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    fun setPureBlack(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PURE_BLACK, enabled).apply()
        _pureBlack.value = enabled
    }

    fun setDynamicColor(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setDynamicPalette(palette: DynamicPalette) {
        preferences.edit().putString(KEY_DYNAMIC_PALETTE, palette.name).apply()
        _dynamicPalette.value = palette
    }

    fun setCustomColor(color: Int) {
        preferences.edit().putInt(KEY_CUSTOM_COLOR, color).apply()
        _customColor.value = color
    }

    fun setAccentStyle(style: AccentStyle) {
        preferences.edit().putString(KEY_ACCENT_STYLE, style.name).apply()
        _accentStyle.value = style
    }

    fun setAccentGradient(gradient: AccentGradient) {
        preferences.edit().putString(KEY_ACCENT_GRADIENT, gradient.name).apply()
        _accentGradient.value = gradient
    }

    fun setCustomGradientColors(start: Int, end: Int) {
        preferences.edit()
            .putInt(KEY_CUSTOM_GRADIENT_START, start)
            .putInt(KEY_CUSTOM_GRADIENT_END, end)
            .apply()
        _customGradientStart.value = start
        _customGradientEnd.value = end
    }

    private fun readAccentStyle(): AccentStyle =
        runCatching {
            AccentStyle.valueOf(
                preferences.getString(KEY_ACCENT_STYLE, AccentStyle.SOLID.name)
                    ?: AccentStyle.SOLID.name,
            )
        }.getOrDefault(AccentStyle.SOLID)

    private fun readAccentGradient(): AccentGradient =
        runCatching {
            AccentGradient.valueOf(
                preferences.getString(KEY_ACCENT_GRADIENT, AccentGradient.SUNSET.name)
                    ?: AccentGradient.SUNSET.name,
            )
        }.getOrDefault(AccentGradient.SUNSET)

    private fun readPureBlack(): Boolean =
        preferences.getBoolean(KEY_PURE_BLACK, false)

    private fun readDynamicColor(): Boolean =
        preferences.getBoolean(KEY_DYNAMIC_COLOR, false)

    private fun readCustomColor(): Int = preferences.getInt(KEY_CUSTOM_COLOR, 0xFFE92B2B.toInt())

    private fun readDynamicPalette(): DynamicPalette =
        runCatching {
            DynamicPalette.valueOf(
                preferences.getString(KEY_DYNAMIC_PALETTE, DynamicPalette.TONAL_SPOT.name)
                    ?: DynamicPalette.TONAL_SPOT.name
            )
        }.getOrDefault(DynamicPalette.TONAL_SPOT)

    private fun readTheme(): AppTheme =
        runCatching {
            AppTheme.valueOf(
                preferences.getString(KEY_THEME, AppTheme.SYSTEM.name)
                    ?: AppTheme.SYSTEM.name
            )
        }.getOrDefault(AppTheme.SYSTEM)

    companion object {
        private const val PREFERENCES_NAME = "sonora_preferences"
        private const val KEY_THEME = "app_theme"
        private const val KEY_PURE_BLACK = "pure_black"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_DYNAMIC_PALETTE = "dynamic_palette"
        private const val KEY_CUSTOM_COLOR = "custom_color"
        private const val KEY_ACCENT_STYLE = "accent_style"
        private const val KEY_ACCENT_GRADIENT = "accent_gradient"
        private const val KEY_CUSTOM_GRADIENT_START = "custom_gradient_start"
        private const val KEY_CUSTOM_GRADIENT_END = "custom_gradient_end"

        @Volatile
        private var instance: ThemePreferences? = null

        fun from(context: Context): ThemePreferences =
            instance ?: synchronized(this) {
                instance ?: ThemePreferences(context.applicationContext).also {
                    instance = it
                }
            }
    }
}
