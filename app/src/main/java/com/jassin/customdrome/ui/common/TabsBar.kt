package com.jassin.customdrome.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    object Home : Screen("home", "Home", Icons.Default.Home)

    object Songs : Screen("songs", "Songs", Icons.Default.MusicNote)

    object Playlists : Screen("playlists", "Playlists", Icons.Default.Album)

    // object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun TabsBar(navController: NavHostController) {
    val items =
        listOf(
            Screen.Home,
            Screen.Songs,
            Screen.Playlists,
            // Screen.Settings,
        )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (screen.route == currentRoute) {
                        // Already on this screen
                        // scroll to top
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("scroll_to_top", System.currentTimeMillis())

                        return@NavigationBarItem
                    }

                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
fun MusicAppScreen(navController: NavHostController) {
    // Hardcoded: is a song playing?
    var isSongPlaying by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f)) // placeholder for main content
        }

        // Bottom mini player + bottom bar
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter),
        ) {
            // Mini-player bar (shows when a song is playing)
            AnimatedVisibility(
                visible = isSongPlaying,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
            ) {
                MiniPlayerBar(
                    isExpanded = isExpanded,
                    onExpandToggle = { isExpanded = !isExpanded },
                )
            }

            // Bottom navigation bar (hide when mini-player is expanded)
            AnimatedVisibility(
                visible = !isExpanded,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
            ) {
                TabsBar(navController)
            }
        }

        // Fullscreen player overlay
        if (isExpanded) {
            FullscreenPlayer(
                onMinimize = { isExpanded = false },
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.DarkGray)
                .clickable { onExpandToggle() }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Dummy Song - Artist",
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(text = "▶", color = Color.White) // Play/Pause icon placeholder
    }
}

@Composable
fun FullscreenPlayer(onMinimize: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onMinimize() },
    ) {
        Text(
            text = "Fullscreen Player - Dummy Song",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
