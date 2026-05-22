@file:Suppress("unused")

package com.jassin.customdrome.data.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jassin.customdrome.data.repository.PlaylistsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    private val playlistsRepository: PlaylistsRepository,
) : ViewModel() {
    private companion object {
        const val TAG = "PlaylistsViewModel"
    }

    private val _playlistsState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val playlistsState: StateFlow<PlaylistsUiState> = _playlistsState

    init {
        Log.d(TAG, "PlaylistsViewModel created")
        loadPlaylists()
    }

    fun loadPlaylists(forceRefresh: Boolean = false) {
        Log.d(TAG, "loadPlaylists called with forceRefresh=$forceRefresh")
        viewModelScope.launch {
            _playlistsState.value = PlaylistsUiState.Loading

            try {
                Log.d(TAG, "Calling playlistsRepository.loadPlaylists()")
                val playlists = playlistsRepository.loadPlaylists(forceRefresh)
                Log.d(TAG, "playlistsRepository.loadPlaylists() returned ${playlists.size} playlists")
                playlists.forEach { playlist ->
                    Log.d(TAG, "  Playlist: ${playlist.name} - ${playlist.trackCount} tracks")
                }
                _playlistsState.value = PlaylistsUiState.Ready(playlists)
            } catch (t: Throwable) {
                Log.e(TAG, "Error loading playlists", t)
                _playlistsState.value = PlaylistsUiState.Error(t.message ?: "Unknown error")
            }
        }
    }
}

