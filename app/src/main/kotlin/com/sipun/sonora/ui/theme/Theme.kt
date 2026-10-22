package com.sipun.sonora.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.data.preferences.AccentGradient
import com.sipun.sonora.data.preferences.AccentStyle
import com.sipun.sonora.data.preferences.AppTheme
import com.sipun.sonora.data.preferences.ThemePreferences

val LocalAccentBrush = staticCompositionLocalOf {
    Brush.horizontalGradient(
        listOf(Color(0xFFE92B2B), Color(0xFFFF7043)),
    )
}

@Composable
fun Modifier.accentGradientTint(): Modifier {
    val brush = LocalAccentBrush.current
    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
            }
        }
}

private fun accentGradient(gradient: AccentGradient): List<Color> = when (gradient) {
    AccentGradient.SUNSET -> listOf(Color(0xFFFF7A00), Color(0xFFFF006E))
    AccentGradient.OCEAN -> listOf(Color(0xFF0066FF), Color(0xFF00E5FF))
    AccentGradient.AURORA -> listOf(Color(0xFF00F5A0), Color(0xFF8A2BE2))
    AccentGradient.PURPLE_HAZE -> listOf(Color(0xFF6A00FF), Color(0xFFFF6EC7))
    AccentGradient.PEACH -> listOf(Color(0xFFFFD1A9), Color(0xFFFF6B6B))
    AccentGradient.EMERALD -> listOf(Color(0xFF006D5B), Color(0xFFA4FF6B))
    AccentGradient.CHERRY -> listOf(Color(0xFFFF1744), Color(0xFFFF8ED1))
    AccentGradient.ROYAL -> listOf(Color(0xFF1A2ED8), Color(0xFFFFDB35))
    AccentGradient.AMBER -> listOf(Color(0xFFFFC107), Color(0xFFFF3D00))
    AccentGradient.MINT -> listOf(Color(0xFF00E676), Color(0xFF40C4FF))
    AccentGradient.ROSE -> listOf(Color(0xFFD81B60), Color(0xFFFFAB91))
    AccentGradient.INDIGO -> listOf(Color(0xFF3949AB), Color(0xFF00BCD4))
    AccentGradient.CUSTOM -> listOf(Color(0xFFE92DE2), Color(0xFF4A00E0))
}

private val SonoraLightColors = lightColorScheme(
    primary = SonoraRed,
    onPrimary = Color.White,
    primaryContainer = SonoraRed.copy(alpha = 0.12f),
    onPrimaryContainer = SonoraRed,
    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF202124),
    surface = Color.White,
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF0F1F5),
    onSurfaceVariant = Color(0xFF777A83),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFFE0E1E6),
)

private val SonoraDarkColors = darkColorScheme(
    primary = SonoraRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5F0000),
    onPrimaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141313),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF1C1B1B),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF2A282B),
    onSurfaceVariant = Color(0xFFD8C2BE),
    surfaceContainerLow = Color(0xFF1C1B1F),
    surfaceContainer = Color(0xFF1C1B1F),
    surfaceContainerHigh = Color(0xFF1C1B1F),
    surfaceContainerHighest = Color(0xFF1C1B1F),
    outline = Color(0xFFA08C89),
)

private val SonoraPureBlackColors = darkColorScheme(
    primary = SonoraRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5F0000),
    onPrimaryContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color(0xFFF5F5F5),
    surface = Color.Black,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF121212),
    outline = Color(0xFF3A3A3A),
)

@Composable
fun SonoraTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val preferences = androidx.compose.runtime.remember(context) {
        ThemePreferences.from(context.applicationContext)
    }
    val selectedTheme by preferences.theme.collectAsStateWithLifecycle()
    val pureBlack by preferences.pureBlack.collectAsStateWithLifecycle()
    val dynamicColor by preferences.dynamicColor.collectAsStateWithLifecycle()
    val customColor by preferences.customColor.collectAsStateWithLifecycle()
    val accentStyle by preferences.accentStyle.collectAsStateWithLifecycle()
    val selectedGradient by preferences.accentGradient.collectAsStateWithLifecycle()
    val customGradientStart by preferences.customGradientStart.collectAsStateWithLifecycle()
    val customGradientEnd by preferences.customGradientEnd.collectAsStateWithLifecycle()
    val paletteStyle by preferences.dynamicPalette.collectAsStateWithLifecycle()
    var wallpaperSeedColor by androidx.compose.runtime.remember(context) {
        androidx.compose.runtime.mutableStateOf(
            readWallpaperSeed(context)
        )
    }

    androidx.compose.runtime.DisposableEffect(context, dynamicColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 || !dynamicColor) onDispose { }
        else {
            val manager = WallpaperManager.getInstance(context)
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0 && colors != null) wallpaperSeedColor =
                    Color(colors.primaryColor.toArgb())
            }
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            manager.addOnColorsChangedListener(listener, handler)
            onDispose { manager.removeOnColorsChangedListener(listener) }
        }
    }

    val gradientColors = if (selectedGradient == AccentGradient.CUSTOM) {
        listOf(Color(customGradientStart), Color(customGradientEnd))
    } else {
        accentGradient(selectedGradient)
    }
    val accentBrush =
        if (accentStyle == AccentStyle.GRADIENT) {
            Brush.horizontalGradient(gradientColors)
        } else {
            Brush.horizontalGradient(listOf(Color(customColor), Color(customColor)))
        }
    val accentColor =
        if (accentStyle == AccentStyle.GRADIENT) {
            gradientColors.first()
        } else {
            Color(customColor)
        }

    val darkTheme = pureBlack || when (selectedTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val palette = wallpaperPalette(wallpaperSeedColor, paletteStyle, darkTheme)
    val dynamicScheme = if (dynamicColor) {
        if (darkTheme) darkColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.container,
            onPrimaryContainer = Color.White,
            secondary = palette.secondary,
            onSecondary = Color.White,
            tertiary = palette.tertiary,
            onTertiary = Color.White,
            surfaceContainer = palette.container,
            surfaceContainerLow = palette.container,
            surfaceContainerHigh = palette.container,
            surfaceContainerHighest = palette.container,
        )
        else lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.container,
            onPrimaryContainer = Color.Black,
            secondary = palette.secondary,
            onSecondary = Color.White,
            tertiary = palette.tertiary,
            onTertiary = Color.White,
            background = SonoraLightColors.background,
            onBackground = SonoraLightColors.onBackground,
            surface = SonoraLightColors.surface,
            onSurface = SonoraLightColors.onSurface,
            surfaceVariant = SonoraLightColors.surfaceVariant,
            onSurfaceVariant = SonoraLightColors.onSurfaceVariant,
            surfaceContainerLow = SonoraLightColors.surfaceContainerLow,
            surfaceContainer = SonoraLightColors.surfaceContainerLow,
            surfaceContainerHigh = SonoraLightColors.surfaceContainerLow,
            surfaceContainerHighest = SonoraLightColors.surfaceContainerLow,
        )
    } else null

    val colorScheme = when {
        pureBlack -> (dynamicScheme ?: SonoraPureBlackColors).copy(
            primary = if (dynamicScheme != null) {
                dynamicScheme.primary
            } else {
                accentColor
            },
            onPrimary = Color.White,
            primaryContainer = if (dynamicScheme != null) {
                dynamicScheme.primaryContainer
            } else {
                accentColor.copy(alpha = 0.35f)
            },
            onPrimaryContainer = if (dynamicScheme != null) {
                dynamicScheme.onPrimaryContainer
            } else {
                accentColor
            },
            background = Color.Black,
            onBackground = Color(0xFFF5F5F5),
            surface = Color(0xFF121212),
            onSurface = Color(0xFFF5F5F5),
            surfaceVariant = Color(0xFF181818),
            onSurfaceVariant = Color(0xFFBDBDBD),
            surfaceContainerLow = Color(0xFF121212),
            surfaceContainer = Color(0xFF121212),
            surfaceContainerHigh = Color(0xFF121212),
            surfaceContainerHighest = Color(0xFF121212),
            outline = Color(0xFF3A3A3A),
        )

        dynamicScheme != null -> dynamicScheme.copy(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = if (darkTheme) 0.35f else 0.12f),
            onPrimaryContainer = if (darkTheme) accentColor else Color.Black,
            secondary = accentColor,
            tertiary = accentColor,
        )
        darkTheme -> SonoraDarkColors.copy(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.35f),
            onPrimaryContainer = accentColor,
        )

        else -> SonoraLightColors.copy(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = accentColor,
        )
    }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAccentBrush provides accentBrush) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SonoraTypography,
            shapes = SonoraShapes,
            content = content,
        )
    }
}

private fun readWallpaperSeed(context: android.content.Context): Color =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        androidx.compose.material3.dynamicLightColorScheme(context).primary
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?.primaryColor
            ?.let { Color(it.toArgb()) }
            ?: Color(0xFFE92B2B)
    } else {
        Color(0xFFE92B2B)
    }