package com.jassin.customdrome.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Playlists() {
    Box(
        modifier = Modifier.Companion.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Companion.Center,
    ) {
        Box(Modifier.Companion.fillMaxSize(), contentAlignment = Alignment.Companion.Center) {
            Text("Playlists Screen")
        }
    }
}
