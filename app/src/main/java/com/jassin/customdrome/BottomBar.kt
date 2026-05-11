package com.jassin.customdrome

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    object Home : Screen("home", "Home", Icons.Default.Home)

    object Playlists : Screen("playlists", "Playlists", Icons.Default.Album)

    // object Albums : Screen("albums", "Albums", Icons.Default.Home)

    // object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun BottomBar(navController: NavHostController) {
    val items =
        listOf(
            Screen.Home,
            Screen.Playlists,
            /*Screen.Albums,
            Screen.Settings,*/
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
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
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
