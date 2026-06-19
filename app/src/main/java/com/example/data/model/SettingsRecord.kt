package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings_record")
data class SettingsRecord(
    @PrimaryKey val id: Int = 1, // Single-row configuration design
    
    // Alarms Configuration
    val alarmASpeed: Double = 50.0,
    val alarmAColor: String = "#FF1744", // Neon Red/Pink
    val alarmAEnabled: Boolean = true,
    
    val alarmBSpeed: Double = 80.0,
    val alarmBColor: String = "#FF9100", // Neon Orange
    val alarmBEnabled: Boolean = true,
    
    val alarmCSpeed: Double = 120.0,
    val alarmCColor: String = "#E040FB", // Neon Violet/Magenta
    val alarmCEnabled: Boolean = true,
    
    // Sounds and Database features
    val soundAlertsEnabled: Boolean = true,
    val autoSaveTripsEnabled: Boolean = true,
    val autoSaveCoordinatesEnabled: Boolean = true,
    
    // HUD Visibility customization
    val showSpeedChart: Boolean = true,
    val showStatsPanel: Boolean = true,
    val showGForceTelemetry: Boolean = true,
    val showSensorsCompass: Boolean = true,
    
    // Theme details
    val hudColorTheme: String = "#00FFFF", // CYAN, MAGENTA, ORANGE, GREEN or Hex

    // Speed decimal settings
    val showSpeedDecimals: Boolean = true,

    // Audio Buzzer Settings
    val alarmVolume: Int = 80, // Volume 0..100
    val alarmSoundType: String = "STANDARD" // STANDARD, HIGH_PITCH, DUAL_ALERT, RADAR_BEEP
)
