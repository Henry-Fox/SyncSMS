package com.syncsms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var tvBattery: TextView
    private lateinit var tvStatus: TextView

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val charging = plugged != 0
            tvBattery.text = buildString {
                append("电量：")
                if (pct >= 0) append("${pct}%") else append("--")
                append(if (charging) "（充电中）" else "（未充电）")
            }
            val low = pct in 0..19 && !charging
            tvBattery.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (low) android.R.color.holo_red_dark else android.R.color.black
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBattery = findViewById(R.id.tvBattery)
        tvStatus = findViewById(R.id.tvStatus)
        val etServer = findViewById<EditText>(R.id.etServer)
        val etDeviceKey = findViewById<EditText>(R.id.etDeviceKey)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnFullSync = findViewById<Button>(R.id.btnFullSync)
        val btnNotifAccess = findViewById<Button>(R.id.btnNotifAccess)
        val tvNotifStatus = findViewById<TextView>(R.id.tvNotifStatus)
        val btnBatterySettings = findViewById<Button>(R.id.btnBatterySettings)

        requestNotificationPermissionIfNeeded()
        requestSmsPermissionsIfNeeded()
        updateNotifAccessStatus(tvNotifStatus)

        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        etServer.setText(prefs.getString(Prefs.KEY_SERVER, ""))
        etDeviceKey.setText(prefs.getString(Prefs.KEY_DEVICE_KEY, ""))

        if (prefs.getBoolean(Prefs.KEY_SERVICE_ENABLED, false)) {
            startSyncService()
            tvStatus.text = "状态：运行中（前台服务，后台保持）"
        }

        btnSave.setOnClickListener {
            val newServer = etServer.text.toString().trim()
            val newDeviceKey = etDeviceKey.text.toString().trim()
            val oldServer = prefs.getString(Prefs.KEY_SERVER, "") ?: ""
            val oldDeviceKey = prefs.getString(Prefs.KEY_DEVICE_KEY, "") ?: ""

            prefs.edit {
                putString(Prefs.KEY_SERVER, newServer)
                putString(Prefs.KEY_DEVICE_KEY, newDeviceKey)
                if (newServer != oldServer || newDeviceKey != oldDeviceKey) {
                    remove(Prefs.KEY_DEVICE_TOKEN)
                }
            }
            tvStatus.text = "状态：已保存"
        }

        btnStart.setOnClickListener {
            prefs.edit { putBoolean(Prefs.KEY_SERVICE_ENABLED, true) }
            startSyncService()
            tvStatus.text = "状态：运行中（前台服务，后台保持）"
        }

        btnFullSync.setOnClickListener {
            prefs.edit { putLong(Prefs.KEY_LAST_SEEN_SMS_TIME_MS, 0L) }
            tvStatus.text = "状态：已重置同步游标，下次轮询将补传所有短信"
            Log.i("SyncSMS", "full sync requested: lastSeenSmsTimeMs reset to 0")
        }

        btnNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnBatterySettings.setOnClickListener {
            openBatteryRelatedSettings()
        }
    }

    /**
     * Android 13+ 必须授予通知权限，前台服务的常驻通知才能稳定显示。
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQ_POST_NOTIFICATIONS
        )
    }

    /**
     * Android 6+ 短信权限为运行时权限：未授权则无法接收 SMS_RECEIVED 广播，自然无法入队与上传。
     */
    private fun requestSmsPermissionsIfNeeded() {
        val missing = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.RECEIVE_SMS)
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_SMS)
        }
        if (missing.isEmpty()) return
        ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ_SMS_PERMISSIONS)
    }

    private fun startSyncService() {
        val intent = Intent(this, SyncForegroundService::class.java)
        startForegroundService(intent)
        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val server = prefs.getString(Prefs.KEY_SERVER, "")?.trim().orEmpty()
        Log.i("SyncSMS", "start service requested, server=$server")
    }

    /**
     * 引导用户关闭对本应用的电池优化或允许后台运行，降低被系统杀进程的概率。
     */
    private fun openBatteryRelatedSettings() {
        // 合规做法：不直接请求忽略电池优化权限（Play 政策限制），改为跳转系统设置由用户手动配置。
        val pkg = packageName
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            return
        } catch (_: Exception) {
            // 部分 ROM 不支持该页面，退回应用详情页
        }

        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", pkg, null)
        })
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onResume() {
        super.onResume()
        updateNotifAccessStatus(findViewById(R.id.tvNotifStatus))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(batteryReceiver)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, SmsNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.contains(cn.flattenToString())
    }

    private fun updateNotifAccessStatus(tv: TextView) {
        if (isNotificationListenerEnabled()) {
            tv.text = "✅ 通知监听权限已授予，可同步所有短信通知"
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            tv.text = "⚠️ 华为手机必须授予此权限才能同步所有验证码短信"
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    companion object {
        private const val REQ_POST_NOTIFICATIONS = 1001
        private const val REQ_SMS_PERMISSIONS = 1002
    }
}
