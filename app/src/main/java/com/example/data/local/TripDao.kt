package com.example.data.local

import androidx.room.*
import com.example.data.model.TripRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_records ORDER BY dateTime DESC")
    fun getAllTrips(): Flow<List<TripRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripRecord): Long

    @Query("DELETE FROM trip_records WHERE id = :id")
    suspend fun deleteTripById(id: Int)

    @Query("SELECT * FROM trip_records WHERE id = :id")
    suspend fun getTripById(id: Int): TripRecord?

    @Query("DELETE FROM trip_records")
    suspend fun deleteAllTrips()
}
