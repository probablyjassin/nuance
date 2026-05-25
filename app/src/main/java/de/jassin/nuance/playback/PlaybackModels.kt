@file:Suppress("ArrayInDataClass")

package de.jassin.nuance.playback

import de.jassin.nuance.data.models.SongUiModel

data class PlaybackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val coverArtId: String? = null,
    val durationMs: Long? = null,
    val streamUrl: String? = null,
)

data class PlaybackState(
    val queue: List<PlaybackItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val currentDurationMs: Long? = null,
    val currentCoverArt: ByteArray? = null,
) {
    val currentItem: PlaybackItem?
        get() = queue.getOrNull(currentIndex)
}

fun SongUiModel.toPlaybackItem(): PlaybackItem =
    PlaybackItem(
        id = id,
        title = title,
        artist = artist,
        album = album,
        // TODO: wire proper cover art id once playback metadata comes from API/player domain.
        coverArtId = id,
    )
