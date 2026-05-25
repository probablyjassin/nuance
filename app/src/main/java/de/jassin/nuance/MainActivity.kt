package de.jassin.nuance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Global Settings / Prefrences object
        val userPrefs =
            UserPreferences(applicationContext)
        setContent {
            _root_ide_package_.de.jassin.nuance.ui.theme.NuanceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    _root_ide_package_.de.jassin.nuance
                        .AppNavigation(userPrefs)
                }
            }
        }
    }
}
