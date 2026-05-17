package com.duoji.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duoji.app.data.local.dao.TransactionDao
import com.duoji.app.data.local.entity.TransactionEntity

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class DuojiDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: DuojiDatabase? = null

        fun getInstance(context: Context): DuojiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DuojiDatabase::class.java,
                    "duoji_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
