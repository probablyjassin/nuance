package de.jassin.nuance.data.models

sealed class SongsUiState {
    data object Loading : SongsUiState()

    data class Ready(
        val songs: List<SongUiModel>,
    ) : SongsUiState()

    data class Error(
        val message: String,
    ) : SongsUiState()
}
