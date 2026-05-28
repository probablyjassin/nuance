package de.jassin.nuance.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import de.jassin.nuance.UserPreferences
import de.jassin.nuance.data.api.NavidromeApiClient
import kotlinx.coroutines.flow.first

// this is the first screen the user lands at when opening the app
// because we need to check whether 1. they have a login token
// and 2. whether it's still valid
// then navigate to either home or login
@Composable
fun SplashScreen(
    navController: NavHostController,
    userPrefs: UserPreferences,
    apiClient: NavidromeApiClient,
) {
    val context = LocalContext.current
    val secureHostnames by userPrefs.server.secureHostnames.collectAsState(initial = null)

    LaunchedEffect(secureHostnames) {
        if (secureHostnames == null) return@LaunchedEffect
        try {
            logBackStack(navController, "SplashScreen entry")

            val token = userPrefs.auth.token.first()
            val serverUrl = userPrefs.server.serverURL.first()

            if (token.isNullOrBlank() || serverUrl.isNullOrBlank()) {
                Log.d("SplashScreen", "No credentials saved (token or serverUrl blank)")
                // Inform the user why they are being routed to login
                Toast.makeText(context, "No credentials saved", Toast.LENGTH_LONG).show()
                logBackStack(navController, "Before navigate to login (no creds)")
                // No credentials saved -> go to login and remove splash
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
                logBackStack(navController, "After navigate to login (no creds)")
                return@LaunchedEffect
            }

            Log.d("SplashScreen", "Token and URL found, pinging auth: url=$serverUrl")
            val ok =
                try {
                    apiClient.pingAuth(serverUrl, token)
                } catch (e: Exception) {
                    Log.w("SplashScreen", "pingAuth failed", e)
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    false
                }

            if (ok) {
                Log.d("SplashScreen", "Auth ping successful, navigating to home")
                logBackStack(navController, "Before navigate to home (auth OK)")
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
                logBackStack(navController, "After navigate to home (auth OK)")
            } else {
                // token invalid -> clear or prompt login
                Log.d("SplashScreen", "Auth ping failed (invalid token), navigating to login")
                // Inform the user why they are being routed to login
                Toast.makeText(context, "Invalid or expired token — please log in again", Toast.LENGTH_LONG).show()
                logBackStack(navController, "Before navigate to login (auth failed)")
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
                logBackStack(navController, "After navigate to login (auth failed)")
            }
        } catch (t: Throwable) {
            Log.e("SplashScreen", "unexpected error during auth check", t)
            logBackStack(navController, "Before navigate to login (exception)")
            // fallback to login on any unexpected error
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
            logBackStack(navController, "After navigate to login (exception)")
        }
    }

    // screen content (only briefly visible)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Helper function to log the NavController state.
 * Useful for debugging navigation state issues.
 */
fun logBackStack(
    navController: NavHostController,
    label: String,
) {
    val current = navController.currentDestination?.route ?: "unknown"
    val previous = navController.previousBackStackEntry?.destination?.route ?: "none"
    Log.d("SplashScreen", "[$label] Current: $current | Previous: $previous")
}
