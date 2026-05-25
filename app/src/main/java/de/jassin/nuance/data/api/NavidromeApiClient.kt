@file:Suppress("unused")

package de.jassin.nuance.data.api

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String? = null,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val year: Int? = null,
    val genre: String? = null,
)

@Serializable
data class PlaylistDto(
    val id: String? = null,
    val name: String? = null,
    val comment: String? = null,
    val duration: Double? = null,
    val size: Int? = null,
    val songCount: Int? = null,
    val ownerName: String? = null,
    val ownerId: String? = null,
    val public: Boolean? = null,
    val path: String? = null,
    val sync: Boolean? = null,
    val uploadedImage: String? = null,
    val externalImageUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class PlaylistTrackDto(
    val id: String? = null,
    val mediaFileId: String? = null,
    val playlistId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val duration: Double? = null,
    val bitRate: Int? = null,
    val path: String? = null,
    val starred: Boolean? = null,
    val starredAt: String? = null,
    val playCount: Int? = null,
    val playDate: String? = null,
    val rating: Int? = null,
    val ratedAt: String? = null,
    val libraryPath: String? = null,
    val libraryName: String? = null,
)

class NavidromeApiClient {
    private val client = _root_ide_package_.de.jassin.nuance.data.api.HttpClientProvider.client

    private companion object {
        const val TAG = "NavidromeApiClient"
    }

    private fun buildAuthenticatedUrl(
        serverUrl: String,
        pathSegments: List<String>,
        queryParameters: Map<String, String>,
    ): String =
        URLBuilder(serverUrl.trimEnd('/'))
            .apply {
                pathSegments.forEach { encodedPath += "/$it" }
                queryParameters.forEach { (key, value) -> parameters.append(key, value) }
            }.buildString()

    suspend fun pingAuth(
        serverUrl: String,
        token: String,
    ): Boolean {
        val baseUrl = serverUrl.trimEnd('/')
        Log.d(TAG, "pingAuth -> $baseUrl/api/song")
        val response =
            client.get("$baseUrl/api/song") {
                header("x-nd-authorization", "Bearer $token")
            }
        Log.d(TAG, "pingAuth response status=${response.status}")
        return response.status.isSuccess()
    }

    suspend fun fetchSongs(
        serverUrl: String,
        token: String,
    ): List<de.jassin.nuance.data.api.SongDto> {
        val baseUrl = serverUrl.trimEnd('/')
        Log.d(TAG, "fetchSongs -> $baseUrl/api/song")
        val response =
            client.get("$baseUrl/api/song") {
                header("x-nd-authorization", "Bearer $token")
            }

        if (!response.status.isSuccess()) {
            Log.d(TAG, "fetchSongs failed: ${response.status}")
            return emptyList()
        }

        return try {
            val songs = response.body<List<de.jassin.nuance.data.api.SongDto>>()
            Log.d(TAG, "fetchSongs success: count=${songs.size}")
            songs
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing songs response", e)
            emptyList()
        }
    }

    suspend fun fetchSongCount(
        serverUrl: String,
        token: String,
    ): Int = fetchSongs(serverUrl, token).size

    suspend fun fetchCoverArt(
        serverUrl: String,
        username: String,
        subsonicToken: String,
        subsonicSalt: String,
        songId: String,
        apiVersion: String = "1.16.1",
        clientName: String = "Nuance",
    ): ByteArray? {
        Log.d(TAG, "fetchCoverArt requested for songId=$songId")
        val url =
            buildAuthenticatedUrl(
                serverUrl = serverUrl,
                pathSegments = listOf("rest", "getCoverArt"),
                queryParameters =
                    mapOf(
                        "u" to username,
                        "t" to subsonicToken,
                        "s" to subsonicSalt,
                        "v" to apiVersion,
                        "c" to clientName,
                        "id" to songId,
                    ),
            )
        val response =
            client.get(url) {
                header("Accept", "image/*")
            }

        if (!response.status.isSuccess()) {
            Log.d(TAG, "fetchCoverArt failed for songId=$songId: ${response.status}")
            return null
        }

        return try {
            val bytes: ByteArray = response.body()
            Log.d(TAG, "fetchCoverArt success for songId=$songId: bytes=${bytes.size}")
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cover art bytes for songId=$songId", e)
            null
        }
    }

    suspend fun resolveStreamUrl(
        serverUrl: String,
        username: String,
        subsonicToken: String,
        subsonicSalt: String,
        songId: String,
        apiVersion: String = "1.16.1",
        clientName: String = "Nuance",
    ): String =
        buildAuthenticatedUrl(
            serverUrl = serverUrl,
            pathSegments = listOf("rest", "stream"),
            queryParameters =
                mapOf(
                    "u" to username,
                    "t" to subsonicToken,
                    "s" to subsonicSalt,
                    "v" to apiVersion,
                    "c" to clientName,
                    "id" to songId,
                ),
        ).also {
            Log.d(TAG, "resolveStreamUrl built stream URL for songId=$songId")
        }

    suspend fun fetchPlaylists(
        serverUrl: String,
        token: String,
    ): List<de.jassin.nuance.data.api.PlaylistDto> {
        val baseUrl = serverUrl.trimEnd('/')
        Log.d(TAG, "fetchPlaylists -> $baseUrl/api/playlist/")
        val response =
            client.get("$baseUrl/api/playlist/") {
                header("x-nd-authorization", "Bearer $token")
            }

        Log.d(TAG, "fetchPlaylists response status=${response.status}")
        if (!response.status.isSuccess()) {
            Log.d(TAG, "fetchPlaylists failed: ${response.status}")
            return emptyList()
        }

        return try {
            val playlists = response.body<List<PlaylistDto>>()
            Log.d(TAG, "fetchPlaylists success: count=${playlists.size}")
            playlists.forEach { p ->
                Log.d(TAG, "  Playlist: id=${p.id}, name=${p.name}, songCount=${p.songCount}")
            }
            playlists
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing playlists response", e)
            emptyList()
        }
    }

    suspend fun fetchPlaylistTracks(
        serverUrl: String,
        token: String,
        playlistId: String,
    ): List<PlaylistTrackDto> {
        val baseUrl = serverUrl.trimEnd('/')
        Log.d(TAG, "fetchPlaylistTracks -> $baseUrl/api/playlist/$playlistId/tracks")
        val response =
            client.get("$baseUrl/api/playlist/$playlistId/tracks") {
                header("x-nd-authorization", "Bearer $token")
            }

        Log.d(TAG, "fetchPlaylistTracks response status for $playlistId=${response.status}")
        if (!response.status.isSuccess()) {
            Log.d(TAG, "fetchPlaylistTracks failed for playlistId=$playlistId: ${response.status}")
            return emptyList()
        }

        return try {
            val tracks = response.body<List<PlaylistTrackDto>>()
            Log.d(TAG, "fetchPlaylistTracks success: playlistId=$playlistId count=${tracks.size}")
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing playlist tracks response for playlistId=$playlistId", e)
            emptyList()
        }
    }
}
