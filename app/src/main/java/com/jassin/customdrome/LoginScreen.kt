package com.jassin.customdrome

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
// Important: Import the Material3 version of components for Dynamic Color
import androidx.compose.material3.MaterialTheme
import com.jassin.customdrome.ui.theme.CustomDromeTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.safeDrawingPadding

@Composable
fun LoginScreen(onBack: () -> Unit) {
    CustomDromeTheme {
        // responsible for the themed bg color
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // prevents overlap with the status bar
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Login to CustomDrome",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Button(onClick = { onBack() }) {
                        // Trigger the back action
                        Text("Go Back")
                    }

                    Button(onClick = { /* logic goes here */ }) {
                        Text("Login")
                    }
                }
            }
        }
    }
}
