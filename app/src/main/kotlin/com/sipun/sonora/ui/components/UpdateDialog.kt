package com.sipun.sonora.ui.components

import com.sipun.sonora.ui.theme.accentGradientTint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipun.sonora.core.update.AppUpdate
import com.sipun.sonora.core.update.DownloadProgress

@Composable
fun UpdateDialog(
    update: AppUpdate,
    appIcon: ImageBitmap,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onDownload: () -> Unit,
    onInstall: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                bitmap = appIcon,
                contentDescription = "Sonora",
                modifier = Modifier.size(62.dp),
            )
        },
        title = {
            Text(
                if (isDownloaded) "Update ready" else "New Sonora update",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text(
                    "Sonora " + update.version + " is available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    ),
                ) {
                    Column(
                        Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UpdateInfoRow(Icons.Default.NewReleases, "Version", update.version)
                        UpdateInfoRow(Icons.Default.Inventory2, "Download", formatSize(update.size))
                        update.releaseDate?.let {
                            UpdateInfoRow(Icons.Default.NewReleases, "Released", it)
                        }
                    }
                }
                if (update.notes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What's new", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 132.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            update.notes.lineSequence()
                                .filter { it.isNotBlank() }
                                .forEach { note ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "•",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            note,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Later")
                }
                Button(
                    onClick = if (isDownloaded) onInstall else onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(17.dp).accentGradientTint())
                    Spacer(Modifier.size(6.dp))
                    Text(
                        when {
                            isDownloaded -> "Install"
                            isDownloading -> "Downloading…"
                            else -> "Download"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    )
}

@Composable
fun DownloadProgressDialog(
    update: AppUpdate,
    appIcon: ImageBitmap,
    progress: DownloadProgress,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Image(
                bitmap = appIcon,
                contentDescription = "Sonora",
                modifier = Modifier.size(62.dp),
            )
        },
        title = {
            Text("Downloading update", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Sonora " + update.version,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                LinearProgressIndicator(
                    progress = { (progress.percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        formatSize(progress.downloadedBytes) + " / " +
                                formatSize(if (progress.totalBytes > 0L) progress.totalBytes else update.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    Text(
                        progress.percent.toString() + "%",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    )
}

@Composable
private fun UpdateInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).accentGradientTint())
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes < 1024L * 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
