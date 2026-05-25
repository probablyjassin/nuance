package de.jassin.nuance.data.models

import de.jassin.nuance.data.api.SongDto

/**
 * UI-friendly song representation used by the songs screen.
 */
data class SongUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val year: Int? = null,
    val genre: String? = null,
)

fun SongDto.toUiModel(): SongUiModel =
    SongUiModel(
        id = id.orEmpty(),
        title = title.orEmpty(),
        artist = artist.orEmpty(),
        album = album,
        year = year,
        genre = genre,
    )
