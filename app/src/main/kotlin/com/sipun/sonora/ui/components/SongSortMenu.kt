package com.sipun.sonora.ui.components

import com.sipun.sonora.ui.theme.accentGradientTint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sipun.sonora.domain.model.Song
import com.sipun.sonora.feature.sorting.SongSortOption
import com.sipun.sonora.feature.sorting.SongSortScope
import com.sipun.sonora.player.PlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSortButton(
    scope: SongSortScope,
    currentOption: SongSortOption,
    onOptionSelected: (SongSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(scope) { mutableStateOf(false) }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort songs", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.accentGradientTint())
        }
        if (scope == SongSortScope.SONGS) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(252.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                ) {
                    Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                        SortMenuHeading()
                        SongSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                modifier = Modifier
                                    .height(44.dp)
                                    .padding(vertical = 1.dp)
                                    .background(
                                        color = if (option == currentOption) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                text = {
                                    Text(
                                        option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                leadingIcon = {
                                    SortOptionIcon(option, option == currentOption, modifier = Modifier.accentGradientTint())
                                },
                                trailingIcon = {
                                    if (option == currentOption) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp).accentGradientTint(),
                                        )
                                    }
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                onClick = {
                                    onOptionSelected(option)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        } else if (expanded) {
            ModalBottomSheet(
                onDismissRequest = { expanded = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SortMenuHeading()
                    SongSortOption.entries.forEach { option ->
                        val selected = option == currentOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable {
                                    onOptionSelected(option)
                                    expanded = false
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SortOptionIcon(option, selected, modifier = Modifier.accentGradientTint())
                            Text(
                                option.label,
                                modifier = Modifier.weight(1f).padding(start = 14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp).accentGradientTint(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenuHeading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Sort,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).accentGradientTint(),
        )
        Text(
            "Sort by",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun SortOptionIcon(option: SongSortOption, selected: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = when (option) {
            SongSortOption.TITLE_ASC -> Icons.Default.ArrowUpward
            SongSortOption.TITLE_DESC -> Icons.Default.ArrowDownward
            SongSortOption.ARTIST_ASC, SongSortOption.ARTIST_DESC -> Icons.Default.Person
            SongSortOption.ALBUM_ASC, SongSortOption.ALBUM_DESC -> Icons.Default.Album
            SongSortOption.RECENTLY_ADDED -> Icons.Default.AccessTime
            SongSortOption.RECENTLY_PLAYED -> Icons.Default.History
        },
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp).accentGradientTint(),
    )
}

@Composable
fun SongCollectionControls(
    songs: List<Song>,
    playerController: PlayerController,
    onOpenNowPlaying: () -> Unit,
    scope: SongSortScope,
    currentOption: SongSortOption,
    onOptionSelected: (SongSortOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { playerController.playQueueShuffled(songs); onOpenNowPlaying() }) {
            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.accentGradientTint())
        }
        FilledIconButton(
            onClick = { playerController.playQueue(songs); onOpenNowPlaying() },
            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(28.dp),
                tint = Color.White,
            )
        }
        SongSortButton(scope, currentOption, onOptionSelected)
    }
}
