package com.sipun.sonora.feature.settings

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.AppTheme
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.data.preferences.ThemePreferences
import com.sipun.sonora.ui.theme.BrushScriptFont

@Composable
fun SettingsScreen(
    onCheckForUpdates: () -> Unit = {},
) {
    val context = LocalContext.current
    val themePreferences = remember(context) {
        ThemePreferences.from(context.applicationContext)
    }
    val playbackPreferences = remember(context) {
        SonoraPreferences(context.applicationContext)
    }
    val selectedTheme by themePreferences.theme.collectAsStateWithLifecycle()
    val pureBlack by themePreferences.pureBlack.collectAsStateWithLifecycle()
    val dynamicColor by themePreferences.dynamicColor.collectAsStateWithLifecycle()
    val selectedPalette by themePreferences.dynamicPalette.collectAsStateWithLifecycle()
    val customColor by themePreferences.customColor.collectAsStateWithLifecycle()
    val accentStyle by themePreferences.accentStyle.collectAsStateWithLifecycle()
    val accentGradient by themePreferences.accentGradient.collectAsStateWithLifecycle()
    val customGradientStart by themePreferences.customGradientStart.collectAsStateWithLifecycle()
    val customGradientEnd by themePreferences.customGradientEnd.collectAsStateWithLifecycle()
    var resumePlayback by remember { mutableStateOf(playbackPreferences.resumePlayback()) }
    var gaplessPlayback by remember { mutableStateOf(playbackPreferences.gaplessPlayback()) }
    var crossfadeSeconds by remember { mutableIntStateOf(playbackPreferences.crossfadeSeconds()) }
    var normalizeVolume by remember { mutableStateOf(playbackPreferences.normalizeVolume()) }
    var keepScreenAwake by remember { mutableStateOf(playbackPreferences.keepScreenAwake()) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSonoraDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var mediaManagementAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    MediaStore.canManageMedia(context),
        )
    }
    var ringtoneAllowed by remember(context) {
        mutableStateOf(Settings.System.canWrite(context))
    }
    var installUpdatesAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls(),
        )
    }
    var batteryUnrestricted by remember(context) {
        mutableStateOf(
            (context.getSystemService(PowerManager::class.java))
                ?.isIgnoringBatteryOptimizations(context.packageName) == true,
        )
    }
    var musicPermissionAllowed by remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                },
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var photoPermissionAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES,
                    ) == PackageManager.PERMISSION_GRANTED ||
                    (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                                    ) == PackageManager.PERMISSION_GRANTED
                            ),
        )
    }
    var notificationPermissionAllowed by remember(context) {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    fun refreshPermissionStates() {
        mediaManagementAllowed =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    MediaStore.canManageMedia(context)
        ringtoneAllowed = Settings.System.canWrite(context)
        installUpdatesAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls()
        batteryUnrestricted =
            (context.getSystemService(PowerManager::class.java))
                ?.isIgnoringBatteryOptimizations(context.packageName) == true
        musicPermissionAllowed = ContextCompat.checkSelfPermission(
            context,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            },
        ) == PackageManager.PERMISSION_GRANTED
        photoPermissionAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES,
                    ) == PackageManager.PERMISSION_GRANTED ||
                    (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                                    ) == PackageManager.PERMISSION_GRANTED
                            )
        notificationPermissionAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var wallpaperSeed by remember(context) { mutableStateOf(Color(0xFFE92B2B)) }

    DisposableEffect(context, dynamicColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 || !dynamicColor) {
            onDispose { }
        } else {
            wallpaperSeed = readWallpaperSeed(context)
            val manager = WallpaperManager.getInstance(context)
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0 && colors != null) {
                    wallpaperSeed = Color(colors.primaryColor.toArgb())
                }
            }
            val handler = Handler(Looper.getMainLooper())
            manager.addOnColorsChangedListener(listener, handler)
            onDispose { manager.removeOnColorsChangedListener(listener) }
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissionStates()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val setupPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshPermissionStates()
    }

    fun openMediaManagementSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                        data = Uri.parse("package:" + context.packageName)
                    },
                )
            }
        }
    }

    fun openRingtoneSettings() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + context.packageName),
                ),
            )
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.packageName),
                ),
            )
        }
    }

    fun openBatterySettings(enable: Boolean) {
        val action = if (enable) {
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        } else {
            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        runCatching {
            context.startActivity(
                Intent(action).apply {
                    if (enable) {
                        data = Uri.parse("package:" + context.packageName)
                    }
                },
            )
        }
    }

    val darkTheme = pureBlack || when (selectedTheme) {
        AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val themeLabel = if (pureBlack) {
        stringResource(R.string.settings_pure_black)
    } else {
        when (selectedTheme) {
            AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
            AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
            AppTheme.SYSTEM -> stringResource(R.string.settings_theme_device_default)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(top = 20.dp, bottom = 18.dp)) {
            Text(
                stringResource(R.string.settings_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = BrushScriptFont,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                ),
            )
            Text(
                stringResource(R.string.settings_tagline),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SettingsSection(stringResource(R.string.settings_appearance)) {
            SettingsRow(
                icon = Icons.Default.LightMode,
                title = stringResource(R.string.settings_theme),
                subtitle = themeLabel,
                onClick = { showThemeDialog = true },
                trailing = {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            themeLabel,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                },
            )
            SettingsRow(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_detail),
                trailing = {
                    Switch(
                        checked = dynamicColor,
                        onCheckedChange = themePreferences::setDynamicColor,
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
                },
            )
            if (dynamicColor) {
                PaletteSelector(
                    seed = wallpaperSeed,
                    darkTheme = darkTheme,
                    selected = selectedPalette,
                    onSelect = themePreferences::setDynamicPalette,
                )
            }

            if (!dynamicColor) {
                CustomColorSelector(
                    color = Color(customColor),
                    style = accentStyle,
                    gradient = accentGradient,
                    onColorChange = { themePreferences.setCustomColor(it) },
                    onStyleChange = themePreferences::setAccentStyle,
                    onGradientChange = themePreferences::setAccentGradient,
                    customGradientStart = customGradientStart,
                    customGradientEnd = customGradientEnd,
                    onCustomGradientChange = themePreferences::setCustomGradientColors,
                )
            }
        }

        SettingsSection(stringResource(R.string.settings_library_folders)) {
            SettingsRow(
                icon = Icons.Default.FolderOpen,
                title = stringResource(R.string.settings_excluded_folders),
                subtitle = stringResource(R.string.settings_excluded_folders_detail),
                onClick = {
                    showFolderDialog = true
                },
            )
        }

        SettingsSection(stringResource(R.string.settings_playback)) {
            SettingsRow(
                icon = Icons.Default.PlayArrow,
                title = stringResource(R.string.settings_resume_playback),
                subtitle = stringResource(R.string.settings_resume_playback_detail),
                trailing = {
                    PlaybackSwitch(
                        checked = resumePlayback,
                        darkTheme = darkTheme,
                        onCheckedChange = {
                            resumePlayback = it
                            playbackPreferences.setResumePlayback(it)
                        },
                    )
                },
            )
            SettingsRow(
                icon = Icons.Default.AllInclusive,
                title = stringResource(R.string.settings_gapless_playback),
                subtitle = stringResource(R.string.settings_gapless_playback_detail),
                trailing = {
                    PlaybackSwitch(
                        checked = gaplessPlayback,
                        darkTheme = darkTheme,
                        onCheckedChange = {
                            gaplessPlayback = it
                            playbackPreferences.setGaplessPlayback(it)
                        },
                    )
                },
            )
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.settings_normalize_volume),
                subtitle = stringResource(R.string.settings_normalize_volume_detail),
                trailing = {
                    PlaybackSwitch(
                        checked = normalizeVolume,
                        darkTheme = darkTheme,
                        onCheckedChange = {
                            normalizeVolume = it
                            playbackPreferences.setNormalizeVolume(it)
                        },
                    )
                },
            )
            SettingsRow(
                icon = Icons.Default.Visibility,
                title = stringResource(R.string.settings_keep_screen_awake),
                subtitle = stringResource(R.string.settings_keep_screen_awake_detail),
                trailing = {
                    PlaybackSwitch(
                        checked = keepScreenAwake,
                        darkTheme = darkTheme,
                        onCheckedChange = {
                            keepScreenAwake = it
                            playbackPreferences.setKeepScreenAwake(it)
                        },
                    )
                },
            )
            CrossfadeSelector(
                seconds = crossfadeSeconds,
                onSecondsChange = {
                    crossfadeSeconds = it
                    playbackPreferences.setCrossfadeSeconds(it)
                },
            )
        }

        SettingsSection(stringResource(R.string.settings_permissions)) {
            if (!musicPermissionAllowed) {
                SettingsPermissionRow(
                    icon = Icons.Default.MusicNote,
                    title = stringResource(R.string.settings_permission_music),
                    subtitle = stringResource(R.string.settings_permission_music_detail),
                    status = stringResource(R.string.settings_permission_allow),
                    onClick = {
                        setupPermissionLauncher.launch(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            },
                        )
                    },
                )
            }
            if (!photoPermissionAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SettingsPermissionRow(
                    icon = Icons.Default.Image,
                    title = stringResource(R.string.settings_permission_photos),
                    subtitle = stringResource(R.string.settings_permission_photos_detail),
                    status = stringResource(R.string.settings_permission_allow),
                    onClick = {
                        setupPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    },
                )
            }
            if (!notificationPermissionAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SettingsPermissionRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_permission_notifications),
                    subtitle = stringResource(R.string.settings_permission_notifications_detail),
                    status = stringResource(R.string.settings_permission_allow),
                    onClick = {
                        setupPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            SettingsPermissionRow(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.settings_permission_media_edit),
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    stringResource(R.string.settings_permission_media_edit_detail)
                } else {
                    stringResource(R.string.settings_permission_media_ondemand)
                },
                status = when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
                        stringResource(R.string.settings_permission_ondemand)

                    mediaManagementAllowed ->
                        stringResource(R.string.settings_permission_allowed)

                    else ->
                        stringResource(R.string.settings_permission_allow)
                },
                onClick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ::openMediaManagementSettings
                } else {
                    null
                },
            )
            SettingsPermissionRow(
                icon = Icons.Default.DeleteOutline,
                title = stringResource(R.string.settings_permission_media_delete),
                subtitle = stringResource(R.string.settings_permission_media_delete_ondemand),
                status = stringResource(R.string.settings_permission_ondemand),
                onClick = null,
            )
            SettingsPermissionRow(
                icon = Icons.Default.Phone,
                title = stringResource(R.string.settings_permission_ringtone),
                subtitle = stringResource(R.string.settings_permission_ringtone_detail),
                status = if (ringtoneAllowed) {
                    stringResource(R.string.settings_permission_allowed)
                } else {
                    stringResource(R.string.settings_permission_allow)
                },
                onClick = ::openRingtoneSettings,
            )
            SettingsPermissionRow(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.settings_permission_install_updates),
                subtitle = stringResource(R.string.settings_permission_install_updates_detail),
                status = if (installUpdatesAllowed) {
                    stringResource(R.string.settings_permission_allowed)
                } else {
                    stringResource(R.string.settings_permission_allow)
                },
                onClick = ::openInstallPermissionSettings,
            )
            SettingsRow(
                icon = Icons.Default.BatteryFull,
                title = stringResource(R.string.settings_unrestricted_battery),
                subtitle = stringResource(R.string.settings_unrestricted_battery_detail),
                onClick = { openBatterySettings(!batteryUnrestricted) },
                trailing = {
                    PlaybackSwitch(
                        checked = batteryUnrestricted,
                        darkTheme = darkTheme,
                        onCheckedChange = { openBatterySettings(it) },
                    )
                },
            )
        }

        SettingsSection(stringResource(R.string.settings_about)) {
            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_sonora),
                subtitle = stringResource(R.string.about_sonora_detail),
                onClick = { showSonoraDialog = true },
            )
            SettingsRow(
                icon = Icons.Default.Code,
                title = stringResource(R.string.about_open_source),
                subtitle = stringResource(R.string.about_open_source_detail),
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(SONORA_REPOSITORY)),
                    )
                },
            )
            SettingsRow(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.about_contact),
                subtitle = stringResource(R.string.about_contact_detail),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://t.me/Darkstar085"),
                            ),
                        )
                    }
                },
            )
        }
        Spacer(Modifier.height(112.dp))
    }

    if (showSonoraDialog) {
        SonoraAboutDialog(
            onDismiss = { showSonoraDialog = false },
            onCheckForUpdates = onCheckForUpdates,
        )
    }

    if (showFolderDialog) {
        FolderManagementDialog(
            onDismiss = { showFolderDialog = false }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Choose how Sonora looks on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_device_default),
                        icon = Icons.Default.BrightnessAuto,
                        description = "Follows your device settings",
                        selected = !pureBlack && selectedTheme == AppTheme.SYSTEM,
                        onClick = {
                            themePreferences.setPureBlack(false)
                            themePreferences.setTheme(AppTheme.SYSTEM)
                            showThemeDialog = false
                        },
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_light),
                        icon = Icons.Default.LightMode,
                        description = "Bright and clear",
                        selected = !pureBlack && selectedTheme == AppTheme.LIGHT,
                        onClick = {
                            themePreferences.setPureBlack(false)
                            themePreferences.setTheme(AppTheme.LIGHT)
                            showThemeDialog = false
                        },
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_theme_dark),
                        icon = Icons.Default.DarkMode,
                        description = "Easy on the eyes",
                        selected = !pureBlack && selectedTheme == AppTheme.DARK,
                        onClick = {
                            themePreferences.setPureBlack(false)
                            themePreferences.setTheme(AppTheme.DARK)
                            showThemeDialog = false
                        },
                    )
                    ThemeOption(
                        title = stringResource(R.string.settings_pure_black),
                        icon = Icons.Default.DarkMode,
                        description = "True black surfaces for dark mode",
                        selected = pureBlack,
                        onClick = {
                            themePreferences.setPureBlack(true)
                            showThemeDialog = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeDialog = false },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
