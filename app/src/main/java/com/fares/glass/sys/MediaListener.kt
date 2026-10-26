package com.fares.glass.sys

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** What is playing right now, for the card on the home screen. */
object NowPlaying {
    /** True while a music app has a session (playing or paused). The card shows only then. */
    var active by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var title by mutableStateOf("")
    var artist by mutableStateOf("")
    var art by mutableStateOf<ImageBitmap?>(null)
    var durationMs by mutableLongStateOf(0L)
    private var basePos by mutableLongStateOf(0L)
    private var baseTime by mutableLongStateOf(0L)
    private var speed by mutableFloatStateOf(1f)

    private var controller: MediaController? = null
    private var lastBitmap: Bitmap? = null

    fun isPlaying(c: MediaController): Boolean {
        val s = c.playbackState?.state
        return s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
    }

    /** Playing or paused: the app is still running. Stopped, errored or no state means it is gone. */
    fun isLive(c: MediaController): Boolean {
        val s = c.playbackState?.state ?: return false
        return s != PlaybackState.STATE_NONE &&
            s != PlaybackState.STATE_STOPPED &&
            s != PlaybackState.STATE_ERROR
    }

    fun attach(c: MediaController?) {
        controller = c
        refresh()
    }

    fun refresh() {
        val c = controller
        if (c == null) {
            active = false
            playing = false
            return
        }
        val state = c.playbackState
        val meta = c.metadata
        playing = isPlaying(c)
        active = isLive(c)
        title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
        artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val bmp = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bmp !== lastBitmap) {
            lastBitmap = bmp
            art = bmp?.asImageBitmap()
        }
        val pos = state?.position ?: 0L
        basePos = if (pos < 0) 0L else pos
        val stamp = state?.lastPositionUpdateTime ?: 0L
        baseTime = if (stamp > 0) stamp else SystemClock.elapsedRealtime()
        speed = state?.playbackSpeed ?: 1f
    }

    /** Current play position, estimated from the last update. */
    fun position(now: Long): Long {
        val p = if (playing) basePos + ((now - baseTime) * speed).toLong() else basePos
        return if (durationMs > 0) p.coerceIn(0L, durationMs) else p.coerceAtLeast(0L)
    }

    // The three controls return false when nothing is attached, so the remote key goes on to the system.
    fun togglePlayPause(): Boolean {
        val c = controller ?: return false
        if (isPlaying(c)) c.transportControls.pause() else c.transportControls.play()
        return true
    }

    fun next(): Boolean {
        val c = controller ?: return false
        c.transportControls.skipToNext()
        return true
    }

    fun previous(): Boolean {
        val c = controller ?: return false
        c.transportControls.skipToPrevious()
        return true
    }

    /**
     * Turns on Glass's access to media sessions. Needs the WRITE_SECURE_SETTINGS permission
     * (granted over ADB, like the animation speed one).
     */
    fun ensureListener(context: Context) {
        try {
            val me = ComponentName(context, MediaListener::class.java).flattenToString()
            val key = "enabled_notification_listeners"
            val current = Settings.Secure.getString(context.contentResolver, key) ?: ""
            if (current.split(":").contains(me)) return
            val updated = if (current.isEmpty()) me else "$current:$me"
            Settings.Secure.putString(context.contentResolver, key, updated)
        } catch (e: Exception) {
            Log.w("Glass", "Can't enable media access", e)
        }
    }
}

/** Gives Glass the list of active media sessions. It never reads any notification. */
class MediaListener : NotificationListenerService() {

    private var manager: MediaSessionManager? = null
    private var controllers: List<MediaController> = emptyList()
    private val component by lazy { ComponentName(this, MediaListener::class.java) }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        track(list)
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = choose()
        override fun onMetadataChanged(metadata: MediaMetadata?) = choose()
        override fun onSessionDestroyed() = choose()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val m = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            manager = m
            m.addOnActiveSessionsChangedListener(sessionListener, component)
            track(m.getActiveSessions(component))
        } catch (e: Exception) {
            Log.w("Glass", "Media sessions unavailable", e)
        }
    }

    override fun onListenerDisconnected() {
        try {
            manager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (_: Exception) {
        }
        controllers.forEach { runCatching { it.unregisterCallback(callback) } }
        controllers = emptyList()
        NowPlaying.attach(null)
        super.onListenerDisconnected()
    }

    private fun track(list: List<MediaController>?) {
        controllers.forEach { runCatching { it.unregisterCallback(callback) } }
        controllers = list.orEmpty().filter { it.packageName != packageName }
        controllers.forEach { it.registerCallback(callback) }
        choose()
    }

    /** Prefer the session that is playing; otherwise one that is paused. None means the card hides. */
    private fun choose() {
        val best = controllers.firstOrNull { NowPlaying.isPlaying(it) }
            ?: controllers.firstOrNull { NowPlaying.isLive(it) }
        NowPlaying.attach(best)
    }
}
