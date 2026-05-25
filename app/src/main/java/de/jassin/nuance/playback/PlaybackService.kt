package de.jassin.nuance.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import de.jassin.nuance.MainActivity
import kotlin.jvm.java

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

        // OPTIONAL BUT RECOMMENDED: Set a single-top activity intent on the session.
        // This ensures that when a user taps the background of the system notification,
        // it cleanly opens your MainActivity instead of launching a duplicate instance.
        if (session?.sessionActivity == null) {
            val intent =
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            session?.setSessionActivity(pendingIntent)
        }

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
