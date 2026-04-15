package com.syncsms.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PendingSmsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingSmsDao(): PendingSmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            val existing = INSTANCE
            if (existing != null) return existing
            return synchronized(this) {
                val again = INSTANCE
                if (again != null) again
                else Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncsms.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

