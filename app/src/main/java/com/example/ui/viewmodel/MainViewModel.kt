package com.example.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DatabaseProvider
import com.example.data.model.SettingsRecord
import com.example.data.model.TripRecord
import com.example.data.repository.TripRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class HudLatLng(val latitude: Double, val longitude: Double)

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val repository: TripRepository
    
    // Live db streams
    val allTrips: StateFlow<List<TripRecord>>
    val settingsState: StateFlow<SettingsRecord>

    // Speed states (in km/h)
    val currentSpeed = MutableStateFlow(0.0)
    val maxSpeed = MutableStateFlow(0.0)
    val avgSpeed = MutableStateFlow(0.0)
    
    // Distance & Time states
    val distanceKm = MutableStateFlow(0.0)
    val durationSeconds = MutableStateFlow(0L)
    val isTracking = MutableStateFlow(false)
    val keepScreenOn = MutableStateFlow(true)

    // Speed readings for total calculation
    private val speedHistoryPoints = mutableListOf<Double>()
    
    // Raw second-by-second data points recorded during tracking
    private val secondBySecondPointsList = java.util.Collections.synchronizedList(mutableListOf<String>())
    
    // Real-time speed graph sequence (for Canvas plotting)
    val speedGraphHistory = MutableStateFlow<List<Float>>(emptyList())
    val graphZoomPoints = MutableStateFlow(40) // Default viewport width of X-axis

    // Telemetry Sensor States
    val gForceX = MutableStateFlow(0f)
    val gForceY = MutableStateFlow(0f)
    val gForceZ = MutableStateFlow(0f)
    val compassAzimuth = MutableStateFlow(0f) // Bearing degrees (0..360)

    // Active Sensor objects
    private  var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var gravityValues = FloatArray(3)
    private var magneticValues = FloatArray(3)

    // GPS Core
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    // Track state coordinates
    val currentRoutePath = MutableStateFlow<List<HudLatLng>>(emptyList())
    private var lastLocation: Location? = null

    // Real-time simulation mode (for testing inside headless/static units)
    val isSimulating = MutableStateFlow(false)
    val simulatedTargetSpeed = MutableStateFlow(0.0)

    // Alarm state management
    val activeAlarmColor = MutableStateFlow<String?>(null) // Hex color code when triggered
    private val alarmCooldowns = mutableMapOf<String, Long>()
    private val alarmSounded = mutableMapOf<String, Boolean>(
        "A" to false,
        "B" to false,
        "C" to false
    )

    // Tone alerts
    private var toneGenerator: ToneGenerator? = null

    // Switch between radar grid map plotter vs digital compass
    val showMiniMapRadar = MutableStateFlow(true)

    // Active navigation state helper
    val activeScreen = MutableStateFlow("dashboard") // dashboard, settings, history
    val selectedTripIdForMap = MutableStateFlow<Int?>(null)
    val activeTripId = MutableStateFlow<Int?>(null)
    private var initialTripDateTime: Long = System.currentTimeMillis()

    private var tickerJob: Job? = null

    init {
        val database = DatabaseProvider.getDatabase(application)
        repository = TripRepository(database.tripDao(), database.settingsDao())
        
        allTrips = repository.allTrips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        settingsState = repository.settings.map { it ?: SettingsRecord() }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRecord()
        )

        // Initialize Tone Generator
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to create ToneGenerator", e)
        }

        // Initialize Sensors
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        registerSensors()

        // Sync initial DB defaults if settings are completely empty
        viewModelScope.launch {
            val existing = repository.getSettingsOneShot()
            if (existing == null) {
                repository.insertOrUpdateSettings(SettingsRecord())
            }
        }

        // Start GPS
        startGpsUpdates()
    }

    private fun registerSensors() {
        sensorManager?.let { sm ->
            accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Calculate G-Force by dividing acceleration in m/s^2 by standard earth gravity 9.81
                gForceX.value = event.values[0] / 9.81f
                gForceY.value = event.values[1] / 9.81f
                gForceZ.value = event.values[2] / 9.81f

                gravityValues = event.values.clone()
                updateCompassOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticValues = event.values.clone()
                updateCompassOrientation()
            }
        }
    }

    private fun updateCompassOrientation() {
        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, gravityValues, magneticValues)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(r, orientation)
            // Convert azimuth from radians to degrees (-180..180 to 0..360)
            val azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            compassAzimuth.value = (azimuthDegrees + 360f) % 360f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // LOCALIZATION SETUP
    fun startGpsUpdates() {
        if (locationCallback != null) {
            Log.d("MainViewModel", "GPS updates are already configured")
            return
        }

        val context = getApplication<Application>()
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w("MainViewModel", "GPS updates requested but permission is not granted yet")
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(0.2f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (isSimulating.value) return // If simulation is active, bypass GPS
                
                for (loc in locationResult.locations) {
                    processNewLocation(loc)
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.i("MainViewModel", "Successfully started GPS updates")
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "SecurityException requesting location updates: ${e.message}")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to register GPS updates: ${e.message}")
        }
    }

    fun stopGpsUpdates() {
        if (locationCallback != null) {
            try {
                fusedLocationClient?.removeLocationUpdates(locationCallback!!)
                Log.i("MainViewModel", "Successfully stopped GPS updates")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to stop GPS updates: ${e.message}")
            } finally {
                locationCallback = null
            }
        }
    }

    fun processNewLocation(location: Location) {
        if (!isTracking.value) return

        // In m/s -> multiply by 3.6 to get km/h
        val rawSpeed = if (location.hasSpeed()) location.speed * 3.6 else 0.0
        updateTelemetrySpeed(rawSpeed)

        // Calculate distance
        if (lastLocation != null) {
            val deltaDistanceMeters = lastLocation!!.distanceTo(location)
            distanceKm.value += (deltaDistanceMeters / 1000.0)
        }
        
        lastLocation = location

        if (settingsState.value.autoSaveCoordinatesEnabled) {
            val newPath = currentRoutePath.value.toMutableList()
            newPath.add(HudLatLng(location.latitude, location.longitude))
            currentRoutePath.value = newPath
        }
    }

    private fun updateTelemetrySpeed(speedVal: Double) {
        val finalSpeed = if (speedVal < 0.5) 0.0 else speedVal
        currentSpeed.value = finalSpeed

        // Update statistics
        if (finalSpeed > maxSpeed.value) {
            maxSpeed.value = finalSpeed
        }

        speedHistoryPoints.add(finalSpeed)
        val avg = if (speedHistoryPoints.isNotEmpty()) speedHistoryPoints.average() else 0.0
        avgSpeed.value = avg

        // Real-time plotting chart data feed
        val graphPoints = speedGraphHistory.value.toMutableList()
        graphPoints.add(finalSpeed.toFloat())
        
        // Trim elements keeping a buffer for smooth scrolling
        if (graphPoints.size > 200) {
            graphPoints.removeAt(0)
        }
        speedGraphHistory.value = graphPoints

        // Evaluate alarm transgressions
        checkAlarmThresholds(finalSpeed)
    }

    private fun checkAlarmThresholds(speed: Double) {
        val settings = settingsState.value
        var triggeredAlarmColor: String? = null

        val limitAExceeded = settings.alarmAEnabled && speed >= settings.alarmASpeed
        val limitBExceeded = settings.alarmBEnabled && speed >= settings.alarmBSpeed
        val limitCExceeded = settings.alarmCEnabled && speed >= settings.alarmCSpeed

        // Visually trigger the active color matching the highest priority/limit reached
        if (limitCExceeded) {
            triggeredAlarmColor = settings.alarmCColor
        } else if (limitBExceeded) {
            triggeredAlarmColor = settings.alarmBColor
        } else if (limitAExceeded) {
            triggeredAlarmColor = settings.alarmAColor
        }

        activeAlarmColor.value = triggeredAlarmColor

        // Latching beep audio triggers: sound once per crossing, then deactivate until dropping below the speed threshold
        // Alarm A
        if (settings.alarmAEnabled) {
            if (speed >= settings.alarmASpeed) {
                if (alarmSounded["A"] == false) {
                    playAlertSound("A")
                    alarmSounded["A"] = true
                }
            } else {
                alarmSounded["A"] = false
            }
        } else {
            alarmSounded["A"] = false
        }

        // Alarm B
        if (settings.alarmBEnabled) {
            if (speed >= settings.alarmBSpeed) {
                if (alarmSounded["B"] == false) {
                    playAlertSound("B")
                    alarmSounded["B"] = true
                }
            } else {
                alarmSounded["B"] = false
            }
        } else {
            alarmSounded["B"] = false
        }

        // Alarm C
        if (settings.alarmCEnabled) {
            if (speed >= settings.alarmCSpeed) {
                if (alarmSounded["C"] == false) {
                    playAlertSound("C")
                    alarmSounded["C"] = true
                }
            } else {
                alarmSounded["C"] = false
            }
        } else {
            alarmSounded["C"] = false
        }
    }

    private fun playAlertSound(alarmId: String) {
        val settings = settingsState.value
        if (!settings.soundAlertsEnabled) return
        try {
            val volume = settings.alarmVolume.coerceIn(0, 100)
            val generator = ToneGenerator(AudioManager.STREAM_ALARM, volume)
            
            val toneType = when (settings.alarmSoundType) {
                "HIGH_PITCH" -> ToneGenerator.TONE_CDMA_PIP
                "DUAL_ALERT" -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                "RADAR_BEEP" -> ToneGenerator.TONE_PROP_BEEP
                else -> ToneGenerator.TONE_CDMA_ABBR_ALERT // STANDARD
            }
            
            generator.startTone(toneType, 200)
            
            // Release the generator resource after warning beep triggers
            viewModelScope.launch {
                delay(300)
                try { generator.release() } catch (e: Exception) {}
            }
            Log.i("MainViewModel", "Beep Alert $alarmId played. Volume: $volume, Tone: $toneType")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to play beep: ${e.message}")
        }
    }

    // ACTIONS DECLARATION
    fun startCampaign() {
        if (isTracking.value) return
        isTracking.value = true

        lastLocation = null
        secondBySecondPointsList.clear()
        initialTripDateTime = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            val initialTrip = TripRecord(
                id = 0,
                dateTime = initialTripDateTime,
                durationSeconds = 0,
                distanceKm = 0.0,
                maxSpeed = 0.0,
                avgSpeed = 0.0,
                coordinatesPath = "",
                isReadOnly = true,
                isSimulation = isSimulating.value,
                secondBySecondData = ""
            )
            val generatedId = repository.insertTrip(initialTrip)
            activeTripId.value = generatedId.toInt()
        }

        tickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                durationSeconds.value += 1

                // Snapshot telemetry exact 1 second tabular recording
                val currentSpeedVal = currentSpeed.value
                val currentDistanceVal = distanceKm.value
                val isSim = isSimulating.value
                
                val currentDate = Date()
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDate)
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDate)
                
                val routePoints = currentRoutePath.value
                val coordStr = if (routePoints.isNotEmpty()) {
                    val lastPoint = routePoints.last()
                    String.format(Locale.US, "Lat: %.6f, Lng: %.6f", lastPoint.latitude, lastPoint.longitude)
                } else {
                    if (isSim) "COORD_SIMULADAS" else "COORD_SIN_GPS"
                }

                // FECHA | HORA | VELOCIDAD | KM RECORRIDOS | COORDENADAS
                val snapshotLine = String.format(Locale.US, "%s|%s|%.1f|%.3f|%s", dateStr, timeStr, currentSpeedVal, currentDistanceVal, coordStr)
                secondBySecondPointsList.add(snapshotLine)

                // If simulating, adjust currentSpeed smoothly towards simulated target
                if (isSim) {
                    val current = currentSpeed.value
                    val target = simulatedTargetSpeed.value
                    val next = current + (target - current) * 0.25
                    
                    // Add micro-vibration noise to simulated speed for real-time sensor effect
                    val noise = (Math.random() - 0.5) * 0.4
                    val speedWithNoise = (next + noise).coerceAtLeast(0.0)

                    withContext(Dispatchers.Main) {
                        updateTelemetrySpeed(speedWithNoise)
                        
                        // Fake coordinate shifting to show route drawing in live canvas radar
                        val path = currentRoutePath.value.toMutableList()
                        val lastPoint = path.lastOrNull() ?: HudLatLng(-12.0463, -77.0427) // Standard fallback pivot
                        
                        // Scale simulated vector displacement proportional to speed
                        val displacement = (speedWithNoise / 3600.0) * 0.009 // arbitrary scaler
                        val nextPoint = HudLatLng(
                            lastPoint.latitude + (Math.sin(System.currentTimeMillis() / 15000.0) * displacement),
                            lastPoint.longitude + (Math.cos(System.currentTimeMillis() / 15000.0) * displacement)
                        )
                        
                        // Odometer distance step
                        distanceKm.value += (speedWithNoise / 3600.0)

                        if (settingsState.value.autoSaveCoordinatesEnabled) {
                            path.add(nextPoint)
                            currentRoutePath.value = path
                        }
                    }
                }

                // Real-time persistent increments (immediate save)
                val currentId = activeTripId.value
                if (currentId != null) {
                    val duration = durationSeconds.value
                    val dist = distanceKm.value
                    val topSpeed = maxSpeed.value
                    val avg = avgSpeed.value
                    val tabData = secondBySecondPointsList.toList().joinToString("\n")
                    val coordsString = currentRoutePath.value.joinToString(";") { "${it.latitude},${it.longitude}" }

                    viewModelScope.launch(Dispatchers.IO) {
                        repository.insertTrip(
                            TripRecord(
                                id = currentId,
                                dateTime = initialTripDateTime,
                                durationSeconds = duration,
                                distanceKm = dist,
                                maxSpeed = topSpeed,
                                avgSpeed = avg,
                                coordinatesPath = coordsString,
                                isReadOnly = true,
                                isSimulation = isSim,
                                secondBySecondData = tabData
                            )
                        )
                    }
                }
            }
        }
    }

    fun pauseCampaign() {
        isTracking.value = false
        tickerJob?.cancel()
    }

    fun resetCampaign() {
        pauseCampaign()
        
        // Auto-save prior campaign on reset if enabled
        if (settingsState.value.autoSaveTripsEnabled && (distanceKm.value > 0.01 || durationSeconds.value > 2)) {
            // Already updated in database in real-time. Discard temporary reference
            activeTripId.value = null
        } else {
            // Unsaved or short trip: discard from database
            val currentId = activeTripId.value
            if (currentId != null) {
                deleteTrip(currentId)
                activeTripId.value = null
            }
        }

        // Reset variables
        currentSpeed.value = 0.0
        maxSpeed.value = 0.0
        avgSpeed.value = 0.0
        distanceKm.value = 0.0
        durationSeconds.value = 0
        speedHistoryPoints.clear()
        speedGraphHistory.value = emptyList()
        currentRoutePath.value = emptyList()
        activeAlarmColor.value = null
        secondBySecondPointsList.clear()
    }

    fun resetTripStatsOnly() {
        maxSpeed.value = 0.0
        avgSpeed.value = 0.0
        distanceKm.value = 0.0
        secondBySecondPointsList.clear()
    }

    fun saveTripToDatabase() {
        val duration = durationSeconds.value
        val dist = distanceKm.value
        val topSpeed = maxSpeed.value
        val avg = avgSpeed.value
        val isSim = isSimulating.value
        val tabularData = secondBySecondPointsList.toList().joinToString("\n")

        val coordsString = currentRoutePath.value.joinToString(";") { "${it.latitude},${it.longitude}" }

        viewModelScope.launch {
            repository.insertTrip(
                TripRecord(
                    durationSeconds = duration,
                    distanceKm = dist,
                    maxSpeed = topSpeed,
                    avgSpeed = avg,
                    coordinatesPath = coordsString,
                    isReadOnly = true,
                    isSimulation = isSim,
                    secondBySecondData = tabularData
                )
            )
        }
    }

    fun deleteTrip(id: Int) {
        viewModelScope.launch {
            repository.deleteTripById(id)
        }
    }

    fun clearAllTrips() {
        viewModelScope.launch {
            repository.deleteAllTrips()
        }
    }

    // Graph Viewport zoom changes
    fun increaseZoom() {
        graphZoomPoints.value = (graphZoomPoints.value - 10).coerceAtLeast(10)
    }

    fun decreaseZoom() {
        graphZoomPoints.value = (graphZoomPoints.value + 10).coerceAtMost(180)
    }

    // Toggle simulated speeds
    fun setSimulation(enabled: Boolean) {
        isSimulating.value = enabled
        if (!enabled) {
            currentSpeed.value = 0.0
        } else {
            simulatedTargetSpeed.value = 45.0
        }
    }

    fun setSimulatedSpeedTarget(speed: Double) {
        simulatedTargetSpeed.value = speed
    }

    // Set customization options
    fun updateSettings(newSettings: SettingsRecord) {
        viewModelScope.launch {
            repository.insertOrUpdateSettings(newSettings)
        }
    }

    // CSV REPORT EXPORT SYSTEM
    fun exportTripsToCsv(context: Context): Uri? {
        val tripsList = allTrips.value.sortedBy { it.dateTime }
        if (tripsList.isEmpty()) return null

        val stringBuffer = java.lang.StringBuilder()
        // Header using semicolon for Spanish region CSV compatibility in Excel
        stringBuffer.append("FECHA;HORA;VELOCIDAD;KM RECORRIDOS;COORDENADAS;RESUMEN\n")

        for (trip in tripsList) {
            val rows = trip.secondBySecondData.split("\n").filter { it.isNotBlank() }
            
            val hrs = trip.durationSeconds / 3600
            val mns = (trip.durationSeconds % 3600) / 60
            val scs = trip.durationSeconds % 60
            val formattedDuration = String.format(Locale.US, "%02d:%02d:%02d", hrs, mns, scs)
            
            val tripType = if (trip.isSimulation) "SIMULACIÓN" else "VIAJE REAL"
            val summaryText = "REGISTRO #${trip.id} [$tripType] - Distancia total: ${String.format(Locale.US, "%.3f", trip.distanceKm)} Km | Duracion: $formattedDuration | Vel Maxima: ${String.format(Locale.US, "%.1f", trip.maxSpeed)} Km/h"

            if (rows.isNotEmpty()) {
                rows.forEachIndexed { index, row ->
                    val cols = if (row.contains(";")) row.split(";") else if (row.contains("|")) row.split("|") else row.split(",")
                    if (cols.size >= 5) {
                        val fechaValue = cols[0].trim()
                        val horaValue = cols[1].trim()
                        val velValue = cols[2].trim()
                        val kmValue = cols[3].trim()
                        val coordValue = if (row.contains("|") && cols.size >= 6) {
                            "${cols[4].trim()} - ${cols[5].trim()}"
                        } else {
                            cols[4].trim()
                        }
                        
                        // Quoted string fields to prevent parsing issues
                        val resumeCell = if (index == 0) "\"$summaryText\"" else ""
                        stringBuffer.append("$fechaValue;$horaValue;$velValue;$kmValue;\"$coordValue\";$resumeCell\n")
                    }
                }
            } else {
                // Fallback for legacy trips without second-by-second records
                val legacyDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(trip.dateTime))
                val legacyTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(trip.dateTime))
                val legacyCoords = trip.coordinatesPath.replace(";", " | ")
                stringBuffer.append("$legacyDate;$legacyTime;${String.format(Locale.US, "%.1f", trip.avgSpeed)};${String.format(Locale.US, "%.3f", trip.distanceKm)};\"$legacyCoords\";\"$summaryText\"\n")
            }
        }

        return try {
            val cacheDirectory = File(context.cacheDir, "exports")
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs()
            }
            val reportFile = File(cacheDirectory, "HUD_Odom_Telemetry_Log_${System.currentTimeMillis()}.csv")
            val outputStream = FileOutputStream(reportFile)
            outputStream.write(stringBuffer.toString().toByteArray())
            outputStream.close()

            // Security: Protect file making it strictly read-only after creation
            try {
                reportFile.setWritable(false)
                reportFile.setReadOnly()
            } catch (e: Exception) {
                // ignore if not supported by local file system sandbox
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )
        } catch (e: Exception) {
            Log.e("MainViewModel", "CSV Generation crashed: ${e.message}")
            null
        }
    }

    suspend fun exportSingleTripToCsv(context: Context, tripId: Int): Uri? = withContext(Dispatchers.IO) {
        val trip = repository.getTripById(tripId) ?: return@withContext null

        val stringBuffer = java.lang.StringBuilder()
        // Header using semicolon for Spanish region CSV compatibility in Excel
        stringBuffer.append("FECHA;HORA;VELOCIDAD;KM RECORRIDOS;COORDENADAS;RESUMEN\n")

        val rows = trip.secondBySecondData.split("\n").filter { it.isNotBlank() }
        
        val hrs = trip.durationSeconds / 3600
        val mns = (trip.durationSeconds % 3600) / 60
        val scs = trip.durationSeconds % 60
        val formattedDuration = String.format(Locale.US, "%02d:%02d:%02d", hrs, mns, scs)
        
        val tripType = if (trip.isSimulation) "SIMULACIÓN" else "VIAJE REAL"
        val summaryText = "REGISTRO #${trip.id} [$tripType] - Distancia total: ${String.format(Locale.US, "%.3f", trip.distanceKm)} Km | Duracion: $formattedDuration | Vel Maxima: ${String.format(Locale.US, "%.1f", trip.maxSpeed)} Km/h"

        if (rows.isNotEmpty()) {
            rows.forEachIndexed { index, row ->
                val cols = if (row.contains(";")) row.split(";") else if (row.contains("|")) row.split("|") else row.split(",")
                if (cols.size >= 5) {
                    val fechaValue = cols[0].trim()
                    val horaValue = cols[1].trim()
                    val velValue = cols[2].trim()
                    val kmValue = cols[3].trim()
                    val coordValue = if (row.contains("|") && cols.size >= 6) {
                        "${cols[4].trim()} - ${cols[5].trim()}"
                    } else {
                        cols[4].trim()
                    }
                    
                    val resumeCell = if (index == 0) "\"$summaryText\"" else ""
                    stringBuffer.append("$fechaValue;$horaValue;$velValue;$kmValue;\"$coordValue\";$resumeCell\n")
                }
            }
        } else {
            val legacyDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(trip.dateTime))
            val legacyTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(trip.dateTime))
            val legacyCoords = trip.coordinatesPath.replace(";", " | ")
            stringBuffer.append("$legacyDate;$legacyTime;${String.format(Locale.US, "%.1f", trip.avgSpeed)};${String.format(Locale.US, "%.3f", trip.distanceKm)};\"$legacyCoords\";\"$summaryText\"\n")
        }

        try {
            val cacheDirectory = File(context.cacheDir, "exports")
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs()
            }
            val reportFile = File(cacheDirectory, "HUD_Trip_${tripId}_Telemetry_${System.currentTimeMillis()}.csv")
            val outputStream = FileOutputStream(reportFile)
            outputStream.write(stringBuffer.toString().toByteArray())
            outputStream.close()

            try {
                reportFile.setWritable(false)
                reportFile.setReadOnly()
            } catch (e: Exception) {
                // ignore
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )
        } catch (e: Exception) {
            Log.e("MainViewModel", "CSV Generation for single trip crashed: ${e.message}")
            null
        }
    }

    suspend fun exportAndOpenMap(context: Context, tripId: Int): Uri? = withContext(Dispatchers.IO) {
        val trip = repository.getTripById(tripId) ?: return@withContext null
        
        // 1. Generate XML/CSV String representation of details
        val stringBuffer = java.lang.StringBuilder()
        stringBuffer.append("FECHA;HORA;VELOCIDAD;KM RECORRIDOS;COORDENADAS;RESUMEN\n")

        val rows = trip.secondBySecondData.split("\n").filter { it.isNotBlank() }
        
        val hrs = trip.durationSeconds / 3600
        val mns = (trip.durationSeconds % 3600) / 60
        val scs = trip.durationSeconds % 60
        val formattedDuration = String.format(Locale.US, "%02d:%02d:%02d", hrs, mns, scs)
        
        val tripType = if (trip.isSimulation) "SIMULACIÓN" else "VIAJE REAL"
        val summaryText = "REGISTRO #${trip.id} [$tripType] - Distancia total: ${String.format(Locale.US, "%.3f", trip.distanceKm)} Km | Duracion: $formattedDuration | Vel Maxima: ${String.format(Locale.US, "%.1f", trip.maxSpeed)} Km/h"

        if (rows.isNotEmpty()) {
            rows.forEachIndexed { index, row ->
                val cols = if (row.contains(";")) row.split(";") else if (row.contains("|")) row.split("|") else row.split(",")
                if (cols.size >= 5) {
                    val fechaValue = cols[0].trim()
                    val horaValue = cols[1].trim()
                    val velValue = cols[2].trim()
                    val kmValue = cols[3].trim()
                    val coordValue = if (row.contains("|") && cols.size >= 6) {
                        "${cols[4].trim()} - ${cols[5].trim()}"
                    } else {
                        cols[4].trim()
                    }
                    
                    val resumeCell = if (index == 0) "\"$summaryText\"" else ""
                    stringBuffer.append("$fechaValue;$horaValue;$velValue;$kmValue;\"$coordValue\";$resumeCell\n")
                }
            }
        } else {
            val legacyDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(trip.dateTime))
            val legacyTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(trip.dateTime))
            val legacyCoords = trip.coordinatesPath.replace(";", " | ")
            stringBuffer.append("$legacyDate;$legacyTime;${String.format(Locale.US, "%.1f", trip.avgSpeed)};${String.format(Locale.US, "%.3f", trip.distanceKm)};\"$legacyCoords\";\"$summaryText\"\n")
        }
        val csvData = stringBuffer.toString()

        // 2. Read template HTML from app assets
        val htmlTemplate = try {
            context.assets.open("visualizador.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to open visualizador.html from assets: ${e.message}")
            return@withContext null
        }

        // 3. Inject safe escapes into multi-line backticks template injection
        val escapedCvs = csvData.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
        val customizedHtml = htmlTemplate.replace("<!-- ANTENA_DATA_PLACEHOLDER -->", escapedCvs)

        // 4. Save to exports cache directory
        try {
            val cacheDirectory = File(context.cacheDir, "exports")
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs()
            }
            val mapFile = File(cacheDirectory, "HUD_Trip_${tripId}_Map_${System.currentTimeMillis()}.html")
            val outputStream = FileOutputStream(mapFile)
            outputStream.write(customizedHtml.toByteArray(Charsets.UTF_8))
            outputStream.close()

            // 5. Build Content URI with Package fileprovider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                mapFile
            )
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to compile visualizer target HTML file: ${e.message}")
            null
        }
    }

    suspend fun getTripCsvString(tripId: Int): String? = withContext(Dispatchers.IO) {
        val trip = repository.getTripById(tripId) ?: return@withContext null
        
        val stringBuffer = java.lang.StringBuilder()
        stringBuffer.append("FECHA;HORA;VELOCIDAD;KM RECORRIDOS;COORDENADAS;RESUMEN\n")

        val rows = trip.secondBySecondData.split("\n").filter { it.isNotBlank() }
        
        val hrs = trip.durationSeconds / 3600
        val mns = (trip.durationSeconds % 3600) / 60
        val scs = trip.durationSeconds % 60
        val formattedDuration = String.format(Locale.US, "%02d:%02d:%02d", hrs, mns, scs)
        
        val tripType = if (trip.isSimulation) "SIMULACIÓN" else "VIAJE REAL"
        val summaryText = "REGISTRO #${trip.id} [$tripType] - Distancia total: ${String.format(Locale.US, "%.3f", trip.distanceKm)} Km | Duracion: $formattedDuration | Vel Maxima: ${String.format(Locale.US, "%.1f", trip.maxSpeed)} Km/h"

        if (rows.isNotEmpty()) {
            rows.forEachIndexed { index, row ->
                val cols = if (row.contains(";")) row.split(";") else if (row.contains("|")) row.split("|") else row.split(",")
                if (cols.size >= 5) {
                    val fechaValue = cols[0].trim()
                    val horaValue = cols[1].trim()
                    val velValue = cols[2].trim()
                    val kmValue = cols[3].trim()
                    val coordValue = if (row.contains("|") && cols.size >= 6) {
                        "${cols[4].trim()} - ${cols[5].trim()}"
                    } else {
                        cols[4].trim()
                    }
                    
                    val resumeCell = if (index == 0) "\"$summaryText\"" else ""
                    stringBuffer.append("$fechaValue;$horaValue;$velValue;$kmValue;\"$coordValue\";$resumeCell\n")
                }
            }
        } else {
            val legacyDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(trip.dateTime))
            val legacyTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(trip.dateTime))
            val legacyCoords = trip.coordinatesPath.replace(";", " | ")
            stringBuffer.append("$legacyDate;$legacyTime;${String.format(Locale.US, "%.1f", trip.avgSpeed)};${String.format(Locale.US, "%.3f", trip.distanceKm)};\"$legacyCoords\";\"$summaryText\"\n")
        }
        stringBuffer.toString()
    }

    override fun onCleared() {
        super.onCleared()
        unregisterSensors()
        stopGpsUpdates()
        tickerJob?.cancel()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
