package com.sipun.sonora

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sipun.sonora.core.update.UpdateInstaller
import com.sipun.sonora.core.update.UpdateManager
import com.sipun.sonora.core.update.UpdateNotificationHelper
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.OnboardingPreferences
import com.sipun.sonora.navigation.SonoraApp
import com.sipun.sonora.ui.onboarding.WelcomeScreen
import com.sipun.sonora.ui.theme.SonoraTheme
import com.sipun.sonora.ui.update.UpdateContent

class MainActivity : ComponentActivity() {
    private var pendingAudioUri by mutableStateOf<Uri?>(null)
    private val updateCheckRequested = mutableStateOf(false)
    private val updateNotificationRequested = mutableStateOf(false)
    private var batteryOptimizationPromptRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidMusicRepository.notifyMetadataChanged()
        configureSystemBars()
        UpdateNotificationHelper.createChannel(this)
        handleUpdateIntent(intent)
        handleAudioIntent(intent)
        setContent { SonoraTheme { SonoraPermissionGate() } }
    }

    @Composable
    private fun SonoraPermissionGate() {
        val permissions = remember {
            buildList {
                add(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                    add(Manifest.permission.READ_MEDIA_VIDEO)
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        val musicPermission = permissions.first()
        val onboardingPreferences = remember(this) {
            OnboardingPreferences(applicationContext)
        }
        val initialPermissionGranted = remember {
            ContextCompat.checkSelfPermission(this, musicPermission) ==
                    PackageManager.PERMISSION_GRANTED
        }
        var permissionGranted by remember {
            mutableStateOf(initialPermissionGranted)
        }
        var welcomeComplete by remember {
            mutableStateOf(
                onboardingPreferences.isWelcomeComplete() || initialPermissionGranted
            )
        }
        var showExplanation by remember { mutableStateOf(false) }

        LaunchedEffect(permissionGranted) {
            if (permissionGranted) requestBatteryOptimizationExemption()
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val musicGranted = result[musicPermission] == true
            permissionGranted = musicGranted
            if (musicGranted) {
                onboardingPreferences.setWelcomeComplete()
                welcomeComplete = true
            } else {
                showExplanation = false
            }
        }

        when {
            !welcomeComplete -> {
                WelcomeScreen(
                    onRequestMusicAccess = { launcher.launch(permissions.toTypedArray()) },
                )
            }

            permissionGranted -> {
                SonoraContent(
                    externalAudioUri = pendingAudioUri,
                    onExternalAudioConsumed = { pendingAudioUri = null },
                )
            }

            else -> {
                if (!showExplanation) {
                    PermissionRequiredScreen {
                        showExplanation = true
                    }
                } else {
                    AlertDialog(
                        onDismissRequest = { showExplanation = false },
                        title = { Text(stringResource(R.string.permission_title)) },
                        text = {
                            Text(stringResource(R.string.permission_explanation))
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showExplanation = false
                                    launcher.launch(permissions.toTypedArray())
                                },
                            ) {
                                Text(stringResource(R.string.action_allow))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExplanation = false }) {
                                Text(stringResource(R.string.action_not_now))
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun SonoraContent(
        externalAudioUri: Uri? = null,
        onExternalAudioConsumed: () -> Unit = {},
    ) {
        val appIcon = remember {
            requireNotNull(ContextCompat.getDrawable(this, R.drawable.sonora_icon_foreground))
                .toBitmap()
                .asImageBitmap()
        }
        val checkRequested by updateCheckRequested
        val notificationRequested by updateNotificationRequested

        SonoraApp(
            onCheckForUpdates = { updateCheckRequested.value = true },
            externalAudioUri = pendingAudioUri,
            onExternalAudioConsumed = { pendingAudioUri = null },
        )
        UpdateContent(
            checkRequested = checkRequested,
            onCheckRequestConsumed = { updateCheckRequested.value = false },
            onRequestCheck = { updateCheckRequested.value = true },
            notificationRequested = notificationRequested,
            onNotificationRequestConsumed = {
                updateNotificationRequested.value = false
            },
            appIcon = appIcon,
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
        handleAudioIntent(intent)
    }

    private fun handleAudioIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW || intent.data == null) return
        if (intent.resolveTypeIfNeeded(contentResolver)?.startsWith("audio/") != true) return
        if (!hasMusicPermission()) {
            pendingAudioUri = intent.data
            return
        }
        pendingAudioUri = intent.data
    }

    private fun hasMusicPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || batteryOptimizationPromptRequested) return

        val powerManager = getSystemService(PowerManager::class.java) ?: return
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        batteryOptimizationPromptRequested = true
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + packageName),
            ),
        )
    }

    private fun configureSystemBars() {
        enableEdgeToEdge()
    }

    private fun handleUpdateIntent(intent: Intent?) {
        when (intent?.action) {
            UpdateManager.ACTION_SHOW_UPDATE -> {
                updateNotificationRequested.value = true
            }

            UpdateManager.ACTION_INSTALL_UPDATE -> {
                val installer = UpdateInstaller(applicationContext)
                if (
                    installer.installDownloadedUpdate() ==
                    UpdateInstaller.Result.PermissionRequired
                ) {
                    installer.openInstallPermissionSettings()
                }
            }
        }
    }

    @Composable
    private fun PermissionRequiredScreen(onRequest: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.permission_required_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                stringResource(R.string.permission_required_message),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequest,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text(stringResource(R.string.action_allow_access))
            }
        }
    }
}
