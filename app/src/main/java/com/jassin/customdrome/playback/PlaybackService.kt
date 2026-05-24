package com.jassin.customdrome.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jassin.customdrome.MainActivity

class PlaybackService : MediaSessionService() {
    private companion object {
        const val TAG = "PlaybackService"
        const val NOTIFICATION_CHANNEL_ID = "playback_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Music playback"
        const val NOTIFICATION_ID = 1001
    }

    private var isForeground = false

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateForegroundNotification()
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                updateForegroundNotification()
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateForegroundNotification()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateForegroundNotification()
            }
        }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()
        createNotificationChannel()
        val player = PlaybackEngine.initialize(this)
        player.addListener(playerListener)
        updateForegroundNotification()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "onGetSession() from controller=${controllerInfo.packageName}")
        return PlaybackEngine.currentSession().also {
            Log.d(TAG, "onGetSession() returning session=${it != null}")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        PlaybackEngine.currentPlayer()?.removeListener(playerListener)
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        PlaybackEngine.release()
        super.onDestroy()
    }

    private fun updateForegroundNotification() {
        val notification = buildNotification()
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val state = PlaybackEngine.currentPlayer()
        val item = state?.currentMediaItem
        val metadata = item?.mediaMetadata

        val title = metadata?.title?.toString().orEmpty().ifBlank { "CustomDrome" }
        val artist = metadata?.artist?.toString().orEmpty().ifBlank {
            if (state?.isPlaying == true) "Playing" else "Paused"
        }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return

        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            )
        manager.createNotificationChannel(channel)
    }
}


