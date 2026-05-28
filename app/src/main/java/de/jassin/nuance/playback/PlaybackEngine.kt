package de.jassin.nuance.playback

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import de.jassin.nuance.MainActivity
import de.jassin.nuance.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object PlaybackEngine {
    private const val TAG = "PlaybackEngine"

    @Volatile
    private var player: ExoPlayer? = null

    @Volatile
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    @Synchronized
    fun initialize(context: Context, userPrefs: UserPreferences): ExoPlayer {
        val existingPlayer = player
        if (existingPlayer != null) return existingPlayer

        val appContext = context.applicationContext


        val secureHostnames = runBlocking {
            userPrefs.server.secureHostnames.first()
        }

        Log.d(TAG, "Initializing ExoPlayer with secureHostnames=$secureHostnames")

        // Build OkHttpClient based on security preference
        val okHttpClient =
            if (secureHostnames == true) {
                // Standard secure client — normal TLS validation
                OkHttpClient.Builder().build()
            } else {
                // Local server mode — trust all certificates
                val trustAllCerts =
                    arrayOf<TrustManager>(
                        @SuppressLint("CustomX509TrustManager")
                        object : X509TrustManager {
                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkClientTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?,
                            ) {}

                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkServerTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?,
                            ) {}

                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        },
                    )

                val sslContext =
                    SSLContext.getInstance("SSL").apply {
                        init(null, trustAllCerts, SecureRandom())
                    }

                OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
                    .build()
            }


        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val createdPlayer =
            ExoPlayer
                .Builder(appContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true,
                    )
                    setHandleAudioBecomingNoisy(true)
                }

        val intent =
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pendingIntent =
            PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val createdSession =
            MediaSession
                .Builder(appContext, createdPlayer)
                .setSessionActivity(pendingIntent)
                .build()

        player = createdPlayer
        mediaSession = createdSession
        Log.d(TAG, "ExoPlayer initialized successfully")
        return createdPlayer
    }

    fun currentPlayer(): ExoPlayer? = player

    fun currentSession(): MediaSession? = mediaSession

    @Synchronized
    fun release() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        Log.d(TAG, "PlaybackEngine released")
    }
}
