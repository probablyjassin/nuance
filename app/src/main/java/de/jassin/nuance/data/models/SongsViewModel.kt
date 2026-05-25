package de.jassin.nuance.data.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.jassin.nuance.data.repository.SongsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SongsViewModel(
    private val songsRepository: SongsRepository,
) : ViewModel() {
    private val _songsState = MutableStateFlow<SongsUiState>(SongsUiState.Loading)
    val songsState: StateFlow<SongsUiState> = _songsState

    init {
        loadSongs()
    }

    fun loadSongs(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _songsState.value = SongsUiState.Loading

            try {
                val songs = songsRepository.loadSongs(forceRefresh)
                _songsState.value = SongsUiState.Ready(songs)
            } catch (t: Throwable) {
                _songsState.value = SongsUiState.Error(t.message ?: "Unknown error")
            }
        }
    }
}
