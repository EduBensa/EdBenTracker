package com.example.data.local

import androidx.room.*
import com.example.data.model.SettingsRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings_record WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsRecord?>

    @Query("SELECT * FROM settings_record WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOneShot(): SettingsRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsRecord)
}
