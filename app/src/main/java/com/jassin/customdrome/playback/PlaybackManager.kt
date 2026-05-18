package com.jassin.customdrome.playback

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackManager(
    private val context: Context,
    private val coverFetcher: suspend (songId: String) -> ByteArray? = { null },
    private val streamUrlResolver: suspend (songId: String) -> String? = { null },
) {
    private companion object {
        const val TAG = "PlaybackManager"
    }

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observedPlayer: ExoPlayer? = null

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "Player isPlaying changed: $isPlaying")
                syncStateFromPlayer()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "Media item transition: mediaId=${mediaItem?.mediaId}, reason=$reason")
                syncStateFromPlayer()
                refreshCoverForCurrent()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "Playback state changed: $playbackState")
                syncStateFromPlayer()
            }

            override fun onPlayerError(error: PlaybackException) {
                val current = player.currentMediaItem
                Log.e(
                    TAG,
                    buildString {
                        append("Player error: code=")
                        append(error.errorCodeName)
                        append(", message=")
                        append(error.message)
                        append(", mediaId=")
                        append(current?.mediaId)
                        append(", uri=")
                        append(current?.localConfiguration?.uri)
                        append(", playWhenReady=")
                        append(player.playWhenReady)
                        append(", playbackState=")
                        append(player.playbackState)
                    },
                    error,
                )
            }

            override fun onEvents(player: Player, events: Player.Events) {
                Log.d(TAG, "Player events received: size=${events.size()}")
                syncStateFromPlayer()
            }
        }

    private val player: ExoPlayer
        get() = PlaybackEngine.initialize(context)

    init {
        Log.d(TAG, "PlaybackManager created")
    }

    fun playQueue(
        queue: List<PlaybackItem>,
        startIndex: Int = 0,
        startPlaying: Boolean = true,
    ) {
        if (queue.isEmpty()) {
            Log.d(TAG, "playQueue called with empty queue; clearing playback state")
            clearQueue()
            return
        }

        Log.d(TAG, "playQueue requested: queueSize=${queue.size}, startIndex=$startIndex, startPlaying=$startPlaying")
        scope.launch {
            try {
                val resolvedQueue = resolveQueue(queue)
                Log.d(TAG, "Resolved queue entries: ${resolvedQueue.size}/${queue.size}")
                if (resolvedQueue.isEmpty()) {
                    Log.w(TAG, "No playable items resolved; clearing playback")
                    clearQueue()
                    return@launch
                }

                val resolvedStartIndex = resolveStartIndex(queue, resolvedQueue, startIndex)
                val mediaItems = resolvedQueue.map { it.toMediaItem() }

                withContext(Dispatchers.Main.immediate) {
                    Log.d(TAG, "Starting playback service and preparing ExoPlayer (startIndex=$resolvedStartIndex)")
                    startPlaybackService()
                    val exoPlayer = player
                    observePlayer(exoPlayer)
                    exoPlayer.setMediaItems(mediaItems, resolvedStartIndex, 0L)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = startPlaying
                    if (startPlaying) {
                        exoPlayer.play()
                    } else {
                        exoPlayer.pause()
                    }
                    Log.d(
                        TAG,
                        "ExoPlayer prepared: mediaItemCount=${exoPlayer.mediaItemCount}, currentIndex=${exoPlayer.currentMediaItemIndex}, playWhenReady=${exoPlayer.playWhenReady}",
                    )
                }

                _state.value =
                    PlaybackState(
                        queue = resolvedQueue,
                        currentIndex = resolvedStartIndex,
                        isPlaying = startPlaying,
                        positionMs = 0L,
                        currentCoverArt = null,
                    )
                refreshCoverForCurrent()
            } catch (t: Throwable) {
                Log.e(TAG, "playQueue failed", t)
                clearQueue()
            }
        }
    }

    fun play() {
        Log.d(TAG, "play() called")
        startPlaybackService()
        player.play()
        syncStateFromPlayer()
    }

    fun pause() {
        Log.d(TAG, "pause() called")
        player.pause()
        syncStateFromPlayer()
    }

    fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause() called; currentlyPlaying=${player.isPlaying}")
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun next() {
        Log.d(TAG, "next() called; hasNext=${player.hasNextMediaItem()}")
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
        syncStateFromPlayer()
    }

    fun previous() {
        Log.d(TAG, "previous() called; hasPrevious=${player.hasPreviousMediaItem()}")
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
        syncStateFromPlayer()
    }

    fun seekTo(positionMs: Long) {
        Log.d(TAG, "seekTo($positionMs) called")
        player.seekTo(positionMs.coerceAtLeast(0L))
        syncStateFromPlayer()
    }

    fun enqueue(items: List<PlaybackItem>) {
        if (items.isEmpty()) {
            Log.d(TAG, "enqueue called with empty list; ignoring")
            return
        }

        Log.d(TAG, "enqueue requested: itemCount=${items.size}")
        scope.launch {
            try {
                val resolvedItems = resolveQueue(items)
                Log.d(TAG, "enqueue resolved items: ${resolvedItems.size}/${items.size}")
                if (resolvedItems.isEmpty()) {
                    Log.w(TAG, "enqueue resolved no playable items")
                    return@launch
                }

                withContext(Dispatchers.Main.immediate) {
                    val exoPlayer = player
                    observePlayer(exoPlayer)
                    if (exoPlayer.mediaItemCount == 0) {
                        exoPlayer.setMediaItems(resolvedItems.map { it.toMediaItem() })
                        exoPlayer.prepare()
                    } else {
                        exoPlayer.addMediaItems(resolvedItems.map { it.toMediaItem() })
                    }
                }

                _state.update { current ->
                    val mergedQueue = current.queue + resolvedItems
                    if (current.currentIndex == -1) {
                        current.copy(queue = mergedQueue, currentIndex = 0)
                    } else {
                        current.copy(queue = mergedQueue)
                    }
                }
                Log.d(TAG, "enqueue complete; queueSize=${_state.value.queue.size}")
            } catch (t: Throwable) {
                Log.e(TAG, "enqueue failed", t)
            }
        }
    }

    fun clearQueue() {
        Log.d(TAG, "clearQueue() called")
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                player.stop()
                player.clearMediaItems()
            }
            PlaybackEngine.release()
            observedPlayer = null
            _state.value = PlaybackState()
            stopPlaybackService()
            Log.d(TAG, "Playback cleared and service stopped")
        }
    }

    private suspend fun resolveQueue(queue: List<PlaybackItem>): List<PlaybackItem> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Resolving stream URLs for queueSize=${queue.size}")
            queue.mapNotNull { item ->
                val streamUrl = item.streamUrl ?: streamUrlResolver(item.id)
                if (streamUrl.isNullOrBlank()) {
                    Log.w(TAG, "No stream URL for songId=${item.id}; skipping item")
                    null
                } else {
                    Log.d(TAG, "Resolved stream URL for songId=${item.id}")
                    item.copy(streamUrl = streamUrl)
                }
            }
        }

    private fun resolveStartIndex(
        originalQueue: List<PlaybackItem>,
        resolvedQueue: List<PlaybackItem>,
        requestedIndex: Int,
    ): Int {
        if (resolvedQueue.isEmpty()) return 0

        val resolvedIds = resolvedQueue.map { it.id }.toSet()
        val startCandidateOriginalIndex =
            (requestedIndex.coerceAtLeast(0) until originalQueue.size).firstOrNull { originalQueue[it].id in resolvedIds }
                ?: (0 until originalQueue.size).firstOrNull { originalQueue[it].id in resolvedIds }
                ?: return 0

        val candidateId = originalQueue[startCandidateOriginalIndex].id
        return resolvedQueue.indexOfFirst { it.id == candidateId }.coerceAtLeast(0)
    }

    private fun startPlaybackService() {
        Log.d(TAG, "Starting PlaybackService via startService()")
        context.startService(Intent(context, PlaybackService::class.java))
    }

    private fun stopPlaybackService() {
        Log.d(TAG, "Stopping PlaybackService")
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    private fun observePlayer(newPlayer: ExoPlayer) {
        if (observedPlayer === newPlayer) return
        Log.d(TAG, "Observing ExoPlayer instance")
        observedPlayer?.removeListener(playerListener)
        newPlayer.addListener(playerListener)
        observedPlayer = newPlayer
    }

    private fun syncStateFromPlayer() {
        val exoPlayer = PlaybackEngine.currentPlayer()
        if (exoPlayer == null) {
            Log.w(TAG, "syncStateFromPlayer: no player available")
            _state.value = PlaybackState()
            return
        }

        val currentQueue = _state.value.queue
        val maxIndex = if (currentQueue.isNotEmpty()) currentQueue.lastIndex else exoPlayer.mediaItemCount - 1
        val currentIndex =
            if (exoPlayer.mediaItemCount == 0) {
                -1
            } else {
                exoPlayer.currentMediaItemIndex.coerceIn(0, maxIndex.coerceAtLeast(0))
            }

        _state.update { current ->
            current.copy(
                currentIndex = currentIndex,
                isPlaying = exoPlayer.isPlaying,
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            )
        }
        Log.d(
            TAG,
            "Synced state: queueSize=${_state.value.queue.size}, currentIndex=${_state.value.currentIndex}, isPlaying=${_state.value.isPlaying}, positionMs=${_state.value.positionMs}",
        )
    }

    private fun refreshCoverForCurrent() {
        val id = _state.value.currentItem?.coverArtId ?: return
        Log.d(TAG, "Refreshing cover art for songId=$id")
        _state.update { it.copy(currentCoverArt = null) }
        scope.launch {
            try {
                val bytes = coverFetcher(id)
                Log.d(TAG, "Cover art fetch complete for songId=$id: bytes=${bytes?.size ?: 0}")
                _state.update { it.copy(currentCoverArt = bytes) }
            } catch (t: Throwable) {
                Log.e(TAG, "Cover art fetch failed for songId=$id", t)
                _state.update { it.copy(currentCoverArt = null) }
            }
        }
    }

    private fun PlaybackItem.toMediaItem(): MediaItem =
        run {
            Log.d(TAG, "Creating MediaItem for songId=$id, hasStreamUrl=${streamUrl != null}")
            MediaItem.Builder()
                .setMediaId(id)
                .setUri(requireNotNull(streamUrl) { "PlaybackItem.streamUrl must be resolved before playback" }.toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .build(),
                )
                .build()
        }
}
