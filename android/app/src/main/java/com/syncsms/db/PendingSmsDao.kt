package com.syncsms.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PendingSmsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(entity: PendingSmsEntity): Long

    @Query(
        """
        SELECT * FROM pending_sms
        WHERE nextRetryAtMs <= :nowMs
        ORDER BY createdAtMs ASC
        LIMIT :limit
        """
    )
    fun queryReady(nowMs: Long, limit: Int): List<PendingSmsEntity>

    @Query("DELETE FROM pending_sms WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Long>): Int

    @Query(
        """
        UPDATE pending_sms
        SET attemptCount = :attemptCount,
            lastError = :lastError,
            nextRetryAtMs = :nextRetryAtMs
        WHERE id IN (:ids)
        """
    )
    fun updateRetry(ids: List<Long>, attemptCount: Int, lastError: String?, nextRetryAtMs: Long): Int

    @Transaction
    fun markRetry(entities: List<PendingSmsEntity>, lastError: String?, nextRetryAtMs: Long) {
        if (entities.isEmpty()) return
        val ids = entities.map { it.id }
        val nextAttempt = (entities.maxOfOrNull { it.attemptCount } ?: 0) + 1
        updateRetry(ids, nextAttempt, lastError?.take(4000), nextRetryAtMs)
    }
}

