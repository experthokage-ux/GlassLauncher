package com.fares.glass

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class HomeAccessibilityService : AccessibilityService() {

    // Stock launchers to take over from. Add the ones you encounter.
    private val stockLaunchers = setOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.amazon.tv.launcher",
        "com.amazon.firelauncher",
        "com.android.tv.launcher"
    )

    private var lastLaunch = 0L

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        // Let long-press through (system menu / recents)
        if (event.action == KeyEvent.ACTION_UP && event.eventTime - event.downTime < 500) {
            openGlass()
        }
        return true // consume Home so the stock launcher never gets it
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in stockLaunchers) openGlass()
    }

    private fun openGlass() {
        // Debounce: both mechanisms can fire for the same press
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 600) return
        lastLaunch = now

        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }

    override fun onInterrupt() {}
}
