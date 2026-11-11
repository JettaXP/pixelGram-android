package com.pixelgram.android.music

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pixelgram.android.PixelApp

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            androidx.media3.session.DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(com.pixelgram.android.R.drawable.ic_pixelgram) },
        )
        val open = android.content.Intent(this, com.pixelgram.android.MainActivity::class.java)
            .setAction(OpenPlayerAction)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = android.app.PendingIntent.getActivity(this, 0, open, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, (application as PixelApp).graph.music.player).setSessionActivity(pending).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}

const val OpenPlayerAction = "com.pixelgram.android.OPEN_PLAYER"
