package de.jassin.nuance.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.jassin.nuance.data.api.SongDto

class SongCacheDatabase(
    context: Context,
) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE songs (
                id TEXT PRIMARY KEY,
                title TEXT,
                album TEXT,
                artist TEXT,
                year INTEGER,
                genre TEXT
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
        db.execSQL("DROP TABLE IF EXISTS songs")
        db.execSQL("DROP TABLE IF EXISTS cache_meta")
        onCreate(db)
    }

    fun getSongCount(): Int {
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM songs", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun isSongsCacheInitialized(): Boolean {
        val db = readableDatabase
        db
            .rawQuery(
                "SELECT cache_value FROM cache_meta WHERE cache_key = ?",
                arrayOf(SONGS_CACHE_KEY),
            ).use { cursor ->
                return cursor.moveToFirst() && cursor.getInt(0) == 1
            }
    }

    fun getAllSongs(): List<SongDto> {
        val db = readableDatabase
        val songs = mutableListOf<SongDto>()

        db.rawQuery("SELECT id, title, album, artist, year, genre FROM songs", null).use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(
                    SongDto(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        album = cursor.getString(cursor.getColumnIndexOrThrow("album")),
                        artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                        year = cursor.getInt(cursor.getColumnIndexOrThrow("year")),
                        genre = cursor.getString(cursor.getColumnIndexOrThrow("genre")),
                    ),
                )
            }
        }

        return songs
    }

    fun replaceAllSongs(songs: List<SongDto>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("songs", null, null)
            songs.forEach { song ->
                val row =
                    ContentValues().apply {
                        put("id", song.id ?: "")
                        put("title", song.title)
                        put("album", song.album)
                        put("artist", song.artist)
                        put("year", song.year)
                        put("genre", song.genre)
                    }
                db.insert("songs", null, row)
            }

            val meta =
                ContentValues().apply {
                    put("cache_key", SONGS_CACHE_KEY)
                    put("cache_value", 1)
                }
            db.insertWithOnConflict("cache_meta", null, meta, SQLiteDatabase.CONFLICT_REPLACE)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DB_NAME = "song_cache.db"
        private const val DB_VERSION = 1
        private const val SONGS_CACHE_KEY = "songs_initialized"
    }
}
