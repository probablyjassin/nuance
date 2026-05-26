package de.jassin.nuance.playback

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private companion object {
        const val TAG = "PlaybackService"
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()

        // 1. Initialize your engine and player
        val player = PlaybackEngine.initialize(this)

        // 2. Fetch the session
        val session = PlaybackEngine.currentSession()

        // 3. CRITICAL: Add the session to the service's internal manager.
        // This tells Media3 to listen to this session for background notification lifecycles!
        if (session != null) {
            addSession(session)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "onGetSession() from controller=${controllerInfo.packageName}")

        val session = PlaybackEngine.currentSession()

        return session
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        PlaybackEngine.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = PlaybackEngine.currentPlayer()
        // If the player isn't playing, kill the service completely when swiped away
        if (player == null || !player.playWhenReady || player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            stopSelf()
        }
    }
}
