package com.sipun.sonora.feature.settings

import com.sipun.sonora.ui.theme.accentGradientTint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R
import com.sipun.sonora.data.media.AndroidMusicRepository
import com.sipun.sonora.data.preferences.LibraryPreferences

@Composable
internal fun FolderManagementDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { LibraryPreferences(context) }
    val repository = remember(context) { AndroidMusicRepository(context) }

    var discoveredFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var excludedFolders by remember { mutableStateOf(preferences.excludedFolders()) }

    LaunchedEffect(Unit) {
        discoveredFolders = repository.getAvailableFolders()
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val path = it.path ?: it.toString()
            val cleanPath = path.substringAfterLast("primary:", "").ifBlank { path }
            if (cleanPath.isNotBlank()) {
                preferences.addExcludedFolder(cleanPath)
                excludedFolders = preferences.excludedFolders()
                AndroidMusicRepository.notifyMetadataChanged()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(stringResource(R.string.settings_excluded_folders))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (excludedFolders.isEmpty()) "No excluded folders" else "${excludedFolders.size} excluded folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { folderPickerLauncher.launch(null) }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp).accentGradientTint())
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_add_folder))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (discoveredFolders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.folder_no_discovered),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(discoveredFolders) { folder ->
                            val isExcluded = folder in excludedFolders
                            val folderName = folder.substringAfterLast('/').ifBlank { folder }
                            val displayPath = folder.removePrefix("/storage/emulated/0/").trimStart('/')

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp).accentGradientTint()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = folderName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = displayPath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = isExcluded,
                                        onCheckedChange = { checked ->
                                            if (checked) preferences.addExcludedFolder(folder)
                                            else preferences.removeExcludedFolder(folder)
                                            excludedFolders = preferences.excludedFolders()
                                            AndroidMusicRepository.notifyMetadataChanged()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.action_done))
            }
        }
    )
}
