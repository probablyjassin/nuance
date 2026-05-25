@file:Suppress("unused")

package de.jassin.nuance.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import de.jassin.nuance.UserPreferences
import de.jassin.nuance.data.api.NavidromeApiClient
import de.jassin.nuance.data.local.PlaylistCacheDatabase
import de.jassin.nuance.data.models.PlaylistsUiState
import de.jassin.nuance.data.models.PlaylistsViewModel
import de.jassin.nuance.data.repository.PlaylistsRepository
import de.jassin.nuance.data.repository.SongsRepository
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(
    userPrefs: UserPreferences? = null,
    songsRepository: SongsRepository? = null,
) {
    val context = LocalContext.current
    val apiClient = remember { NavidromeApiClient() }
    val playlistCacheDatabase = remember { PlaylistCacheDatabase(context.applicationContext) }
    val playlistsRepository =
        remember(userPrefs, playlistCacheDatabase) {
            PlaylistsRepository(userPrefs!!, apiClient, playlistCacheDatabase)
        }

    val vm: PlaylistsViewModel =
        viewModel(
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = PlaylistsViewModel(playlistsRepository) as T
                },
        )

    val playlistsState by vm.playlistsState.collectAsState()
    val listState = rememberLazyListState()
    val coverCache = remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }

    when (val state = playlistsState) {
        PlaylistsUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading playlists...")
            }
        }

        is PlaylistsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Error: ${state.message}")
            }
        }

        is PlaylistsUiState.Ready -> {
            if (state.playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No playlists found")
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(state.playlists, key = { _, it -> it.id }) { index, playlist ->
                            val shape =
                                when {
                                    state.playlists.size == 1 -> RoundedCornerShape(12.dp)
                                    index == 0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                    index == state.playlists.lastIndex -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }

                            val bottomPadding = if (index == state.playlists.lastIndex) 80.dp else 0.dp
                            val firstTrackId = playlist.tracks.firstOrNull()

                            // Fetch cover art for the first track
                            LaunchedEffect(firstTrackId) {
                                if (firstTrackId != null && songsRepository != null && !coverCache.value.containsKey(firstTrackId)) {
                                    launch {
                                        val coverBytes = songsRepository.getCoverArtQueued(firstTrackId)
                                        if (coverBytes != null) {
                                            val current = coverCache.value
                                            coverCache.value = current + (firstTrackId to coverBytes)
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = bottomPadding),
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 1.dp,
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    // Cover art on the left
                                    if (firstTrackId != null && coverCache.value.containsKey(firstTrackId)) {
                                        val coverBytes = coverCache.value[firstTrackId]
                                        if (coverBytes != null) {
                                            val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Cover art for ${playlist.name}",
                                                    modifier =
                                                        Modifier
                                                            .size(80.dp)
                                                            .align(Alignment.Top),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            }
                                        }
                                    } else if (firstTrackId != null) {
                                        // Placeholder while loading
                                        Surface(
                                            modifier =
                                                Modifier
                                                    .size(80.dp)
                                                    .align(Alignment.Top),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp),
                                        ) {}
                                    }

                                    // Playlist info on the right
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                    ) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        if (!playlist.comment.isNullOrBlank()) {
                                            Text(
                                                text = playlist.comment,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = "${playlist.trackCount} tracks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}
