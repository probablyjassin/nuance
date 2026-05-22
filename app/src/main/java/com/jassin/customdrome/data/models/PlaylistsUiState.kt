@file:Suppress("unused")

package com.jassin.customdrome.data.models

sealed class PlaylistsUiState {
    data object Loading : PlaylistsUiState()

    data class Ready(
        val playlists: List<PlaylistUiModel>,
    ) : PlaylistsUiState()

    data class Error(
        val message: String,
    ) : PlaylistsUiState()
}

