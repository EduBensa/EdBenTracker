package com.example.data.repository

import com.example.data.local.TripDao
import com.example.data.local.SettingsDao
import com.example.data.model.TripRecord
import com.example.data.model.SettingsRecord
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao,
    private val settingsDao: SettingsDao
) {
    val allTrips: Flow<List<TripRecord>> = tripDao.getAllTrips()
    val settings: Flow<SettingsRecord?> = settingsDao.getSettings()

    suspend fun getSettingsOneShot(): SettingsRecord? = settingsDao.getSettingsOneShot()

    suspend fun getTripById(id: Int): TripRecord? {
        return tripDao.getTripById(id)
    }

    suspend fun insertTrip(trip: TripRecord): Long {
        return tripDao.insertTrip(trip)
    }

    suspend fun deleteTripById(id: Int) {
        tripDao.deleteTripById(id)
    }

    suspend fun deleteAllTrips() {
        tripDao.deleteAllTrips()
    }

    suspend fun insertOrUpdateSettings(settingsRecord: SettingsRecord) {
        settingsDao.insertOrUpdateSettings(settingsRecord)
    }
}
