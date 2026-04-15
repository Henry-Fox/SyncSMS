package com.syncsms.work

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.syncsms.ApiClient
import com.syncsms.Prefs
import com.syncsms.db.AppDatabase
import com.syncsms.db.PendingSmsEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class SmsUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    private val dao = AppDatabase.get(appContext).pendingSmsDao()
    private val prefs = appContext.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    override fun doWork(): Result {
        val server = prefs.getString(Prefs.KEY_SERVER, "")?.trim().orEmpty()
        val deviceKey = prefs.getString(Prefs.KEY_DEVICE_KEY, "")?.trim().orEmpty()
        if (server.isEmpty() || deviceKey.isEmpty()) {
            Log.w("SyncSMS", "upload skipped: server/deviceKey not set")
            return Result.retry()
        }

        Log.i("SyncSMS", "upload worker start, server=$server")
        val batch = dao.queryReady(System.currentTimeMillis(), 50)
        if (batch.isEmpty()) return Result.success()

        var token = prefs.getString(Prefs.KEY_DEVICE_TOKEN, "")?.trim().orEmpty()
        try {
            if (token.isEmpty()) {
                Log.i("SyncSMS", "upload auth start, server=$server")
                token = ApiClient.deviceAuth(server, deviceKey)
                prefs.edit().putString(Prefs.KEY_DEVICE_TOKEN, token).apply()
            }
            uploadOnce(server, token, batch)
            dao.deleteByIds(batch.map { it.id })
            Log.i("SyncSMS", "uploaded ${batch.size} sms")
            return Result.success()
        } catch (e: Exception) {
            val msg = e.message ?: "unknown"
            Log.w("SyncSMS", "upload failed: $msg")

            // token 可能失效：清掉并让下次重新认证
            if (msg.contains("401") || msg.contains("403")) {
                prefs.edit().remove(Prefs.KEY_DEVICE_TOKEN).apply()
            }

            val delayMs = computeBackoffMs(batch)
            val nextRetryAt = System.currentTimeMillis() + delayMs
            dao.markRetry(batch, msg, nextRetryAt)
            return Result.retry()
        }
    }

    private fun uploadOnce(server: String, token: String, batch: List<PendingSmsEntity>) {
        val items = batch.map {
            ApiClient.SmsItem(
                sender = it.sender,
                content = it.content,
                smsTime = fmt.format(Date(it.smsTimeMs))
            )
        }
        ApiClient.batchUploadSms(server, token, items)
    }

    private fun computeBackoffMs(batch: List<PendingSmsEntity>): Long {
        val attempt = (batch.maxOfOrNull { it.attemptCount } ?: 0) + 1
        val base = 10_000L
        val max = 5 * 60_000L
        val exp = base * (1L shl min(attempt, 6)) // 10s * 2^n, capped by max
        return min(exp, max)
    }
}

