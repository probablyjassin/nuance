package com.jassin.customdrome

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(onGoToSettings: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("CustomDrome") },
        actions = {
            IconButton(onClick = { onGoToSettings() }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
    )
}