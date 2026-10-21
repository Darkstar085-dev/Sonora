package com.sipun.sonora.feature.home

import com.sipun.sonora.ui.theme.accentGradientTint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sipun.sonora.R

@Composable
internal fun EmptyLibraryState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.MusicOff,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp).accentGradientTint()
        )
        Text(
            stringResource(R.string.home_no_music),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            stringResource(R.string.home_add_music),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun EmptySearchState(query: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.SearchOff,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp).accentGradientTint()
        )
        Text(
            stringResource(R.string.home_no_music),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            stringResource(R.string.home_no_search_results, query),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun songCountLabel(count: Int): String =
    if (count == 1) {
        stringResource(R.string.song_count_one)
    } else {
        stringResource(R.string.song_count_other, count)
    }
