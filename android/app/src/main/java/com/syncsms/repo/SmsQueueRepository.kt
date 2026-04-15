package com.syncsms.repo

import android.content.Context
import com.syncsms.db.AppDatabase
import com.syncsms.db.PendingSmsEntity
import java.security.MessageDigest

class SmsQueueRepository(private val context: Context) {
    private val dao = AppDatabase.get(context).pendingSmsDao()

    fun enqueue(sender: String, content: String, smsTimeMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val key = sha256("$sender|$smsTimeMs|$content")
        val id = dao.insertIgnore(
            PendingSmsEntity(
                dedupeKey = key,
                sender = sender,
                content = content,
                smsTimeMs = smsTimeMs,
                createdAtMs = now,
                nextRetryAtMs = 0
            )
        )
        return id != -1L
    }

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }
}

