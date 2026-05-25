package de.jassin.nuance.data.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.jassin.nuance.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    private val homeRepository: HomeRepository,
) : ViewModel() {
    private val _songCount = MutableStateFlow<Int?>(null)
    val songCount: StateFlow<Int?> = _songCount

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun loadHome() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val count = homeRepository.loadSongCount()
                _songCount.value = count
                _toastMessage.value = "Logged in. You have $count songs."
            } catch (t: Throwable) {
                _toastMessage.value = "Failed to load: ${t.message ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
