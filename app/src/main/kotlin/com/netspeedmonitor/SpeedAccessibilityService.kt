package com.netspeedmonitor

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Draws the speed at the top of the screen with an accessibility overlay (ColorOS/status-bar friendly). */
class SpeedAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var textView: TextView? = null
    private var executor: ScheduledExecutorService? = null
    private val handler = Handler(Looper.getMainLooper())
    private val dataSource = SpeedDataSource()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val tv = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            gravity = Gravity.CENTER
        }
        textView = tv
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 0
            params.y = getStatusBarHeight()
            windowManager?.addView(tv, params)
        } catch (_: Exception) {
        }

        executor = Executors.newSingleThreadScheduledExecutor()
        executor?.scheduleAtFixedRate({
            val data = dataSource.sample()
            val text = "↓ ${formatSpeed(data.downBps)} ↑ ${formatSpeed(data.upBps)}"
            handler.post { textView?.text = text }
        }, 0, 1, TimeUnit.SECONDS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No UI interaction needed.
    }

    override fun onInterrupt() {
        // No-op.
    }

    override fun onDestroy() {
        executor?.shutdownNow()
        executor = null
        try {
            textView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        textView = null
        super.onDestroy()
    }

    private fun getStatusBarHeight(): Int {
        val res = resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) res.getDimensionPixelSize(id) else dp(24)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
