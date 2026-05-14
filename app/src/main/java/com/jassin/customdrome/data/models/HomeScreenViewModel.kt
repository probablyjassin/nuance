package com.jassin.customdrome.data.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jassin.customdrome.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _homeState = MutableStateFlow<HomeLoadResult>(HomeLoadResult.Loading)
    val homeState: StateFlow<HomeLoadResult> = _homeState

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun loadHome() {
        viewModelScope.launch {
            _homeState.value = HomeLoadResult.Loading

            try {
                when (val result = authRepository.checkLoginAndGetSongCount()) {
                    is HomeLoadResult.LoggedIn -> {
                        _homeState.value = result
                        _toastMessage.value = "Logged in. You have ${result.songCount} songs."
                    }

                    HomeLoadResult.NotLoggedIn -> {
                        _homeState.value = HomeLoadResult.NotLoggedIn
                        _toastMessage.value = "Not logged in."
                    }

                    else -> {
                        _homeState.value = HomeLoadResult.NotLoggedIn
                    }
                }
            } catch (t: Throwable) {
                _homeState.value = HomeLoadResult.Error(t.message ?: "Unknown error")
                _toastMessage.value = "Failed to check login: ${t.message ?: "Unknown error"}"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
