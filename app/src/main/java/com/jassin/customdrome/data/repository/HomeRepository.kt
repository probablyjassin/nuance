package com.jassin.customdrome.data.repository

import android.util.Log
import com.jassin.customdrome.UserPreferences
import com.jassin.customdrome.data.api.NavidromeApiClient
import com.jassin.customdrome.data.local.SongCacheDatabase
import com.jassin.customdrome.data.models.HomeLoadResult
import kotlinx.coroutines.flow.first

class HomeRepository(
    private val userPrefs: UserPreferences,
    private val apiClient: NavidromeApiClient,
    private val songCacheDatabase: SongCacheDatabase,
) {
    suspend fun loadHomeState(): HomeLoadResult {
        val token = userPrefs.token.first()
        val serverUrl = userPrefs.serverURL.first()

        if (token.isNullOrBlank() || serverUrl.isNullOrBlank()) {
            return HomeLoadResult.NotLoggedIn
        }

        if (!apiClient.pingAuth(serverUrl, token)) {
            return HomeLoadResult.NotLoggedIn
        }

        val songCount = loadSongCount(serverUrl, token)
        return HomeLoadResult.LoggedIn(songCount = songCount)
    }

    private suspend fun loadSongCount(
        serverUrl: String,
        token: String,
    ): Int {
        if (songCacheDatabase.isSongsCacheInitialized()) {
            val cachedCount = songCacheDatabase.getSongCount()
            Log.d("HomeRepository", "Using cached songs: $cachedCount songs")
            return cachedCount
        }

        Log.d("HomeRepository", "Fetching songs from server")
        val songs = apiClient.fetchSongs(serverUrl, token)
        songCacheDatabase.replaceAllSongs(songs)
        return songs.size
    }
}
