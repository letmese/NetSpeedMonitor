package com.netspeedmonitor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import kotlin.math.abs
import kotlin.math.roundToInt

/** A draggable always-on-top bubble (PixelMeter style) showing the current speed. */
class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var textView: TextView? = null
    private var visible = false

    private var lastX = 0
    private var lastY = 0
    private var downRawX = 0f
    private var downRawY = 0f
    private var moved = false

    @SuppressLint("ClickableViewAccessibility")
    private fun build(): TextView {
        return TextView(context).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            gravity = Gravity.CENTER
            setOnClickListener {
                if (!moved) {
                    try {
                        context.startActivity(
                            Intent(context, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                    } catch (_: Exception) {
                    }
                }
            }
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (Prefs.isOverlayLocked(context)) return@setOnTouchListener false
                        moved = false
                        downRawX = event.rawX
                        downRawY = event.rawY
                        v.performClick()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (Prefs.isOverlayLocked(context)) return@setOnTouchListener false
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY
                        if (abs(dx) > 8f || abs(dy) > 8f) moved = true
                        if (event.rawX >= 0f && event.rawY >= 0f) {
                            updatePosition(
                                (event.rawX - downRawX + lastX).roundToInt(),
                                (event.rawY - downRawY + lastY).roundToInt()
                            )
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        Prefs.setOverlayPos(context, lastX, lastY)
                        true
                    }
                    else -> true
                }
            }
        }
    }

    fun show(x: Int, y: Int, text: String) {
        if (visible) {
            update(text)
            updatePosition(x, y)
            return
        }
        val tv = build()
        textView = tv
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y
        tv.text = text
        try {
            windowManager.addView(tv, params)
            visible = true
            lastX = x
            lastY = y
        } catch (_: Exception) {
        }
    }

    fun updatePosition(x: Int, y: Int) {
        lastX = x
        lastY = y
        val tv = textView ?: return
        mainHandler.post {
            try {
                tv.updateLayoutParams<WindowManager.LayoutParams> { this.x = x; this.y = y }
            } catch (_: Exception) {
            }
        }
    }

    fun update(text: String) {
        val tv = textView ?: return
        mainHandler.post { tv.text = text }
    }

    fun hide() {
        val tv = textView ?: return
        textView = null
        visible = false
        mainHandler.post {
            try {
                windowManager.removeView(tv)
            } catch (_: Exception) {
            }
        }
    }

    fun isShowing(): Boolean = visible

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).roundToInt()
}
