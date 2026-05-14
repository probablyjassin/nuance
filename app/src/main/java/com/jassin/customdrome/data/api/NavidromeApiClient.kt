package com.jassin.customdrome.data.api

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
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

class NavidromeApiClient {
    private val client = HttpClientProvider.client

    suspend fun pingAuth(
        serverUrl: String,
        token: String,
    ): Boolean {
        val baseUrl = serverUrl.trimEnd('/')
        val response =
            client.get("$baseUrl/api/song") {
                header("x-nd-authorization", "Bearer $token")
            }
        Log.d("NavidromeApiClient", "pingAuth response: ${response.bodyAsText()}")
        return response.status.isSuccess()
    }

    suspend fun fetchSongCount(
        serverUrl: String,
        token: String,
    ): Int {
        val baseUrl = serverUrl.trimEnd('/')
        val response =
            client.get("$baseUrl/api/song") {
                header("x-nd-authorization", "Bearer $token")
            }

        if (!response.status.isSuccess()) {
            Log.d("NavidromeApiClient", "fetchSongCount failed: ${response.status}")
            return 0
        }

        return try {
            // Parse the response as a list of SongDto objects
            val songs: List<SongDto> = response.body()
            Log.d("NavidromeApiClient", "fetchSongCount success: ${songs.size} songs")
            songs.size
        } catch (e: Exception) {
            Log.e("NavidromeApiClient", "Error parsing song response", e)
            0
        }
    }
}
