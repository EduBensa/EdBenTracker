package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_records")
data class TripRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateTime: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val distanceKm: Double,
    val maxSpeed: Double,
    val avgSpeed: Double,
    val coordinatesPath: String, // Format: "lat,lng;lat,lng;..."
    val isReadOnly: Boolean = true, // Read-only security flag
    val isSimulation: Boolean = false, // Classifies "Viaje" (GPS real) vs "Simulación"
    val secondBySecondData: String = "" // Semicolon & Line-break separated tabular string raw data
)
