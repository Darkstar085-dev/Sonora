package com.sipun.sonora.ui.update

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sipun.sonora.BuildConfig
import com.sipun.sonora.R
import com.sipun.sonora.core.update.AppUpdate
import com.sipun.sonora.core.update.DownloadProgress
import com.sipun.sonora.core.update.UpdateInstaller
import com.sipun.sonora.core.update.UpdateManager
import com.sipun.sonora.ui.components.DownloadProgressDialog
import com.sipun.sonora.ui.components.UpdateDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun UpdateContent(
    checkRequested: Boolean,
    onCheckRequestConsumed: () -> Unit,
    onRequestCheck: () -> Unit,
    notificationRequested: Boolean,
    onNotificationRequestConsumed: () -> Unit,
    appIcon: ImageBitmap,
) {
    val context = LocalContext.current
    val installer = remember { UpdateInstaller(context.applicationContext) }
    val updateDownloadFailedMessage = stringResource(R.string.update_download_failed)
    val updateFileMissingMessage = stringResource(R.string.update_file_missing)
    val updatePermissionRequiredMessage = stringResource(R.string.update_permission_required)
    val updateInstallFailedMessage = stringResource(R.string.update_install_failed)
    var pendingUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var downloadedUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<DownloadProgress?>(null) }
    var showNoUpdate by remember { mutableStateOf(false) }
    var checkingForUpdate by remember { mutableStateOf(false) }
    var updateCheckFailed by remember { mutableStateOf(false) }
    var updateCheckError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UpdateManager.enqueuePeriodicCheck(context)
        val stored = withContext(Dispatchers.IO) {
            UpdateManager.getValidatedPendingUpdate(context)
        }
        if (stored != null) {
            pendingUpdate = stored
            downloadedUpdate = withContext(Dispatchers.IO) {
                UpdateManager.getDownloadedUpdate(context)
            }
            withContext(Dispatchers.IO) {
                UpdateManager.getDownloadProgress(context, stored.tag)
            }?.let {
                if (!it.isFinished && !it.isFailed) {
                    progress = it
                    downloading = true
                }
            }
        }
    }

    LaunchedEffect(checkRequested) {
        if (!checkRequested) return@LaunchedEffect
        checkingForUpdate = true
        updateCheckFailed = false
        updateCheckError = null
        showNoUpdate = false
        try {
            val latest = withContext(Dispatchers.IO) {
                UpdateManager.findLatestUpdate(context)
            }
            if (latest != null) {
                UpdateManager.savePendingUpdate(context, latest)
                pendingUpdate = latest
                downloadedUpdate = withContext(Dispatchers.IO) {
                    UpdateManager.getDownloadedUpdate(context)
                }
            } else {
                showNoUpdate = true
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e("UpdateContent", "Update check failed", exception)
            updateCheckError = exception.message ?: exception.javaClass.simpleName
            updateCheckFailed = true
        } finally {
            checkingForUpdate = false
            onCheckRequestConsumed()
        }
    }

    LaunchedEffect(notificationRequested) {
        if (!notificationRequested) return@LaunchedEffect
        onNotificationRequestConsumed()
        val latest = pendingUpdate ?: withContext(Dispatchers.IO) {
            UpdateManager.getValidatedPendingUpdate(context)
        }
        if (latest != null) {
            pendingUpdate = latest
            downloadedUpdate = withContext(Dispatchers.IO) {
                UpdateManager.getDownloadedUpdate(context)
            }
        }
    }

    LaunchedEffect(pendingUpdate?.tag, downloading) {
        val update = pendingUpdate ?: return@LaunchedEffect
        if (!downloading) return@LaunchedEffect
        UpdateManager.observeDownloadProgress(context, update.tag).collect { status ->
            if (status == null) return@collect
            progress = status
            if (status.isFinished) {
                downloadedUpdate = withContext(Dispatchers.IO) {
                    UpdateManager.getDownloadedUpdate(context)
                }
                downloading = false
                progress = null
            } else if (status.isFailed) {
                downloading = false
                progress = null
                Toast.makeText(
                    context,
                    updateDownloadFailedMessage,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    pendingUpdate?.let { update ->
        if (downloading && progress != null) {
            DownloadProgressDialog(
                update = update,
                appIcon = appIcon,
                progress = progress!!,
                onCancel = {
                    UpdateManager.cancelDownload(context, update.tag)
                    downloading = false
                    progress = null
                },
            )
        } else {
            UpdateDialog(
                update = update,
                appIcon = appIcon,
                isDownloaded = downloadedUpdate?.tag == update.tag,
                isDownloading = downloading,
                onDownload = {
                    downloading = true
                    UpdateManager.enqueueDownload(context, update)
                },
                onInstall = {
                    when (installer.installDownloadedUpdate()) {
                        UpdateInstaller.Result.Success -> Unit
                        UpdateInstaller.Result.FileMissing ->
                            Toast.makeText(
                                context,
                                updateFileMissingMessage,
                                Toast.LENGTH_LONG
                            ).show()

                        UpdateInstaller.Result.PermissionRequired -> {
                            Toast.makeText(
                                context,
                                updatePermissionRequiredMessage,
                                Toast.LENGTH_LONG
                            ).show()
                            installer.openInstallPermissionSettings()
                        }

                        UpdateInstaller.Result.Failed ->
                            Toast.makeText(
                                context,
                                updateInstallFailedMessage,
                                Toast.LENGTH_LONG
                            ).show()
                    }
                },
                onDismiss = {
                    pendingUpdate = null
                    downloadedUpdate = null
                    downloading = false
                    progress = null
                },
            )
        }
    }

    if (checkingForUpdate) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.update_checking)) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                    Text(stringResource(R.string.update_checking_message))
                }
            },
            confirmButton = {},
        )
    }

    if (updateCheckFailed) {
        AlertDialog(
            onDismissRequest = {
                updateCheckFailed = false
                updateCheckError = null
            },
            title = { Text(stringResource(R.string.update_check_failed)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.update_check_failed_message))
                    Text(updateCheckError.orEmpty())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        updateCheckFailed = false
                        updateCheckError = null
                        onRequestCheck()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(stringResource(R.string.action_try_again))
                }
            },
        )
    }

    if (showNoUpdate) {
        Dialog(onDismissRequest = { showNoUpdate = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = "Sonora",
                        modifier = Modifier.size(64.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.update_up_to_date),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(
                                R.string.update_latest_message,
                                BuildConfig.VERSION_NAME
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { showNoUpdate = false },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(stringResource(R.string.action_ok))
                        }
                    }
                }
            }
        }
    }
}
