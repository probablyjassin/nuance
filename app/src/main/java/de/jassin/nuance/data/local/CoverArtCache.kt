package de.jassin.nuance.data.local

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest

class CoverArtCache(
    context: Context,
) {
    private val cacheDir = File(context.cacheDir, "cover_art")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun getCachedCoverArt(songId: String): ByteArray? =
        try {
            val file = File(cacheDir, hashSongId(songId))
            if (file.exists()) {
                Log.d("CoverArtCache", "cache hit for songId=$songId")
                file.readBytes()
            } else {
                Log.d("CoverArtCache", "cache miss for songId=$songId")
                null
            }
        } catch (e: Exception) {
            Log.e("CoverArtCache", "error reading cache for songId=$songId: ${e.message}")
            null
        }

    fun saveCoverArt(
        songId: String,
        bytes: ByteArray,
    ) {
        try {
            val file = File(cacheDir, hashSongId(songId))
            file.writeBytes(bytes)
            Log.d("CoverArtCache", "saved cover art for songId=$songId (${bytes.size} bytes)")
        } catch (e: Exception) {
            Log.e("CoverArtCache", "error saving cover art for songId=$songId: ${e.message}")
        }
    }

    fun clearCache() {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            Log.d("CoverArtCache", "cache cleared")
        } catch (e: Exception) {
            Log.e("CoverArtCache", "error clearing cache: ${e.message}")
        }
    }

    private fun hashSongId(songId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(songId.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
