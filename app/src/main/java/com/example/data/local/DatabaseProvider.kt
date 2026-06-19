package com.example.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cyberhud_odometer_v1.db"
            )
            .fallbackToDestructiveMigration()
            .build()
            .also { instance = it }
        }
    }
}
