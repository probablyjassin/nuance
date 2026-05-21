package com.jassin.customdrome.data.repository

import android.util.Log
import com.jassin.customdrome.UserPreferences
import com.jassin.customdrome.data.api.NavidromeApiClient
import com.jassin.customdrome.data.local.SongCacheDatabase
import kotlinx.coroutines.flow.first

class HomeRepository(
    private val userPrefs: UserPreferences,
    private val apiClient: NavidromeApiClient,
    private val songCacheDatabase: SongCacheDatabase,
) {
    suspend fun loadSongCount(): Int {
        val token = userPrefs.auth.token.first()
        val serverUrl = userPrefs.server.serverURL.first()

        // Auth already validated by SplashScreen, just load song count
        val nonNullToken = token ?: throw IllegalStateException("Auth should have been validated by SplashScreen")
        val nonNullUrl = serverUrl ?: throw IllegalStateException("Auth should have been validated by SplashScreen")

        if (nonNullToken.isBlank() || nonNullUrl.isBlank()) {
            throw IllegalStateException("Auth should have been validated by SplashScreen")
        }

        return loadSongCountInternal(nonNullUrl, nonNullToken)
    }

    private suspend fun loadSongCountInternal(
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
