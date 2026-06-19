package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.TripRecord
import com.example.data.model.SettingsRecord

@Database(
    entities = [TripRecord::class, SettingsRecord::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun settingsDao(): SettingsDao
}
