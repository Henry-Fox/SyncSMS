package com.syncsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.syncsms.repo.SmsQueueRepository
import com.syncsms.work.SmsUploadWorker
import java.util.concurrent.TimeUnit

/**
 * 收到短信后触发：解析短信并交由前台服务异步上传
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages.first().originatingAddress ?: ""
        val content = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val timeMillis = messages.first().timestampMillis

        // 1) 先落库（去重）
        val repo = SmsQueueRepository(context)
        val inserted = repo.enqueue(sender = sender, content = content, smsTimeMs = timeMillis)
        Log.i("SyncSMS", "sms received, sender=$sender, inserted=$inserted, ts=$timeMillis")

        // 2) 触发一次上传 Worker（网络可用时执行）
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = OneTimeWorkRequestBuilder<SmsUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(req)
        Log.i("SyncSMS", "sms upload enqueued, workId=${req.id}")

        // 3) 若用户开启了前台服务，确保服务存活（用于心跳与保活）
        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(Prefs.KEY_SERVICE_ENABLED, false)) {
            context.startForegroundService(Intent(context, SyncForegroundService::class.java))
        }
    }
}

