package com.fares.glass

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** How long with no remote press before the screensaver starts over another app (5 minutes). */
private const val IDLE_MS = 5 * 60_000L

class HomeAccessibilityService : AccessibilityService() {

    // Stock launchers to take over from. Add the ones you encounter.
    private val stockLaunchers = setOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.amazon.tv.launcher",
        "com.amazon.firelauncher",
        "com.android.tv.launcher"
    )

    // Music apps: the screensaver may start even while they are playing sound.
    // Add more package names here.
    private val musicApps = setOf(
        "com.spotify.tv.android",
        "com.google.android.youtube.tvmusic",
        "com.apple.android.music"
    )

    // Windows that appear on top of apps and don't count as "the app".
    private val ignoredPackages = setOf("com.android.systemui", "android")

    private val handler = Handler(Looper.getMainLooper())
    private var lastLaunch = 0L
    private var lastInput = SystemClock.elapsedRealtime()
    private var foregroundPkg = ""
    private var saverStarted = false

    private val idleCheck = object : Runnable {
        override fun run() {
            checkIdle()
            handler.postDelayed(this, 5_000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lastInput = SystemClock.elapsedRealtime()
        handler.removeCallbacks(idleCheck)
        handler.postDelayed(idleCheck, 5_000L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(idleCheck)
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Any remote press counts as activity.
        lastInput = SystemClock.elapsedRealtime()
        saverStarted = false

        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        // Let long-press through (system menu / recents)
        if (event.action == KeyEvent.ACTION_UP && event.eventTime - event.downTime < 500) {
            launchGlass(screensaver = false)
        }
        return true // consume Home so the stock launcher never gets it
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in stockLaunchers) {
            launchGlass(screensaver = false)
            return
        }
        if (pkg !in ignoredPackages) foregroundPkg = pkg
    }

    private fun checkIdle() {
        if (saverStarted) return
        // Glass shows its own screensaver when it is the app on screen.
        if (foregroundPkg.isEmpty() || foregroundPkg == packageName) return
        if (SystemClock.elapsedRealtime() - lastInput < IDLE_MS) return

        // Sound playing usually means someone is watching something, except in music apps.
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.isMusicActive && foregroundPkg !in musicApps) return

        saverStarted = true
        launchGlass(screensaver = true)
    }

    private fun launchGlass(screensaver: Boolean) {
        // Debounce: both mechanisms can fire for the same press
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 600) return
        lastLaunch = now

        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra("screensaver", screensaver)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
        )
    }

    override fun onInterrupt() {}
}
