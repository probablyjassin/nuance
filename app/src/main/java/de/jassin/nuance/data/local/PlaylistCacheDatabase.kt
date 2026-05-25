@file:Suppress("unused")

package de.jassin.nuance.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import de.jassin.nuance.data.api.PlaylistDto

class PlaylistCacheDatabase(
    context: Context,
) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        private const val DB_NAME = "playlist_cache.db"
        private const val DB_VERSION = 1
        private const val PLAYLISTS_CACHE_KEY = "playlists_initialized"
        const val TAG = "PlaylistCacheDatabase"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE playlists (
                id TEXT PRIMARY KEY,
                name TEXT,
                comment TEXT,
                duration REAL,
                size INTEGER,
                songCount INTEGER,
                ownerName TEXT,
                ownerId TEXT,
                public INTEGER,
                path TEXT,
                sync INTEGER,
                uploadedImage TEXT,
                externalImageUrl TEXT,
                createdAt TEXT,
                updatedAt TEXT
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE playlist_tracks (
                id TEXT PRIMARY KEY,
                playlistId TEXT,
                songId TEXT,
                FOREIGN KEY(playlistId) REFERENCES playlists(id)
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE cache_meta (
                cache_key TEXT PRIMARY KEY,
                cache_value INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        db.execSQL("DROP TABLE IF EXISTS playlist_tracks")
        db.execSQL("DROP TABLE IF EXISTS playlists")
        db.execSQL("DROP TABLE IF EXISTS cache_meta")
        onCreate(db)
    }

    fun getPlaylistCount(): Int {
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM playlists", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                Log.d(TAG, "getPlaylistCount: $count playlists in database")
                return count
            }
            Log.d(TAG, "getPlaylistCount: 0 playlists in database")
            return 0
        }
    }

    fun isPlaylistsCacheInitialized(): Boolean {
        val db = readableDatabase
        db
            .rawQuery(
                "SELECT cache_value FROM cache_meta WHERE cache_key = ?",
                arrayOf(PLAYLISTS_CACHE_KEY),
            ).use { cursor ->
                val result = cursor.moveToFirst() && cursor.getInt(0) == 1
                Log.d(TAG, "isPlaylistsCacheInitialized: $result")
                return result
            }
    }

    fun getAllPlaylists(): List<PlaylistDto> {
        val db = readableDatabase
        val playlists = mutableListOf<PlaylistDto>()

        db
            .rawQuery(
                """
                SELECT id, name, comment, duration, size, songCount, ownerName, ownerId, public, 
                       path, sync, uploadedImage, externalImageUrl, createdAt, updatedAt 
                FROM playlists
                """.trimIndent(),
                null,
            ).use { cursor ->
                Log.d(TAG, "getAllPlaylists: cursor count=${cursor.count}")
                while (cursor.moveToNext()) {
                    playlists.add(
                        PlaylistDto(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                            comment = cursor.getString(cursor.getColumnIndexOrThrow("comment")),
                            duration = cursor.getDouble(cursor.getColumnIndexOrThrow("duration")),
                            size = cursor.getInt(cursor.getColumnIndexOrThrow("size")),
                            songCount = cursor.getInt(cursor.getColumnIndexOrThrow("songCount")),
                            ownerName = cursor.getString(cursor.getColumnIndexOrThrow("ownerName")),
                            ownerId = cursor.getString(cursor.getColumnIndexOrThrow("ownerId")),
                            public = cursor.getInt(cursor.getColumnIndexOrThrow("public")) == 1,
                            path = cursor.getString(cursor.getColumnIndexOrThrow("path")),
                            sync = cursor.getInt(cursor.getColumnIndexOrThrow("sync")) == 1,
                            uploadedImage = cursor.getString(cursor.getColumnIndexOrThrow("uploadedImage")),
                            externalImageUrl = cursor.getString(cursor.getColumnIndexOrThrow("externalImageUrl")),
                            createdAt = cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
                            updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updatedAt")),
                        ),
                    )
                }
            }

        Log.d(TAG, "getAllPlaylists: returning ${playlists.size} playlists")
        return playlists
    }

    fun getPlaylistTracks(playlistId: String): List<String> {
        val db = readableDatabase
        val songIds = mutableListOf<String>()

        db
            .rawQuery(
                "SELECT songId FROM playlist_tracks WHERE playlistId = ? ORDER BY id",
                arrayOf(playlistId),
            ).use { cursor ->
                Log.d(TAG, "getPlaylistTracks: playlistId=$playlistId, cursor count=${cursor.count}")
                while (cursor.moveToNext()) {
                    songIds.add(cursor.getString(cursor.getColumnIndexOrThrow("songId")))
                }
            }

        Log.d(TAG, "getPlaylistTracks: playlistId=$playlistId returning ${songIds.size} track IDs")
        return songIds
    }

    fun replaceAllPlaylists(playlists: List<PlaylistDto>) {
        val db = writableDatabase
        Log.d(TAG, "replaceAllPlaylists: saving ${playlists.size} playlists")
        db.beginTransaction()
        try {
            db.delete("playlist_tracks", null, null)
            Log.d(TAG, "replaceAllPlaylists: cleared playlist_tracks table")
            db.delete("playlists", null, null)
            Log.d(TAG, "replaceAllPlaylists: cleared playlists table")

            playlists.forEach { playlist ->
                val row =
                    ContentValues().apply {
                        put("id", playlist.id ?: "")
                        put("name", playlist.name)
                        put("comment", playlist.comment)
                        put("duration", playlist.duration)
                        put("size", playlist.size)
                        put("songCount", playlist.songCount)
                        put("ownerName", playlist.ownerName)
                        put("ownerId", playlist.ownerId)
                        put("public", if (playlist.public == true) 1 else 0)
                        put("path", playlist.path)
                        put("sync", if (playlist.sync == true) 1 else 0)
                        put("uploadedImage", playlist.uploadedImage)
                        put("externalImageUrl", playlist.externalImageUrl)
                        put("createdAt", playlist.createdAt)
                        put("updatedAt", playlist.updatedAt)
                    }
                db.insert("playlists", null, row)
                Log.d(TAG, "replaceAllPlaylists: inserted playlist ${playlist.id}")
            }

            db.setTransactionSuccessful()
            Log.d(TAG, "replaceAllPlaylists: transaction successful")
        } catch (e: Exception) {
            Log.e(TAG, "replaceAllPlaylists: error", e)
        } finally {
            db.endTransaction()
        }
    }

    fun markCacheAsInitialized() {
        val db = writableDatabase
        Log.d(TAG, "markCacheAsInitialized: marking cache as initialized")
        val meta =
            ContentValues().apply {
                put("cache_key", PLAYLISTS_CACHE_KEY)
                put("cache_value", 1)
            }
        db.insertWithOnConflict("cache_meta", null, meta, SQLiteDatabase.CONFLICT_REPLACE)
        Log.d(TAG, "markCacheAsInitialized: done")
    }

    fun clearAllCache() {
        val db = writableDatabase
        Log.d(TAG, "clearAllCache: clearing all playlist cache")
        db.beginTransaction()
        try {
            db.delete("playlist_tracks", null, null)
            Log.d(TAG, "clearAllCache: cleared playlist_tracks table")
            db.delete("playlists", null, null)
            Log.d(TAG, "clearAllCache: cleared playlists table")
            db.delete("cache_meta", "cache_key = ?", arrayOf(PLAYLISTS_CACHE_KEY))
            Log.d(TAG, "clearAllCache: cleared cache_meta entry")

            db.setTransactionSuccessful()
            Log.d(TAG, "clearAllCache: cache cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "clearAllCache: error", e)
        } finally {
            db.endTransaction()
        }
    }

    fun replacePlaylistTracks(
        playlistId: String,
        trackIds: List<String>,
    ) {
        val db = writableDatabase
        Log.d(TAG, "replacePlaylistTracks: playlistId=$playlistId, ${trackIds.size} tracks")
        db.beginTransaction()
        try {
            db.delete("playlist_tracks", "playlistId = ?", arrayOf(playlistId))
            Log.d(TAG, "replacePlaylistTracks: cleared tracks for playlistId=$playlistId")

            trackIds.forEachIndexed { index, songId ->
                val row =
                    ContentValues().apply {
                        put("id", "$playlistId-$index")
                        put("playlistId", playlistId)
                        put("songId", songId)
                    }
                db.insert("playlist_tracks", null, row)
            }
            Log.d(TAG, "replacePlaylistTracks: inserted ${trackIds.size} track IDs for playlistId=$playlistId")

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "replacePlaylistTracks: error", e)
        } finally {
            db.endTransaction()
        }
    }
}
