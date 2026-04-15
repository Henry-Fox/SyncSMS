package com.syncsms

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.syncsms.repo.SmsQueueRepository
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.syncsms.work.SmsUploadWorker
import java.util.concurrent.Executors

/**
 * 监听系统短信 App 的通知，从中提取发送人和短信内容。
 * 华为 EMUI/HarmonyOS 不将所有短信写入标准 content://sms，
 * 但通知始终会弹出，因此通过通知捕获是最可靠的方式。
 */
class SmsNotificationListener : NotificationListenerService() {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "syncsms-notif-worker").apply { isDaemon = true }
    }

    companion object {
        private val SMS_PACKAGES = setOf(
            "com.huawei.message",        // 华为信息
            "com.android.mms",           // 原生信息
            "com.google.android.apps.messaging", // Google Messages
            "com.samsung.android.messaging",     // 三星信息
            "com.android.messaging"       // AOSP 信息
        )

        /**
         * 在进程存活时主动请求系统重新绑定此 NotificationListenerService。
         * 解决华为等 OEM 进程重启后系统不自动重连的问题。
         */
        fun tryRebind(context: android.content.Context) {
            try {
                requestRebind(ComponentName(context, SmsNotificationListener::class.java))
                Log.i("SyncSMS", "notification listener: requestRebind sent")
            } catch (e: Exception) {
                Log.w("SyncSMS", "notification listener: requestRebind failed: ${e.message}")
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("SyncSMS", "notification listener: CONNECTED")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("SyncSMS", "notification listener: DISCONNECTED, requesting rebind")
        tryRebind(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        if (pkg !in SMS_PACKAGES) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        if (title.isNullOrEmpty() || text.isNullOrEmpty()) return

        Log.i("SyncSMS", "notification sms captured: pkg=$pkg, sender=$title, len=${text.length}, content=${text.take(120)}")

        val ctx = applicationContext
        executor.execute {
            try {
                val repo = SmsQueueRepository(ctx)
                val inserted = repo.enqueue(
                    sender = title,
                    content = text,
                    smsTimeMs = sbn.postTime
                )
                Log.i("SyncSMS", "notification sms enqueue: inserted=$inserted")

                if (inserted) {
                    WorkManager.getInstance(ctx)
                        .enqueue(OneTimeWorkRequestBuilder<SmsUploadWorker>().build())
                }
            } catch (e: Exception) {
                Log.e("SyncSMS", "notification sms enqueue failed: ${e.message}", e)
            }
        }
    }
}
