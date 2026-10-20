package com.netspeedmonitor

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

object Prefs {
    const val PREFS_NAME = "netspeed"

    private const val KEY_RUNNING = "running"
    private const val KEY_OVERLAY = "overlay_enabled"
    private const val KEY_ACCESSIBILITY = "accessibility_enabled"
    private const val KEY_NOTIFICATION = "notification_enabled"
    private const val KEY_LIVE_UPDATE = "live_update_enabled"
    private const val KEY_OVERLAY_LOCKED = "overlay_locked"
    private const val KEY_OVERLAY_X = "overlay_x"
    private const val KEY_OVERLAY_Y = "overlay_y"

    const val DEFAULT_OVERLAY_X = 24
    const val DEFAULT_OVERLAY_Y = 200

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isRunning(c: Context) = sp(c).getBoolean(KEY_RUNNING, false)
    fun setRunning(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_RUNNING, v).apply()

    fun isOverlayEnabled(c: Context) = sp(c).getBoolean(KEY_OVERLAY, true)
    fun setOverlayEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_OVERLAY, v).apply()

    fun isAccessibilityEnabled(c: Context) = sp(c).getBoolean(KEY_ACCESSIBILITY, false)
    fun setAccessibilityEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_ACCESSIBILITY, v).apply()

    fun isNotificationEnabled(c: Context) = sp(c).getBoolean(KEY_NOTIFICATION, true)
    fun setNotificationEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_NOTIFICATION, v).apply()

    fun isLiveUpdateEnabled(c: Context) = sp(c).getBoolean(KEY_LIVE_UPDATE, true)
    fun setLiveUpdateEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_LIVE_UPDATE, v).apply()

    fun isOverlayLocked(c: Context) = sp(c).getBoolean(KEY_OVERLAY_LOCKED, false)
    fun setOverlayLocked(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_OVERLAY_LOCKED, v).apply()

    fun overlayX(c: Context) = sp(c).getInt(KEY_OVERLAY_X, DEFAULT_OVERLAY_X)
    fun overlayY(c: Context) = sp(c).getInt(KEY_OVERLAY_Y, DEFAULT_OVERLAY_Y)
    fun setOverlayPos(c: Context, x: Int, y: Int) =
        sp(c).edit().putInt(KEY_OVERLAY_X, x).putInt(KEY_OVERLAY_Y, y).apply()

    fun canDrawOverlay(c: Context): Boolean = Settings.canDrawOverlays(c)

    fun isAccessibilityGranted(c: Context): Boolean {
        val enabled = Settings.Secure.getString(
            c.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = c.packageName + "/" + SpeedAccessibilityService::class.java.name
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
