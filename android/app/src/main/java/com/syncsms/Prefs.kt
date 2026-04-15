package com.syncsms

/**
 * 与 [android.content.SharedPreferences] 同步的键名，供 MainActivity、BootReceiver 等共用。
 */
object Prefs {
    const val NAME = "syncsms"
    const val KEY_SERVER = "server"
    const val KEY_DEVICE_KEY = "deviceKey"
    /** 用户点击「启动同步服务」后为 true，用于开机自启与进入应用时恢复前台服务 */
    const val KEY_SERVICE_ENABLED = "serviceEnabled"
    /** 设备 JWT Token（/api/device/auth），用于心跳与短信上传 */
    const val KEY_DEVICE_TOKEN = "deviceToken"

    /** 收件箱轮询：上次已处理的短信时间戳（ms） */
    const val KEY_LAST_SEEN_SMS_TIME_MS = "lastSeenSmsTimeMs"
}
