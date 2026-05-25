package de.jassin.nuance.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.jassin.nuance.UserPreferences
import de.jassin.nuance.data.api.NavidromeApiClient
import de.jassin.nuance.data.local.CoverArtCache
import de.jassin.nuance.data.local.SongCacheDatabase
import de.jassin.nuance.data.models.SongsUiState
import de.jassin.nuance.data.models.SongsViewModel
import de.jassin.nuance.data.repository.SongsRepository
import de.jassin.nuance.playback.PlaybackManager
import de.jassin.nuance.playback.toPlaybackItem
import de.jassin.nuance.ui.common.SingleSongDisplay
import kotlinx.coroutines.launch

@androidx.compose.runtime.Composable
fun SongsScreen(
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
            val scope = rememberCoroutineScope()

            if (state.songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No songs found")
                }
            } else {
                val sortOrder by userPrefs.sorting.songSortOrder.collectAsState(initial = 1)
                val songs = if (sortOrder == 1) state.songs.reversed() else state.songs

                Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = {
                            scope.launch {
                                val newSortOrder = sortOrder xor 1
                                userPrefs.sorting.saveSongSortOrder(newSortOrder)
                            }
                        }) {
                            val rotationAngle by animateFloatAsState(
                                targetValue = if (sortOrder == 1) 0f else 180f,
                                label = "Arrow Rotation", // label just for debugging
                            )

                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Order",
                                modifier = Modifier.rotate(rotationAngle),
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { /* TODO */ }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Shuffle")
                            }
                            val sortMenuExpanded = remember { mutableStateOf(false) }
                            Box {
                                AssistChip(
                                    onClick = { sortMenuExpanded.value = true },
                                    label = { Text("Sort by") },
                                    colors =
                                        AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                )
                                DropdownMenu(
                                    expanded = sortMenuExpanded.value,
                                    onDismissRequest = { sortMenuExpanded.value = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Title") },
                                        onClick = {
                                            sortMenuExpanded.value = false
                                            // TODO: implement title sort
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Artist") },
                                        onClick = {
                                            sortMenuExpanded.value = false
                                            // TODO: implement artist sort
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Album") },
                                        onClick = {
                                            sortMenuExpanded.value = false
                                            // TODO: implement album sort
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duration") },
                                        onClick = {
                                            sortMenuExpanded.value = false
                                            // TODO: implement duration sort
                                        },
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = true,
                        enter =
                            slideInVertically(
                                initialOffsetY = { if (sortOrder == 1) -it else it },
                            ),
                        exit =
                            slideOutVertically(
                                targetOffsetY = { if (sortOrder == 1) it else -it },
                            ),
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth().weight(1f).animateContentSize(),
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

                                val playbackState by playbackManager.state.collectAsStateWithLifecycle()
                                val currentSong = playbackState.currentItem

                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = bottomPadding),
                                    shape = shape,
                                    color = (
                                        if (currentSong?.id ==
                                            song.id
                                        ) {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                                    ),
                                    tonalElevation = 1.dp,
                                ) {
                                    SingleSongDisplay(
                                        title = song.title,
                                        artist = song.artist,
                                        songId = song.id,
                                        songsRepository = songsRepository,
                                        onClick = {
                                            // Only enqueue 25 songs around the selected to avoid
                                            // putting the entire library into the playback queue.
                                            val surrounding = 25
                                            val windowStart = (index - surrounding).coerceAtLeast(0)
                                            val windowEnd = (index + surrounding).coerceAtMost(songs.lastIndex)
                                            val window = songs.subList(windowStart, windowEnd + 1).map { it.toPlaybackItem() }

                                            playbackManager.playQueue(
                                                queue = window,
                                                // Adjust start index to the position within the window
                                                startIndex = index - windowStart,
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
}
