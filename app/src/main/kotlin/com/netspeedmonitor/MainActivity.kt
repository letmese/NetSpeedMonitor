package com.netspeedmonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var switchOverlay: SwitchCompat
    private lateinit var switchAccessibility: SwitchCompat
    private lateinit var switchNotification: SwitchCompat
    private lateinit var switchLiveUpdate: SwitchCompat
    private lateinit var switchLock: SwitchCompat
    private lateinit var tvStatus: TextView
    private lateinit var tvHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvHint = findViewById(R.id.tvHint)
        switchOverlay = findViewById(R.id.switchOverlay)
        switchAccessibility = findViewById(R.id.switchAccessibility)
        switchNotification = findViewById(R.id.switchNotification)
        switchLiveUpdate = findViewById(R.id.switchLiveUpdate)
        switchLock = findViewById(R.id.switchLock)
        val btnStart: MaterialButton = findViewById(R.id.btnStart)
        val btnStop: MaterialButton = findViewById(R.id.btnStop)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        switchOverlay.setOnCheckedChangeListener { _, checked -> Prefs.setOverlayEnabled(this, checked) }
        switchAccessibility.setOnCheckedChangeListener { _, checked ->
            Prefs.setAccessibilityEnabled(this, checked)
            if (checked && !Prefs.isAccessibilityGranted(this)) {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) {
                }
            }
        }
        switchNotification.setOnCheckedChangeListener { _, checked -> Prefs.setNotificationEnabled(this, checked) }
        switchLiveUpdate.setOnCheckedChangeListener { _, checked -> Prefs.setLiveUpdateEnabled(this, checked) }
        switchLock.setOnCheckedChangeListener { _, checked -> Prefs.setOverlayLocked(this, checked) }

        btnStart.setOnClickListener {
            if (!Prefs.canDrawOverlay(this)) {
                runCatching {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }.getOrElse {
                    runCatching { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
                }
            }
            SpeedMonitorService.start(this)
            refresh()
        }
        btnStop.setOnClickListener {
            SpeedMonitorService.stop(this)
            refresh()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        switchOverlay.isChecked = Prefs.isOverlayEnabled(this)
        switchAccessibility.isChecked = Prefs.isAccessibilityEnabled(this)
        switchNotification.isChecked = Prefs.isNotificationEnabled(this)
        switchLiveUpdate.isChecked = Prefs.isLiveUpdateEnabled(this)
        switchLock.isChecked = Prefs.isOverlayLocked(this)

        tvStatus.text = if (Prefs.isRunning(this)) {
            getString(R.string.status_running)
        } else {
            getString(R.string.status_stopped)
        }

        val hints = mutableListOf<String>()
        if (Prefs.isOverlayEnabled(this) && !Prefs.canDrawOverlay(this)) {
            hints.add(getString(R.string.hint_overlay_permission))
        }
        if (Prefs.isAccessibilityEnabled(this) && !Prefs.isAccessibilityGranted(this)) {
            hints.add(getString(R.string.hint_accessibility_permission))
        }
        if (Prefs.isOverlayEnabled(this) && Prefs.isOverlayLocked(this)) {
            hints.add(getString(R.string.hint_overlay_locked))
        }
        tvHint.text = hints.joinToString("\n")
    }
}
