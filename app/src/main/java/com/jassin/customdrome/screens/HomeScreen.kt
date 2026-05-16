package com.jassin.customdrome.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.jassin.customdrome.data.local.SongCacheDatabase
import com.jassin.customdrome.data.models.HomeScreenViewModel
import com.jassin.customdrome.data.repository.HomeRepository

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    userPrefs: UserPreferences,
) {
    val context = LocalContext.current

    // ...existing code...
    val apiClient = remember { NavidromeApiClient() }
    val songCacheDatabase = remember { SongCacheDatabase(context.applicationContext) }
    val homeRepository = remember(userPrefs, songCacheDatabase) { HomeRepository(userPrefs, apiClient, songCacheDatabase) }

    val vm: HomeScreenViewModel =
        viewModel(
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeScreenViewModel(homeRepository) as T
                },
        )

    val songCount by vm.songCount.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val toastMessage by vm.toastMessage.collectAsState()

    LaunchedEffect(Unit) { vm.loadHome() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            isLoading -> {
                Text("Loading...")
            }

            songCount != null -> {
                Text("Logged in.")
                Text("Songs in your library: $songCount")
            }

            else -> {
                Text("Failed to load songs.")
            }
        }
    }
}
