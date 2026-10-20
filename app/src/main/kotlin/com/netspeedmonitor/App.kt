package com.netspeedmonitor

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
