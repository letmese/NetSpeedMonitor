package com.netspeedmonitor

import android.net.TrafficStats
import java.util.Locale

data class SpeedData(val downBps: Long, val upBps: Long)

/** Samples overall TX/RX traffic deltas to compute bytes-per-second, like classic speed monitors. */
class SpeedDataSource {
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTime = 0L
    private var downRate = 0L
    private var upRate = 0L

    @Synchronized
    fun sample(): SpeedData {
        val now = System.currentTimeMillis()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (lastTime > 0L) {
            val dtMs = (now - lastTime).coerceAtLeast(1)
            val dt = dtMs / 1000f
            if (dt > 0f) {
                if (rx >= lastRx) downRate = ((rx - lastRx) / dt).toLong()
                if (tx >= lastTx) upRate = ((tx - lastTx) / dt).toLong()
            }
        }
        lastRx = rx
        lastTx = tx
        lastTime = now
        return SpeedData(downRate, upRate)
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024f
    val mb = bytesPerSec / (1024f * 1024f)
    return when {
        bytesPerSec < 1024 -> "$bytesPerSec B/s"
        kb < 1024 -> String.format(Locale.US, "%.0f KB/s", kb)
        else -> String.format(Locale.US, "%.1f MB/s", mb)
    }
}
