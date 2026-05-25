@file:Suppress("unused")

package de.jassin.nuance.data.models

import de.jassin.nuance.data.api.PlaylistDto

/**
 * UI-friendly playlist representation used by the playlists screen.
 * Tracks are stored as a list of song IDs, not full song objects.
 */
data class PlaylistUiModel(
    val id: String,
    val name: String,
    val comment: String? = null,
    val ownerName: String? = null,
    val ownerId: String? = null,
    val public: Boolean = false,
    val trackCount: Int = 0,
    val songCount: Int? = null,
    val duration: Double? = null,
    val tracks: List<String> = emptyList(), // List of song IDs
)

fun PlaylistDto.toUiModel(trackIds: List<String> = emptyList()): PlaylistUiModel =
    PlaylistUiModel(
        id = id.orEmpty(),
        name = name.orEmpty(),
        comment = comment,
        ownerName = ownerName,
        ownerId = ownerId,
        public = public ?: false,
        trackCount = trackIds.size,
        songCount = songCount,
        duration = duration,
        tracks = trackIds,
    )
