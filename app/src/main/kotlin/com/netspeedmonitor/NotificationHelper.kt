package com.netspeedmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build

object NotificationHelper {
    const val CHANNEL_ID = "netspeed_service"
    const val CHANNEL_NAME = "Net speed service"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the network speed monitor running"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun build(context: Context, text: String, liveUpdate: Boolean): Notification {
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, CHANNEL_ID)
            else
                @Suppress("DEPRECATION") Notification.Builder(context)

        builder
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_speed)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent(context))

        if (liveUpdate) {
            try {
                builder.setSmallIcon(Icon.createWithBitmap(drawTextBitmap(text)))
            } catch (_: Throwable) {
            }
            tryCall(builder, "setShortCriticalText", text)
            tryCallBoolean(builder, "setRequestPromotedOngoing")
        }
        return builder.build()
    }

    private fun drawTextBitmap(text: String): Bitmap {
        val size = 24
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        val baseline = size / 2f - (p.descent() + p.ascent()) / 2f
        c.drawText(text, size / 2f, baseline, p)
        return bmp
    }

    private fun tryCall(builder: Notification.Builder, method: String, arg: CharSequence) {
        try {
            Notification.Builder::class.java
                .getMethod(method, CharSequence::class.java)
                .invoke(builder, arg)
        } catch (_: Throwable) {
        }
    }

    private fun tryCallBoolean(builder: Notification.Builder, method: String) {
        try {
            Notification.Builder::class.java
                .getMethod(method, Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
        } catch (_: Throwable) {
        }
    }

    fun show(context: Context, text: String, liveUpdate: Boolean) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIFICATION_ID, build(context, text, liveUpdate))
    }

    fun cancel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.cancel(NOTIFICATION_ID)
    }
}
