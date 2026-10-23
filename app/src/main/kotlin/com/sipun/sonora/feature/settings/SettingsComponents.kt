package com.sipun.sonora.feature.settings

import android.app.WallpaperManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.sipun.sonora.BuildConfig
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.DynamicPalette
import com.sipun.sonora.ui.theme.LocalAccentBrush
import com.sipun.sonora.ui.theme.WallpaperPalette
import com.sipun.sonora.ui.theme.wallpaperPalette

@Composable
internal fun SettingsPermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    status: String,
    onClick: (() -> Unit)?,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Surface(
                color = if (status == stringResource(R.string.settings_permission_allowed)) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    status,
                    color = if (status == stringResource(R.string.settings_permission_allowed)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        },
    )
}

@Composable
internal fun PlaybackSwitch(
    checked: Boolean,
    darkTheme: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = if (darkTheme) {
                MaterialTheme.colorScheme.onSurface
            } else {
                Color.White
            },
            uncheckedTrackColor = if (darkTheme) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color(0xFFE3E3E8)
            },
            uncheckedBorderColor = if (darkTheme) {
                MaterialTheme.colorScheme.outline
            } else {
                Color(0xFFC1C1C8)
            },
        ),
    )
}

@Composable
internal fun CrossfadeSelector(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        LocalAccentBrush.current,
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    stringResource(R.string.settings_crossfade),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (seconds == 0) {
                        stringResource(R.string.settings_crossfade_off)
                    } else {
                        stringResource(R.string.settings_crossfade_value, seconds)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Slider(
            value = seconds.toFloat(),
            onValueChange = { onSecondsChange(it.toInt()) },
            valueRange = 0f..12f,
            steps = 11,
            modifier = Modifier.padding(start = 42.dp, top = 2.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomColorSelector(
    color: Color,
    style: com.sipun.sonora.data.preferences.AccentStyle,
    gradient: com.sipun.sonora.data.preferences.AccentGradient,
    onColorChange: (Int) -> Unit,
    onStyleChange: (com.sipun.sonora.data.preferences.AccentStyle) -> Unit,
    onGradientChange: (com.sipun.sonora.data.preferences.AccentGradient) -> Unit,
    customGradientStart: Int,
    customGradientEnd: Int,
    onCustomGradientChange: (Int, Int) -> Unit,
) {
    val presetColors = listOf(
        Color(0xFFE92B2B),
        Color(0xFFE57373),
        Color(0xFF42A5F5),
        Color(0xFF26A69A),
        Color(0xFF66BB6A),
        Color(0xFF9CCC65),
        Color(0xFFFFB74D),
        Color(0xFFFF7043),
        Color(0xFFAB47BC),
        Color(0xFF7E57C2),
        Color(0xFF5C6BC0),
        Color(0xFF26C6DA),
        Color(0xFF8BC34A),
        Color(0xFFFFCA28),
        Color(0xFFFFA726),
    )
    val gradients = listOf(
        com.sipun.sonora.data.preferences.AccentGradient.SUNSET to listOf(
            Color(0xFFFF7A00),
            Color(0xFFFF006E)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.OCEAN to listOf(
            Color(0xFF0066FF),
            Color(0xFF00E5FF)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.AURORA to listOf(
            Color(0xFF00F5A0),
            Color(0xFF8A2BE2)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.PURPLE_HAZE to listOf(
            Color(0xFF6A00FF),
            Color(0xFFFF6EC7)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.PEACH to listOf(
            Color(0xFFFFD1A9),
            Color(0xFFFF6B6B)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.EMERALD to listOf(
            Color(0xFF006D5B),
            Color(0xFFA4FF6B)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.CHERRY to listOf(
            Color(0xFFFF1744),
            Color(0xFFFF8ED1)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.ROYAL to listOf(
            Color(0xFF1A2ED8),
            Color(0xFFFFDB35)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.AMBER to listOf(
            Color(0xFFFFC107),
            Color(0xFFFF3D00)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.MINT to listOf(
            Color(0xFF00E676),
            Color(0xFF40C4FF)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.ROSE to listOf(
            Color(0xFFD81B60),
            Color(0xFFFFAB91)
        ),
        com.sipun.sonora.data.preferences.AccentGradient.INDIGO to listOf(
            Color(0xFF3949AB),
            Color(0xFF00BCD4)
        ),
    )
    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }
    var hue by remember(color) { mutableStateOf(hsv[0]) }
    var showCustomGradientEditor by remember { mutableStateOf(false) }
    val startHsv = remember(customGradientStart) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(customGradientStart, it) }
    }
    val endHsv = remember(customGradientEnd) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(customGradientEnd, it) }
    }
    var customStartHue by remember(showCustomGradientEditor, customGradientStart) {
        mutableStateOf(
            startHsv[0]
        )
    }
    var customEndHue by remember(showCustomGradientEditor, customGradientEnd) {
        mutableStateOf(
            endHsv[0]
        )
    }
    val hueColors = remember {
        (0..360 step 30).map { Color.hsv(it.toFloat(), 0.78f, 0.92f) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.settings_custom_color),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.settings_custom_color_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                com.sipun.sonora.data.preferences.AccentStyle.SOLID,
                com.sipun.sonora.data.preferences.AccentStyle.GRADIENT,
            ).forEach { option ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (style == option) {
                                Modifier.background(
                                    LocalAccentBrush.current,
                                    RoundedCornerShape(14.dp)
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onStyleChange(option) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (style == option) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (style == option) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                    ),
                ) {
                    Text(
                        text = if (option == com.sipun.sonora.data.preferences.AccentStyle.SOLID) {
                            "Solid color"
                        } else {
                            "Gradient"
                        },
                        modifier = Modifier.padding(vertical = 11.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (style == option) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        if (style == com.sipun.sonora.data.preferences.AccentStyle.GRADIENT) {
            Text(
                "Choose a gradient",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
            gradients.chunked(4).forEach { rowGradients ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowGradients.forEach { (preset, colors) ->
                        val selected = gradient == preset
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onGradientChange(preset) }
                                .padding(bottom = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.horizontalGradient(colors))
                                    .then(
                                        if (selected) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            RoundedCornerShape(12.dp),
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Text(
                                preset.name.lowercase().replace('_', ' ')
                                    .replaceFirstChar { it.uppercase() },
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    repeat(4 - rowGradients.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { showCustomGradientEditor = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 64.dp, height = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(customGradientStart),
                                        Color(customGradientEnd)
                                    )
                                )
                            ),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Custom gradient",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Choose both colors and preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Edit",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (showCustomGradientEditor) {
                val startColor = Color.hsv(customStartHue, 0.85f, 0.95f)
                val endColor = Color.hsv(customEndHue, 0.85f, 0.95f)
                AlertDialog(
                    onDismissRequest = { showCustomGradientEditor = false },
                    title = { Text("Custom gradient") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                startColor,
                                                endColor
                                            )
                                        )
                                    ),
                            )
                            Text("Start color", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = customStartHue,
                                onValueChange = { customStartHue = it },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = startColor,
                                    activeTrackColor = startColor
                                ),
                            )
                            Text("End color", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = customEndHue,
                                onValueChange = { customEndHue = it },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = endColor,
                                    activeTrackColor = endColor
                                ),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            onCustomGradientChange(startColor.toArgb(), endColor.toArgb())
                            onGradientChange(com.sipun.sonora.data.preferences.AccentGradient.CUSTOM)
                            onStyleChange(com.sipun.sonora.data.preferences.AccentStyle.GRADIENT)
                            showCustomGradientEditor = false
                        }) { Text("Apply gradient") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCustomGradientEditor = false
                        }) { Text("Cancel") }
                    },
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                presetColors.forEach { preset ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(preset, RoundedCornerShape(50))
                            .clickable {
                                val presetHsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(preset.toArgb(), presetHsv)
                                hue = presetHsv[0]
                                onColorChange(preset.toArgb())
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (color.toArgb() == preset.toArgb()) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(hueColors)),
                )
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        onColorChange(
                            android.graphics.Color.HSVToColor(floatArrayOf(it, 0.78f, 0.92f)),
                        )
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                    thumb = {
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(34.dp)
                                .background(Color.White, RoundedCornerShape(50)),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun PaletteSelector(
    seed: Color,
    darkTheme: Boolean,
    selected: DynamicPalette,
    onSelect: (DynamicPalette) -> Unit,
) {
    val palettes = DynamicPalette.entries.take(4)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.settings_palette_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            palettes.forEach { palette ->
                PaletteCard(
                    palette = palette,
                    colors = wallpaperPalette(seed, palette, darkTheme),
                    selected = palette == selected,
                    onClick = { onSelect(palette) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun PaletteCard(
    palette: DynamicPalette,
    colors: WallpaperPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paletteColors = listOf(
        colors.primary,
        colors.secondary,
        colors.tertiary,
        colors.container,
    )

    Card(
        modifier = modifier
            .height(132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 1.dp,
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            },
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(6.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(15.dp)),
            ) {
                paletteColors.forEachIndexed { index, color ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(color),
                    ) {
                        if (index < paletteColors.lastIndex) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)),
                            )
                        }
                    }
                }
            }

            if (selected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .size(28.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

internal fun readWallpaperSeed(context: android.content.Context): Color =
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

internal const val SONORA_REPOSITORY = "https://github.com/Darkstar085/Sonora"

@Composable
internal fun SonoraAboutDialog(
    onDismiss: () -> Unit,
    onCheckForUpdates: () -> Unit,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.about_sound_wave),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = Int.MAX_VALUE,
    )
    val loveComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.love),
    )
    val loveProgress by animateLottieCompositionAsState(
        composition = loveComposition,
        iterations = Int.MAX_VALUE,
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.sonora_icon_foreground_art),
                            contentDescription = stringResource(R.string.about_sonora),
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.about_sonora),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )

                Text(
                    text = stringResource(R.string.about_sonora_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                val animationColor = MaterialTheme.colorScheme.primary.toArgb()
                val dynamicProperties = rememberLottieDynamicProperties(
                    rememberLottieDynamicProperty(
                        property = LottieProperty.COLOR,
                        value = animationColor,
                        "**",
                        "Fill 1",
                    ),
                )

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    dynamicProperties = dynamicProperties,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .padding(top = 14.dp),
                )

                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable(onClick = onCheckForUpdates),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.about_made_with_prefix),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LottieAnimation(
                        composition = loveComposition,
                        progress = { loveProgress },
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = stringResource(R.string.about_made_with_suffix),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ThemeOption(
    title: String,
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        } else {
            Color.Transparent
        },
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                alpha = 0.45f
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (selected) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(
                    LocalAccentBrush.current,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color.White)
        }
        Column(
            Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        trailing?.invoke()
    }
}
