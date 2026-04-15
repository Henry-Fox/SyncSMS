package com.syncsms

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * App 端最小网络客户端（不引入协程，便于快速闭环）
 */
object ApiClient {

    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    data class HeartbeatRequest(val batteryPercent: Int?, val charging: Boolean?)
    data class SmsItem(val sender: String, val content: String, val smsTime: String)
    data class SmsBatchRequest(val messages: List<SmsItem>)

    fun resolveBaseUrl(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        if (s.endsWith("/")) s = s.dropLast(1)
        // 兼容用户输入到 /api（我们会自动拼接 /api/...）
        if (s.endsWith("/api")) s = s.removeSuffix("/api")
        return s
    }

    private fun parseJsonObject(text: String): Map<String, Any?> {
        val t = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(text, t) ?: emptyMap()
    }

    fun deviceAuth(serverBaseUrl: String, deviceKey: String): String {
        val url = "${resolveBaseUrl(serverBaseUrl)}/api/device/auth"
        val bodyJson = gson.toJson(mapOf("deviceKey" to deviceKey))
        val req = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("设备认证失败：HTTP ${resp.code}")
            val parsed = parseJsonObject(text)
            val code = (parsed["code"] as? Double)?.toInt()
            if (code != 200) {
                throw RuntimeException("设备认证失败：${parsed["message"] ?: "请求失败"}")
            }
            val data = parsed["data"]
            if (data is String) return data
            throw RuntimeException("设备认证失败：响应格式异常")
        }
    }

    fun heartbeat(serverBaseUrl: String, token: String, payload: HeartbeatRequest) {
        val url = "${resolveBaseUrl(serverBaseUrl)}/api/device/heartbeat"
        val bodyJson = gson.toJson(payload)
        val req = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(json))
            .addHeader("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                throw RuntimeException("心跳失败：HTTP ${resp.code} ${body.take(200)}")
            }
        }
    }

    fun batchUploadSms(serverBaseUrl: String, token: String, messages: List<SmsItem>) {
        val url = "${resolveBaseUrl(serverBaseUrl)}/api/sms/batch"
        val bodyJson = gson.toJson(SmsBatchRequest(messages))
        val req = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(json))
            .addHeader("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("短信上传失败：HTTP ${resp.code} ${text.take(200)}")
            val parsed = parseJsonObject(text)
            val code = (parsed["code"] as? Double)?.toInt()
            if (code != 200) {
                throw RuntimeException("短信上传失败：${parsed["message"] ?: "请求失败"}")
            }
        }
    }

    fun getPrefs(context: Context) = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
}

