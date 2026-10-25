package com.fares.glass.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.fares.glass.sys.MediaListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class Track(
    val title: String,
    val artist: String,
    val app: String,
    val art: Bitmap?,
    val playing: Boolean,
    val toggle: () -> Unit,
)

object NowPlaying {
    /** Emits null when nothing is playing or notification access isn't granted. */
    fun flow(ctx: Context): Flow<Track?> = callbackFlow {
        val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val listener = ComponentName(ctx, MediaListener::class.java)
        var controllers: List<MediaController> = emptyList()

        fun push() {
            val c = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()
            trySend(c?.let { toTrack(ctx, it) })
        }

        val cb = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = push()
            override fun onPlaybackStateChanged(state: PlaybackState?) = push()
        }

        fun attach(list: List<MediaController>?) {
            controllers.forEach { it.unregisterCallback(cb) }
            controllers = list.orEmpty()
            controllers.forEach { it.registerCallback(cb) }
            push()
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, listener)
            attach(msm.getActiveSessions(listener))
        } catch (e: SecurityException) {
            trySend(null) // notification access not granted yet
        }

        awaitClose {
            controllers.forEach { it.unregisterCallback(cb) }
            runCatching { msm.removeOnActiveSessionsChangedListener(sessionsListener) }
        }
    }

    private fun toTrack(ctx: Context, c: MediaController): Track? {
        val m = c.metadata ?: return null
        val title = m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return null
        val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty()
        val art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val pm = ctx.packageManager
        val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString() }
            .getOrDefault(c.packageName)
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        return Track(title, artist, app, art, playing) {
            if (playing) c.transportControls.pause() else c.transportControls.play()
        }
    }
}
