package com.jassin.customdrome.screens

import android.widget.Toast
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
import com.jassin.customdrome.data.models.HomeLoadResult
import com.jassin.customdrome.data.models.HomeScreenViewModel
import com.jassin.customdrome.data.repository.HomeRepository
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    userPrefs: UserPreferences,
) {
    val context = LocalContext.current

    // Build small dependency graph here; memoize to avoid re-creating on recomposition
    val apiClient = remember { NavidromeApiClient() }
    val songCacheDatabase = remember { SongCacheDatabase(context.applicationContext) }
    val homeRepository = remember(userPrefs, songCacheDatabase) { HomeRepository(userPrefs, apiClient, songCacheDatabase) }

    // Inline factory so we can use viewModel() and keep lifecycle integration
    val vm: HomeScreenViewModel =
        viewModel(
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeScreenViewModel(homeRepository) as T
                },
        )

    val homeState by vm.homeState.collectAsState()
    val toastMessage by vm.toastMessage.collectAsState()

    LaunchedEffect(Unit) { vm.loadHome() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            delay(1500)
            vm.clearToast()
        }
    }

    LaunchedEffect(homeState) {
        if (homeState is HomeLoadResult.NotLoggedIn) {
            onNavigateToLogin()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = homeState) {
            HomeLoadResult.Loading -> {
                Text("Checking login status...")
            }

            HomeLoadResult.NotLoggedIn -> {
                Text("Not logged in.")
            }

            is HomeLoadResult.LoggedIn -> {
                Text("Logged in.")
                Text("Songs in your library: ${state.songCount}")
            }

            is HomeLoadResult.Error -> {
                Text("Error: ${state.message}")
            }
        }
    }
}
