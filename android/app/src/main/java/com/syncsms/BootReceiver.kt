package com.syncsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机完成后，若用户已开启同步服务，则自动拉起前台服务，避免重启后进程未运行。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Prefs.KEY_SERVICE_ENABLED, false)) return

        val serviceIntent = Intent(context, SyncForegroundService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
