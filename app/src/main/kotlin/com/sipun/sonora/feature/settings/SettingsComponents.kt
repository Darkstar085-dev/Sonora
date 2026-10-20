package com.sipun.sonora.feature.settings

import android.app.WallpaperManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.sipun.sonora.ui.theme.WallpaperPalette
import com.sipun.sonora.ui.theme.wallpaperPalette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
    onColorChange: (Int) -> Unit,
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
        Color(0xFF26A69A),
        Color(0xFF8BC34A),
        Color(0xFFD4E157),
        Color(0xFFFFCA28),
        Color(0xFFFFA726),
    )
    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }
    var hue by remember(color) { mutableStateOf(hsv[0]) }

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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            presetColors.forEach { preset ->
                Box(
                    modifier = Modifier
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
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(hueColors)),
            )

            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    val hsvColor = floatArrayOf(it, 0.78f, 0.92f)
                    onColorChange(android.graphics.Color.HSVToColor(hsvColor))
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
                        modifier = Modifier
                            .width(4.dp)
                            .height(34.dp)
                            .background(
                                Color.White,
                                RoundedCornerShape(50),
                            ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
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
                            tint = MaterialTheme.colorScheme.onPrimary,
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
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
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
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            tint = MaterialTheme.colorScheme.onPrimary,
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
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
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
