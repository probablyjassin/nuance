package com.jassin.customdrome

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jassin.customdrome.screens.HomeScreen
import com.jassin.customdrome.screens.SplashScreen
import com.jassin.customdrome.screens.LoginScreen
import com.jassin.customdrome.screens.Playlists
import com.jassin.customdrome.screens.SettingsScreen
import com.jassin.customdrome.screens.Songs
import com.jassin.customdrome.ui.features.PlayerScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(userPrefs: UserPreferences) {
    val navController = rememberNavController()

    // keep track of current route
    // conditionally show nav elements
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun showNavElements(): Boolean = currentRoute != "login" && currentRoute != "settings"

    PlayerScaffold(navController = navController, showNavElements()) { paddingValues ->
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

            composable(route = "songs") { Songs(userPrefs) }

            composable(route = "playlists") { Playlists() }
        }
    }
}
