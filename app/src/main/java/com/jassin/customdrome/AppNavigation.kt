package com.jassin.customdrome

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jassin.customdrome.data.api.NavidromeApiClient
import com.jassin.customdrome.data.local.CoverArtCache
import com.jassin.customdrome.data.local.PlaylistCacheDatabase
import com.jassin.customdrome.data.local.SongCacheDatabase
import com.jassin.customdrome.data.repository.PlaylistsRepository
import com.jassin.customdrome.data.repository.SongsRepository
import com.jassin.customdrome.playback.PlaybackManager
import com.jassin.customdrome.screens.ArtistsScreen
import com.jassin.customdrome.screens.HomeScreen
import com.jassin.customdrome.screens.LoginScreen
import com.jassin.customdrome.screens.PlaylistsScreen
import com.jassin.customdrome.screens.SettingsScreen
import com.jassin.customdrome.screens.SongsScreen
import com.jassin.customdrome.screens.SplashScreen
import com.jassin.customdrome.ui.features.PlayerScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(userPrefs: UserPreferences) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val apiClient = remember { NavidromeApiClient() }
    val songCacheDatabase = remember { SongCacheDatabase(context.applicationContext) }
    val playlistCacheDatabase = remember { PlaylistCacheDatabase(context.applicationContext) }
    val coverArtCache = remember { CoverArtCache(context.applicationContext) }
    val songsRepository =
        remember(userPrefs, songCacheDatabase, coverArtCache) {
            SongsRepository(userPrefs, apiClient, songCacheDatabase, coverArtCache)
        }

    val playbackManager =
        remember(context.applicationContext, songsRepository) {
            PlaybackManager(
                context = context.applicationContext,
                coverFetcher = { id -> songsRepository.getCoverArtQueued(id) },
                streamUrlResolver = { id -> songsRepository.getStreamUrlQueued(id) },
            )
        }

    val playlistsRepository =
        remember(userPrefs, playlistCacheDatabase) {
            PlaylistsRepository(userPrefs, apiClient, playlistCacheDatabase)
        }

    // keep track of current route
    // conditionally show nav elements
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun showNavElements(): Boolean = currentRoute != "login" && currentRoute != "settings"

    PlayerScaffold(
        navController = navController,
        showNavBars = showNavElements(),
        playbackManager = playbackManager,
        songsRepository = songsRepository,
        playlistsRepository = playlistsRepository,
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(paddingValues),
        ) {
            composable("splash") {
                SplashScreen(navController = navController, userPrefs = userPrefs)
            }

            composable("home") {
                HomeScreen(
                    onNavigateToLogin = {
                        navController.navigate("login")
                    },
                    userPrefs = userPrefs,
                )
            }

            composable("login") {
                LoginScreen(
                    onLogin = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    userPrefs = userPrefs,
                )
            }

            composable("settings") {
                SettingsScreen(
                    onGoToLogin = {
                        navController.navigate("login")
                    },
                    userPrefs = userPrefs,
                )
            }

            composable("songs") { backStackEntry ->
                val listState = rememberLazyListState()
                val scrollToTopSignal by backStackEntry
                    .savedStateHandle
                    .getStateFlow("scroll_to_top", 0L)
                    .collectAsState()

                LaunchedEffect(scrollToTopSignal) {
                    if (scrollToTopSignal != 0L) {
                        listState.animateScrollToItem(0)
                    }
                }

                SongsScreen(
                    userPrefs = userPrefs,
                    listState = listState,
                    playbackManager = playbackManager,
                )
            }

            composable(route = "playlists") { PlaylistsScreen(userPrefs = userPrefs, songsRepository = songsRepository) }
            composable(route = "artists") { ArtistsScreen() }
        }
    }
}
