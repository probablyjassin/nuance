@file:Suppress("unused")

package de.jassin.nuance.data.repository

import android.util.Log
import de.jassin.nuance.UserPreferences
import de.jassin.nuance.data.api.NavidromeApiClient
import de.jassin.nuance.data.local.PlaylistCacheDatabase
import de.jassin.nuance.data.models.PlaylistUiModel
import de.jassin.nuance.data.models.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.collections.filter

class PlaylistsRepository(
    private val userPrefs: UserPreferences,
    private val apiClient: NavidromeApiClient,
    private val playlistCacheDatabase: PlaylistCacheDatabase,
) {
    private companion object {
        const val TAG = "PlaylistsRepository"
    }

    suspend fun loadPlaylists(forceRefresh: Boolean = false): List<PlaylistUiModel> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "=== loadPlaylists called with forceRefresh=$forceRefresh ===")
            val isCacheInit = playlistCacheDatabase.isPlaylistsCacheInitialized()
            Log.d(TAG, "loadPlaylists: cache initialized=$isCacheInit")

            if (!forceRefresh && isCacheInit) {
                val cachedCount = playlistCacheDatabase.getPlaylistCount()
                Log.d(TAG, "Using cached playlists: $cachedCount playlists")
                val result = getPlaylistsFromCache()

                // Check if all playlists have tracks
                val playlistsWithoutTracks = result.filter { it.tracks.isEmpty() }
                if (playlistsWithoutTracks.isNotEmpty()) {
                    Log.d(TAG, "WARNING: ${playlistsWithoutTracks.size} playlists have no tracks in cache, forcing refresh")
                    return@withContext refreshPlaylistsInternal()
                }

                return@withContext result
            }

            Log.d(TAG, "Cache not initialized or forceRefresh=true, calling refreshPlaylistsInternal()")
            refreshPlaylistsInternal()
        }

    private suspend fun refreshPlaylistsInternal(): List<PlaylistUiModel> {
        Log.d(TAG, "=== refreshPlaylistsInternal STARTED ===")
        val serverUrl = userPrefs.server.serverURL.first()
        val token = userPrefs.auth.token.first()

        Log.d(
            TAG,
            "refreshPlaylistsInternal: serverUrl=$serverUrl (null=${serverUrl.isNullOrBlank()}), token=${token?.take(
                20,
            )}... (null=${token.isNullOrBlank()})",
        )

        if (serverUrl.isNullOrBlank() || token.isNullOrBlank()) {
            Log.d(TAG, "Cannot fetch playlists: missing server URL or token")
            return emptyList()
        }

        Log.d(TAG, "Fetching playlists from server")
        val playlists = apiClient.fetchPlaylists(serverUrl, token)
        Log.d(TAG, "refreshPlaylistsInternal: fetched ${playlists.size} playlists from API")

        // Save playlists to cache (but don't mark as initialized yet)
        playlistCacheDatabase.replaceAllPlaylists(playlists)

        // For each playlist, fetch its tracks
        val playlistsWithTracks = mutableListOf<PlaylistUiModel>()
        for (playlist in playlists) {
            val playlistId = playlist.id ?: continue
            Log.d(TAG, "refreshPlaylistsInternal: fetching tracks for playlist $playlistId")
            val tracks = apiClient.fetchPlaylistTracks(serverUrl, token, playlistId)
            Log.d(TAG, "refreshPlaylistsInternal: API returned ${tracks.size} tracks for playlist $playlistId")
            val trackIds = tracks.mapNotNull { it.mediaFileId ?: it.id }
            Log.d(TAG, "refreshPlaylistsInternal: extracted ${trackIds.size} track IDs for playlist $playlistId")

            // Cache the track IDs for this playlist
            playlistCacheDatabase.replacePlaylistTracks(playlistId, trackIds)

            playlistsWithTracks.add(playlist.toUiModel(trackIds))
            Log.d(TAG, "Loaded playlist $playlistId with ${trackIds.size} tracks")
        }

        // Now mark the cache as fully initialized
        playlistCacheDatabase.markCacheAsInitialized()
        Log.d(TAG, "Cache marked as initialized with ${playlistsWithTracks.size} playlists")
        Log.d(TAG, "=== refreshPlaylistsInternal COMPLETED ===")

        return playlistsWithTracks
    }

    private fun getPlaylistsFromCache(): List<PlaylistUiModel> {
        val playlists = playlistCacheDatabase.getAllPlaylists()
        Log.d(TAG, "getPlaylistsFromCache: found ${playlists.size} playlists")
        return playlists.map { playlist ->
            val trackIds = playlistCacheDatabase.getPlaylistTracks(playlist.id ?: "")
            Log.d(TAG, "getPlaylistsFromCache: playlist ${playlist.id} has ${trackIds.size} tracks")
            playlist.toUiModel(trackIds)
        }
    }
}
