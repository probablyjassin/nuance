package de.jassin.nuance.screens

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
import de.jassin.nuance.UserPreferences
import de.jassin.nuance.data.api.NavidromeApiClient
import de.jassin.nuance.data.local.SongCacheDatabase
import de.jassin.nuance.data.models.HomeScreenViewModel
import de.jassin.nuance.data.repository.HomeRepository

@Composable
fun HomeScreen(
    userPrefs: UserPreferences,
    apiClient: NavidromeApiClient,
) {
    val context = LocalContext.current

    val songCacheDatabase = remember { SongCacheDatabase(context.applicationContext) }
    val homeRepository = remember(userPrefs, apiClient, songCacheDatabase) { HomeRepository(userPrefs, apiClient, songCacheDatabase) }

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
