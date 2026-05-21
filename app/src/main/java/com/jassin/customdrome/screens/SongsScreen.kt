package com.jassin.customdrome.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jassin.customdrome.UserPreferences
import com.jassin.customdrome.data.api.NavidromeApiClient
import com.jassin.customdrome.data.local.CoverArtCache
import com.jassin.customdrome.data.local.SongCacheDatabase
import com.jassin.customdrome.data.models.SongsUiState
import com.jassin.customdrome.data.models.SongsViewModel
import com.jassin.customdrome.data.repository.SongsRepository
import com.jassin.customdrome.playback.PlaybackManager
import com.jassin.customdrome.playback.toPlaybackItem
import com.jassin.customdrome.ui.common.SingleSongDisplay

@androidx.compose.runtime.Composable
fun Songs(
    userPrefs: UserPreferences,
    listState: LazyListState,
    playbackManager: PlaybackManager,
) {
    val context = LocalContext.current
    val apiClient = remember { NavidromeApiClient() }
    val songCacheDatabase = remember { SongCacheDatabase(context.applicationContext) }
    val coverArtCache = remember { CoverArtCache(context.applicationContext) }
    val songsRepository =
        remember(userPrefs, songCacheDatabase, coverArtCache) {
            SongsRepository(userPrefs, apiClient, songCacheDatabase, coverArtCache)
        }

    val vm: SongsViewModel =
        viewModel(
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = SongsViewModel(songsRepository) as T
                },
        )

    val songsState by vm.songsState.collectAsState()
    val coverCache = remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }

    // Placeholder for future per-song options (currently unused)

    when (val state = songsState) {
        SongsUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading songs...")
            }
        }

        is SongsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Error: ${state.message}")
            }
        }

        is SongsUiState.Ready -> {
            if (state.songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No songs found")
                }
            } else {
                val songs = state.songs

                Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        AssistChip(
                            onClick = { /* TODO: wire up sort options */ },
                            label = { Text("Sort by") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }

                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(songs, key = { _, it -> it.id }) { index, song ->
                            val shape =
                                when {
                                    songs.size == 1 -> RoundedCornerShape(12.dp)
                                    index == 0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                    index == songs.lastIndex -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }

                            val bottomPadding = if (index == songs.lastIndex) 80.dp else 0.dp

                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = bottomPadding),
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 1.dp,
                            ) {
                                SingleSongDisplay(
                                    title = song.title,
                                    artist = song.artist,
                                    songId = song.id,
                                    songsRepository = songsRepository,
                                    onClick = {
                                        playbackManager.playQueue(
                                            queue = songs.map { it.toPlaybackItem() },
                                            startIndex = index,
                                        )
                                    },
                                    // onLongPress = { optionsSelectedSong = song },
                                    onCoverLoaded = { songId, coverBytes ->
                                        val current = coverCache.value
                                        if (!current.containsKey(songId)) {
                                            coverCache.value = current + (songId to coverBytes)
                                        }
                                    },
                                    cachedCover = coverCache.value[song.id],
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
