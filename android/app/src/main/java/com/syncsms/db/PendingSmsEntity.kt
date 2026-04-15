package com.syncsms.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 待上传短信：收到短信后先落库，确保离线/被杀进程也不会丢。
 */
@Entity(
    tableName = "pending_sms",
    indices = [
        Index(value = ["dedupeKey"], unique = true),
        Index(value = ["nextRetryAtMs"])
    ]
)
data class PendingSmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dedupeKey: String,
    val sender: String,
    val content: String,
    val smsTimeMs: Long,
    val createdAtMs: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val nextRetryAtMs: Long = 0
)

