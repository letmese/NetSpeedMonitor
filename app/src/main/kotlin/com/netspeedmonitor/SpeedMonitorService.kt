package com.netspeedmonitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service that samples traffic and drives the overlay + live notification (merged NSM + PixelMeter). */
class SpeedMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.netspeedmonitor.action.START"
        const val ACTION_STOP = "com.netspeedmonitor.action.STOP"

        fun start(context: Context) {
            val i = Intent(context, SpeedMonitorService::class.java).setAction(ACTION_START)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpeedMonitorService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataSource = SpeedDataSource()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: OverlayController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.build(this, getString(R.string.notification_starting), Prefs.isLiveUpdateEnabled(this))
        )
        startMonitoring()
        return START_STICKY
    }

    private var monitoringJob: kotlinx.coroutines.Job? = null

    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        Prefs.setRunning(this, true)
        monitoringJob = scope.launch {
            while (isActive) {
                val data = dataSource.sample()
                val text = "↓ ${formatSpeed(data.downBps)}  ↑ ${formatSpeed(data.upBps)}"

                val wantOverlay = Prefs.isOverlayEnabled(this@SpeedMonitorService) &&
                    Prefs.canDrawOverlay(this@SpeedMonitorService)
                mainHandler.post {
                    if (wantOverlay && overlay == null) overlay = OverlayController(this@SpeedMonitorService).also {
                        it.show(Prefs.overlayX(this@SpeedMonitorService), Prefs.overlayY(this@SpeedMonitorService), text)
                    }
                    if (!wantOverlay && overlay != null) {
                        overlay?.hide()
                        overlay = null
                    }
                    overlay?.update(text)
                }

                if (Prefs.isNotificationEnabled(this@SpeedMonitorService)) {
                    NotificationHelper.show(
                        this@SpeedMonitorService,
                        text,
                        Prefs.isLiveUpdateEnabled(this@SpeedMonitorService)
                    )
                }
                delay(1000)
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Prefs.setRunning(this, false)
        mainHandler.post {
            overlay?.hide()
            overlay = null
        }
        NotificationHelper.cancel(this)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
