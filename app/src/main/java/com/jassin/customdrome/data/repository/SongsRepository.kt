@file:Suppress("unused")

package com.jassin.customdrome.data.repository

import android.util.Log
import com.jassin.customdrome.UserPreferences
import com.jassin.customdrome.data.api.NavidromeApiClient
import com.jassin.customdrome.data.local.CoverArtCache
import com.jassin.customdrome.data.local.SongCacheDatabase
import com.jassin.customdrome.data.models.SongUiModel
import com.jassin.customdrome.data.models.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private class LruCoverCache(
    private val maxSize: Int,
) : LinkedHashMap<String, ByteArray>(maxSize + 1, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean = size > maxSize
}

class SongsRepository(
    private val userPrefs: UserPreferences,
    private val apiClient: NavidromeApiClient,
    private val songCacheDatabase: SongCacheDatabase,
    private val coverArtCache: CoverArtCache,
) {
    private companion object {
        const val TAG = "SongsRepository"
    }

    private val coverFetchMutex = Mutex()
    private val memoryCache = LruCoverCache(maxSize = 100)
    private val memoryCacheLock = Mutex()

    suspend fun loadSongs(forceRefresh: Boolean = false): List<SongUiModel> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && songCacheDatabase.isSongsCacheInitialized()) {
                val cachedCount = songCacheDatabase.getSongCount()
                Log.d("SongsRepository", "Using cached songs: $cachedCount songs")
                return@withContext songCacheDatabase.getAllSongs().map { it.toUiModel() }
            }

            refreshSongsInternal()
        }

    suspend fun getCoverArt(songId: String): ByteArray? =
        withContext(Dispatchers.IO) {
            fetchCoverArtInternal(songId)
        }

    suspend fun getCoverArtQueued(songId: String): ByteArray? =
        coverFetchMutex.withLock {
            getCoverArt(songId)
        }

    suspend fun getStreamUrlQueued(songId: String): String? =
        coverFetchMutex.withLock {
            Log.d(TAG, "Resolving stream URL (queued) for songId=$songId")
            getStreamUrl(songId)
        }

    private suspend fun refreshSongsInternal(): List<SongUiModel> {
        val serverUrl = userPrefs.serverURL.first()
        val token = userPrefs.token.first()

        if (serverUrl.isNullOrBlank() || token.isNullOrBlank()) {
            Log.d("SongsRepository", "Cannot fetch songs: missing server URL or token")
            return emptyList()
        }

        Log.d("SongsRepository", "Fetching songs from server")
        val songs = apiClient.fetchSongs(serverUrl, token)
        songCacheDatabase.replaceAllSongs(songs)
        return songCacheDatabase.getAllSongs().map { it.toUiModel() }
    }

    private suspend fun fetchCoverArtInternal(songId: String): ByteArray? {
        // Check memory cache first (fastest)
        memoryCacheLock.withLock {
            memoryCache[songId]?.let {
                Log.d("SongsRepository", "memory cache hit for songId=$songId (${it.size} bytes)")
                return it
            }
        }

        // Check disk cache second (fast)
        coverArtCache.getCachedCoverArt(songId)?.let {
            Log.d("SongsRepository", "disk cache hit for songId=$songId (${it.size} bytes)")
            // Add to memory cache
            memoryCacheLock.withLock {
                memoryCache[songId] = it
            }
            return it
        }

        val serverUrl = userPrefs.serverURL.first()
        val username = userPrefs.userName.first()
        val subsonicToken = userPrefs.subsonicToken.first()
        val subsonicSalt = userPrefs.subsonicSalt.first()

        Log.d("SongsRepository", "fetchCoverArtInternal: songId=$songId serverUrl=${serverUrl?.take(60)} username=$username")

        if (
            serverUrl.isNullOrBlank() ||
            username.isNullOrBlank() ||
            subsonicToken.isNullOrBlank() ||
            subsonicSalt.isNullOrBlank()
        ) {
            Log.d("SongsRepository", "Cannot fetch cover art for $songId: missing credentials")
            return null
        }

        val result =
            apiClient.fetchCoverArt(
                serverUrl = serverUrl,
                username = username,
                subsonicToken = subsonicToken,
                subsonicSalt = subsonicSalt,
                songId = songId,
            )
        if (result == null) {
            Log.d("SongsRepository", "fetchCoverArtInternal: got null for songId=$songId")
        } else {
            Log.d("SongsRepository", "fetchCoverArtInternal: fetched ${result.size} bytes for songId=$songId")
            // Save to both memory and disk cache
            memoryCacheLock.withLock {
                memoryCache[songId] = result
            }
            coverArtCache.saveCoverArt(songId, result)
        }
        return result
    }

    private suspend fun getStreamUrl(songId: String): String? {
        val serverUrl = userPrefs.serverURL.first()
        val username = userPrefs.userName.first()
        val subsonicToken = userPrefs.subsonicToken.first()
        val subsonicSalt = userPrefs.subsonicSalt.first()

        if (
            serverUrl.isNullOrBlank() ||
            username.isNullOrBlank() ||
            subsonicToken.isNullOrBlank() ||
            subsonicSalt.isNullOrBlank()
        ) {
            Log.d(TAG, "Cannot resolve stream URL for $songId: missing credentials")
            return null
        }

        val resolvedUrl =
            apiClient.resolveStreamUrl(
            serverUrl = serverUrl,
            username = username,
            subsonicToken = subsonicToken,
            subsonicSalt = subsonicSalt,
            songId = songId,
        )
        Log.d(TAG, "Resolved stream URL for songId=$songId")
        return resolvedUrl
    }
}
