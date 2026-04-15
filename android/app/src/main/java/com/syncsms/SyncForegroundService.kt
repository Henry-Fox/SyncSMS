package com.syncsms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest
import android.app.Service
import android.content.IntentFilter
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.syncsms.repo.SmsQueueRepository
import com.syncsms.work.SmsUploadWorker
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 前台服务：用于保活并上报心跳（电量/在线状态）。
 *
 * 短信上传由 Room 队列 + WorkManager 负责，避免依赖进程常驻。
 */
class SyncForegroundService : Service() {

    private var timer: Timer? = null
    /** 与心跳分离，避免同一 [Timer] 多任务在部分机型上表现异常 */
    private var pollTimer: Timer? = null

    /** 收件箱查询在部分 ROM 上会长时间阻塞；单独线程 + 超时避免 Timer 线程卡死且无任何日志 */
    private val inboxPollExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "syncsms-inbox-worker").apply { isDaemon = true }
    }

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification("SyncSMS 运行中（短信同步，请勿关闭通知）")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(1001, notification)
        }

        startHeartbeatLoop()
        startInboxPollLoop()
        SmsNotificationListener.tryRebind(this)
        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val server = prefs.getString(Prefs.KEY_SERVER, "")?.trim().orEmpty()
        Log.i("SyncSMS", "service created, heartbeat loop started, server=$server")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 仅确保服务保持运行；短信接收/上传不再通过 Intent 传递。
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        pollTimer?.cancel()
        pollTimer = null
        inboxPollExecutor.shutdownNow()
    }

    private fun startHeartbeatLoop() {
        // 60 秒一次心跳：更新电量/在线状态，便于 Web 端可视化
        if (timer != null) return
        timer = Timer("syncsms-heartbeat", true)
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    sendHeartbeatOnce()
                } catch (e: Exception) {
                    Log.w("SyncSMS", "heartbeat failed: ${e.message}")
                }
            }
        }, 1000L, 60_000L)
    }

    private fun startInboxPollLoop() {
        if (pollTimer != null) return
        pollTimer = Timer("syncsms-inbox-poll", true)
        // 不依赖 SMS_RECEIVED：华为等 ROM 可能不下发广播给第三方
        pollTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Log.i("SyncSMS", "poll inbox timer fired")
                val future = inboxPollExecutor.submit {
                    pollInboxOnce()
                }
                try {
                    future.get(POLL_QUERY_TIMEOUT_SEC, TimeUnit.SECONDS)
                } catch (_: TimeoutException) {
                    Log.w("SyncSMS", "poll inbox: timed out after ${POLL_QUERY_TIMEOUT_SEC}s (SMS provider may block); cancelling worker")
                    future.cancel(true)
                } catch (e: ExecutionException) {
                    val c = e.cause
                    Log.w("SyncSMS", "poll inbox failed: ${c?.message}", c)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.w("SyncSMS", "poll inbox: interrupted", e)
                }
            }
        }, 2_000L, 30_000L)
    }

    private fun pollInboxOnce() {
        Log.i("SyncSMS", "poll inbox begin")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("SyncSMS", "poll inbox skipped: READ_SMS not granted")
            return
        }

        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val lastSeen = prefs.getLong(Prefs.KEY_LAST_SEEN_SMS_TIME_MS, 0L)

        // 诊断：全面探测内容提供者可见的短信
        try {
            val allSmsCursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf("_id", Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ),
                null, null, "${Telephony.Sms.DATE} DESC"
            )
            val allCount = allSmsCursor?.count ?: -1
            allSmsCursor?.use { c ->
                val iAddr = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val iDate = c.getColumnIndex(Telephony.Sms.DATE)
                val iType = c.getColumnIndex(Telephony.Sms.TYPE)
                val iRead = c.getColumnIndex(Telephony.Sms.READ)
                var idx = 0
                while (c.moveToNext() && idx < 30) {
                    val addr = if (iAddr >= 0) c.getString(iAddr) else "?"
                    val date = if (iDate >= 0) c.getLong(iDate) else 0
                    val type = if (iType >= 0) c.getInt(iType) else -1
                    val read = if (iRead >= 0) c.getInt(iRead) else -1
                    Log.i("SyncSMS", "  diag sms[$idx] addr=$addr date=$date type=$type read=$read")
                    idx++
                }
            }
            // 尝试 mms-sms 混合 URI
            val mmsSmsCount = try {
                contentResolver.query(
                    Uri.parse("content://mms-sms/conversations?simple=true"),
                    null, null, null, null
                )?.use { it.count } ?: -1
            } catch (_: Exception) { -1 }
            Log.i("SyncSMS", "poll inbox diag: total_sms=$allCount, mms_sms_conversations=$mmsSmsCount, lastSeenMs=$lastSeen")
        } catch (e: Exception) {
            Log.w("SyncSMS", "poll inbox diag failed: ${e.message}")
        }

        // 不限 type，全量拉取所有短信（兼容华为等 OEM 对 type 字段的特殊处理）
        val uri: Uri = Telephony.Sms.CONTENT_URI
        Log.i("SyncSMS", "poll inbox querying uri=$uri lastSeenMs=$lastSeen")
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE)
        val selection = if (lastSeen > 0) "${Telephony.Sms.DATE} > ?" else null
        val args = if (lastSeen > 0) arrayOf(lastSeen.toString()) else null

        var maxSeen = lastSeen
        var insertedCount = 0
        var rowCount = 0
        val repo = SmsQueueRepository(this)

        val cursor = try {
            contentResolver.query(uri, projection, selection, args, "${Telephony.Sms.DATE} ASC")
        } catch (e: SecurityException) {
            Log.w("SyncSMS", "poll inbox query denied: ${e.message}")
            return
        }

        if (cursor == null) {
            Log.w("SyncSMS", "poll inbox: contentResolver.query returned null")
            return
        }

        cursor.use { c ->
            val idxAddr = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val idxBody = c.getColumnIndex(Telephony.Sms.BODY)
            val idxDate = c.getColumnIndex(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                rowCount++
                val sender = if (idxAddr >= 0) (c.getString(idxAddr) ?: "") else ""
                val content = if (idxBody >= 0) (c.getString(idxBody) ?: "") else ""
                val ts = if (idxDate >= 0) c.getLong(idxDate) else 0L
                if (ts <= 0L) continue
                if (ts > maxSeen) maxSeen = ts
                val inserted = repo.enqueue(sender = sender, content = content, smsTimeMs = ts)
                if (inserted) insertedCount++
            }
        }

        Log.i(
            "SyncSMS",
            "poll inbox scan: rows=$rowCount, inserted=$insertedCount, lastSeenBefore=$lastSeen, maxSeen=$maxSeen"
        )

        if (maxSeen > lastSeen) {
            prefs.edit().putLong(Prefs.KEY_LAST_SEEN_SMS_TIME_MS, maxSeen).apply()
        }
        if (insertedCount > 0) {
            enqueueUploadWorker()
        }
    }

    companion object {
        private const val POLL_QUERY_TIMEOUT_SEC = 20L
    }

    private fun enqueueUploadWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = OneTimeWorkRequestBuilder<SmsUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(req)
    }

    private fun sendHeartbeatOnce() {
        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val server = prefs.getString(Prefs.KEY_SERVER, "")?.trim().orEmpty()
        val deviceKey = prefs.getString(Prefs.KEY_DEVICE_KEY, "")?.trim().orEmpty()
        if (server.isEmpty() || deviceKey.isEmpty()) return

        val battery = readBattery()

        var token = prefs.getString(Prefs.KEY_DEVICE_TOKEN, "")?.trim().orEmpty()
        if (token.isEmpty()) {
            token = ApiClient.deviceAuth(server, deviceKey)
            prefs.edit().putString(Prefs.KEY_DEVICE_TOKEN, token).apply()
        }

        try {
            ApiClient.heartbeat(
                server,
                token,
                ApiClient.HeartbeatRequest(
                    batteryPercent = battery.first,
                    charging = battery.second
                )
            )
            Log.i("SyncSMS", "heartbeat ok, battery=${battery.first}, charging=${battery.second}")
        } catch (e: Exception) {
            // token 过期/服务端重启等：重试一次认证再发
            token = ApiClient.deviceAuth(server, deviceKey)
            prefs.edit().putString(Prefs.KEY_DEVICE_TOKEN, token).apply()
            ApiClient.heartbeat(
                server,
                token,
                ApiClient.HeartbeatRequest(
                    batteryPercent = battery.first,
                    charging = battery.second
                )
            )
            Log.i("SyncSMS", "heartbeat ok(after reauth), battery=${battery.first}, charging=${battery.second}")
        }
    }

    /**
     * 在心跳 Timer 的后台线程读取电量：不可用 sticky 广播（部分机型/线程下 registerReceiver(null) 会返回 null）。
     * 使用 [BatteryManager] 可在任意线程稳定取到百分比与充电状态（API 21+/23+）。
     */
    private fun readBattery(): Pair<Int?, Boolean?> {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        var pct: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (raw in 0..100) {
                pct = raw
            }
        }
        val charging: Boolean? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bm.isCharging
        } else {
            @Suppress("DEPRECATION")
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            plugged != 0
        }
        return Pair(pct, charging)
    }

    private fun buildNotification(contentText: String): Notification {
        val channelId = "syncsms_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId, "SyncSMS", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("SyncSMS")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("SyncSMS")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1001, buildNotification(text))
    }
}

