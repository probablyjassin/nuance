package com.jassin.customdrome.data.repository

import com.jassin.customdrome.UserPreferences
import com.jassin.customdrome.data.api.NavidromeApiClient
import com.jassin.customdrome.data.models.HomeLoadResult
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val userPrefs: UserPreferences,
    private val apiClient: NavidromeApiClient,
) {
    suspend fun checkLoginAndGetSongCount(): HomeLoadResult {
        val token = userPrefs.token.first()
        val serverUrl = userPrefs.serverURL.first()

        if (token.isNullOrBlank() || serverUrl.isNullOrBlank()) {
            return HomeLoadResult.NotLoggedIn
        }

        val loggedIn = apiClient.pingAuth(serverUrl, token)
        if (!loggedIn) {
            return HomeLoadResult.NotLoggedIn
        }

        val count = apiClient.fetchSongCount(serverUrl, token)
        return HomeLoadResult.LoggedIn(songCount = count)
    }
}
