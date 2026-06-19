package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SettingsRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val currentThemeColor = if (settings.hudColorTheme.startsWith("#")) {
        try { Color(android.graphics.Color.parseColor(settings.hudColorTheme)) } catch (e: Exception) { NeonCyan }
    } else {
        when (settings.hudColorTheme) {
            "CYAN" -> NeonCyan
            "MAGENTA" -> NeonPurple
            "ORANGE" -> NeonOrange
            "GREEN" -> NeonGreen
            else -> NeonCyan
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Terminal Vibe
            Column {
                Text(
                    text = "SISTEMA DE AJUSTES",
                    color = currentThemeColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "CONFIGURACIÓN Y CALIBRACIÓN DE CABINA",
                    color = currentThemeColor.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(currentThemeColor.copy(alpha = 0.2f))
            )

            // CATEGORY 1: HUD VISUAL THEME SELECTION
            SettingsCategoryCard(title = "01 - ESQUEMA DE COLORES DEL HUD", themeColor = currentThemeColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PALETA DE DISEÑADOR DE COLOR HUD:",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val neonPaletteColors = listOf(
                        "#00FFFF" to "CIAN",
                        "#E040FB" to "MAGENTA",
                        "#FF9100" to "NARANJA",
                        "#00E676" to "VERDE",
                        "#FF1744" to "ROJO",
                        "#FFEA00" to "AMARILLO",
                        "#2979FF" to "AZUL",
                        "#FFFFFF" to "BLANCO"
                    )
                    
                    // Row 1 of Theme palette
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        neonPaletteColors.take(4).forEach { (hexCode, colorName) ->
                            val colorValue = try { Color(android.graphics.Color.parseColor(hexCode)) } catch (e: Exception) { currentThemeColor }
                            val isSelected = settings.hudColorTheme.equals(hexCode, ignoreCase = true) ||
                                (hexCode == "#00FFFF" && settings.hudColorTheme == "CYAN") ||
                                (hexCode == "#E040FB" && settings.hudColorTheme == "MAGENTA") ||
                                (hexCode == "#FF9100" && settings.hudColorTheme == "ORANGE") ||
                                (hexCode == "#00E676" && settings.hudColorTheme == "GREEN")
                                
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) colorValue.copy(alpha = 0.12f) else CyberBlack, RoundedCornerShape(2.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) colorValue else Color.Gray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .clickable { viewModel.updateSettings(settings.copy(hudColorTheme = hexCode)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(colorValue, RoundedCornerShape(1.dp))
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = colorName,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Row 2 of Theme palette
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        neonPaletteColors.drop(4).forEach { (hexCode, colorName) ->
                            val colorValue = try { Color(android.graphics.Color.parseColor(hexCode)) } catch (e: Exception) { currentThemeColor }
                            val isSelected = settings.hudColorTheme.equals(hexCode, ignoreCase = true)
                                
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) colorValue.copy(alpha = 0.12f) else CyberBlack, RoundedCornerShape(2.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) colorValue else Color.Gray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .clickable { viewModel.updateSettings(settings.copy(hudColorTheme = hexCode)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(colorValue, RoundedCornerShape(1.dp))
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = colorName,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Parse current HSV values for manual picker
                    val hsv = remember(settings.hudColorTheme) {
                        val array = FloatArray(3)
                        val cleanThemeHex = when (settings.hudColorTheme) {
                            "CYAN" -> "#00FFFF"
                            "MAGENTA" -> "#E040FB"
                            "ORANGE" -> "#FF9100"
                            "GREEN" -> "#00E676"
                            else -> if (settings.hudColorTheme.startsWith("#")) settings.hudColorTheme else "#00FFFF"
                        }
                        try {
                            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(cleanThemeHex), array)
                        } catch (e: Exception) {
                            array[0] = 180f
                            array[1] = 1f
                            array[2] = 1f
                        }
                        array
                    }
                    var localHue by remember(hsv[0]) { mutableStateOf(hsv[0]) }
                    var localSat by remember(hsv[1]) { mutableStateOf(hsv[1]) }
                    var localVal by remember(hsv[2]) { mutableStateOf(hsv[2]) }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "SELECTOR MANUAL DE COLOR HUD (DESLICE CON EL DEDO):",
                        color = currentThemeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack, RoundedCornerShape(2.dp))
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display active color bubble / panel
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(currentThemeColor, RoundedCornerShape(2.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            val currentHexDisplay = String.format("#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(localHue, localSat, localVal)))
                            Text(
                                text = currentHexDisplay,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Hue, Saturation, Value sliders
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Hue slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "TONO",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(55.dp)
                                )
                                Slider(
                                    value = localHue,
                                    onValueChange = { h ->
                                        localHue = h
                                        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, localSat, localVal))
                                        val hexStr = String.format("#%06X", 0xFFFFFF and argb)
                                        viewModel.updateSettings(settings.copy(hudColorTheme = hexStr))
                                    },
                                    valueRange = 0f..360f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = currentThemeColor,
                                        activeTrackColor = currentThemeColor,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f).height(20.dp).testTag("manual_hue_slider")
                                )
                            }
                            
                            // Saturation slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SATURAR",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(55.dp)
                                )
                                Slider(
                                    value = localSat,
                                    onValueChange = { s ->
                                        localSat = s
                                        val argb = android.graphics.Color.HSVToColor(floatArrayOf(localHue, s, localVal))
                                        val hexStr = String.format("#%06X", 0xFFFFFF and argb)
                                        viewModel.updateSettings(settings.copy(hudColorTheme = hexStr))
                                    },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = currentThemeColor,
                                        activeTrackColor = currentThemeColor,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f).height(20.dp).testTag("manual_sat_slider")
                                )
                            }
                            
                            // Value (Brightness) slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BRILLO",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(55.dp)
                                )
                                Slider(
                                    value = localVal,
                                    onValueChange = { v ->
                                        localVal = v
                                        val argb = android.graphics.Color.HSVToColor(floatArrayOf(localHue, localSat, v))
                                        val hexStr = String.format("#%06X", 0xFFFFFF and argb)
                                        viewModel.updateSettings(settings.copy(hudColorTheme = hexStr))
                                    },
                                    valueRange = 0.2f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = currentThemeColor,
                                        activeTrackColor = currentThemeColor,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f).height(20.dp).testTag("manual_val_slider")
                                )
                            }
                        }
                    }
                }
            }

            // CATEGORY 2: ALARM PROGRAMMING (A, B, C)
            SettingsCategoryCard(title = "02 - PARÁMETROS DE ALARMAS DE VELOCIDAD", themeColor = currentThemeColor) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ALARM A CONFIG
                    AlarmConfigRow(
                        label = "LÍMITE ALARMA A",
                        speedLimit = settings.alarmASpeed,
                        isEnabled = settings.alarmAEnabled,
                        hexColor = settings.alarmAColor,
                        onSpeedChange = { viewModel.updateSettings(settings.copy(alarmASpeed = it)) },
                        onToggle = { viewModel.updateSettings(settings.copy(alarmAEnabled = it)) },
                        onColorChange = { viewModel.updateSettings(settings.copy(alarmAColor = it)) },
                        themeColor = currentThemeColor,
                        tagPrefix = "alarm_a"
                    )

                    // ALARM B CONFIG
                    AlarmConfigRow(
                        label = "LÍMITE ALARMA B",
                        speedLimit = settings.alarmBSpeed,
                        isEnabled = settings.alarmBEnabled,
                        hexColor = settings.alarmBColor,
                        onSpeedChange = { viewModel.updateSettings(settings.copy(alarmBSpeed = it)) },
                        onToggle = { viewModel.updateSettings(settings.copy(alarmBEnabled = it)) },
                        onColorChange = { viewModel.updateSettings(settings.copy(alarmBColor = it)) },
                        themeColor = currentThemeColor,
                        tagPrefix = "alarm_b"
                    )

                    // ALARM C CONFIG
                    AlarmConfigRow(
                        label = "LÍMITE ALARMA C",
                        speedLimit = settings.alarmCSpeed,
                        isEnabled = settings.alarmCEnabled,
                        hexColor = settings.alarmCColor,
                        onSpeedChange = { viewModel.updateSettings(settings.copy(alarmCSpeed = it)) },
                        onToggle = { viewModel.updateSettings(settings.copy(alarmCEnabled = it)) },
                        onColorChange = { viewModel.updateSettings(settings.copy(alarmCColor = it)) },
                        themeColor = currentThemeColor,
                        tagPrefix = "alarm_c"
                    )
                }
            }

            // CATEGORY 3: HARDWARE METRIC AND SAVING SETTINGS
            SettingsCategoryCard(title = "03 - INTEGRACIÓN DE AUDIO Y REPOSITORIO", themeColor = currentThemeColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SensorSwitchRow(
                        label = "PÍTIDO DE ALERTA DE AUDIO",
                        description = "Emite un pítido al superar los límites de velocidad",
                        checked = settings.soundAlertsEnabled,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(soundAlertsEnabled = it)) },
                        themeColor = currentThemeColor,
                        testTag = "sound_alert_switch"
                    )

                    if (settings.soundAlertsEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // ACTIVE VOLUME SLIDER (REQ 5)
                        Text(
                            text = "VOLUMEN DE ALARMA SENSORIZADA: ${settings.alarmVolume}%",
                            fontSize = 10.sp,
                            color = currentThemeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack, RoundedCornerShape(2.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (settings.alarmVolume == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Buzzer Volume",
                                tint = currentThemeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Slider(
                                value = settings.alarmVolume.toFloat(),
                                onValueChange = { viewModel.updateSettings(settings.copy(alarmVolume = it.toInt())) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = currentThemeColor,
                                    activeTrackColor = currentThemeColor,
                                    inactiveTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.weight(1f).testTag("alarm_volume_slider")
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // BEEP TONE CHIP SELECTOR (REQ 5)
                        Text(
                            text = "TONO DE AUDIO DEL BUZZER SELECCIONADO:",
                            fontSize = 10.sp,
                            color = currentThemeColor,
                            fontWeight = FontWeight.Bold
                        )
                        val soundTonesList = listOf(
                            "STANDARD" to "ESTÁNDAR",
                            "HIGH_PITCH" to "AGUDO",
                            "DUAL_ALERT" to "DOBLE ALERTA",
                            "RADAR_BEEP" to "RADAR"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            soundTonesList.forEach { (toneKey, toneLabel) ->
                                val isSelected = settings.alarmSoundType == toneKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) currentThemeColor.copy(alpha = 0.12f) else CyberBlack, RoundedCornerShape(2.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) currentThemeColor else Color.Gray.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                        .clickable { 
                                            viewModel.updateSettings(settings.copy(alarmSoundType = toneKey))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = toneLabel,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SensorSwitchRow(
                        label = "AUTOGUARDADO EN BASE DE DATOS",
                        description = "Guarda el trayecto automáticamente al reiniciar el odómetro",
                        checked = settings.autoSaveTripsEnabled,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(autoSaveTripsEnabled = it)) },
                        themeColor = currentThemeColor,
                        testTag = "auto_save_trips_switch"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SensorSwitchRow(
                        label = "GUARDAR COORDENADAS RUTA GPS",
                        description = "Guarda las coordenadas del viaje en SQLite para renderizar el mapa",
                        checked = settings.autoSaveCoordinatesEnabled,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(autoSaveCoordinatesEnabled = it)) },
                        themeColor = currentThemeColor,
                        testTag = "auto_save_coords_switch"
                    )
                }
            }

            // CATEGORY 4: HUD PANEL LAYOUT DISPLACEMENT VISIBILITY
            SettingsCategoryCard(title = "04 - PERSONALIZACIÓN DE PANTALLA HUD", themeColor = currentThemeColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SensorSwitchRow(
                        label = "MOSTRAR GRÁFICO DE VELOCIDAD",
                        description = "Permite visualizar u ocultar el gráfico de telemetría en tiempo real",
                        checked = settings.showSpeedChart,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showSpeedChart = it)) },
                        themeColor = currentThemeColor,
                        testTag = "show_chart_switch"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SensorSwitchRow(
                        label = "MOSTRAR PANEL DE ESTADÍSTICAS",
                        description = "Elige si deseas ver el panel de resumen métrico del viaje",
                        checked = settings.showStatsPanel,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showStatsPanel = it)) },
                        themeColor = currentThemeColor,
                        testTag = "show_stats_switch"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SensorSwitchRow(
                        label = "MOSTRAR BRÚJULA Y RADAR",
                        description = "Elige si deseas visualizar el radar giroscópico y la brújula dinámica",
                        checked = settings.showSensorsCompass,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showSensorsCompass = it)) },
                        themeColor = currentThemeColor,
                        testTag = "show_compass_switch"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SensorSwitchRow(
                        label = "MOSTRAR DECIMALES DE VELOCIDAD",
                        description = "Muestra decimales en el velocímetro principal de la cabina",
                        checked = settings.showSpeedDecimals,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showSpeedDecimals = it)) },
                        themeColor = currentThemeColor,
                        testTag = "show_decimals_switch"
                    )
                }
            }

            // MAINTENANCE & DATA ADMINISTRATION CONTROL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonRed.copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ZONA DE PELIGRO - DEPURAR DATOS",
                        color = NeonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        text = "¡Atención! Las siguientes acciones borrarán de forma irreversible todos los trayectos guardados en la base de datos local SQLite.",
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )

                    Button(
                        onClick = { viewModel.clearAllTrips() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(2.dp),
                        border = borderStrokeGlow(true, NeonRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("purge_database_button")
                    ) {
                        Text(
                            text = "ELIMINAR TODOS LOS REGISTROS DE VIAJE",
                            color = NeonRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ACERCA DE (ABOUT APP) SECTION (REQ 3)
            var showAboutInfo by remember { mutableStateOf(false) }

            Button(
                onClick = { showAboutInfo = !showAboutInfo },
                colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(2.dp),
                border = borderStrokeGlow(true, currentThemeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("about_app_button")
            ) {
                Text(
                    text = "ACERCA DE",
                    color = currentThemeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            if (showAboutInfo) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, currentThemeColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "INFORMACIÓN DEL SISTEMA",
                            color = currentThemeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(currentThemeColor.copy(alpha = 0.15f))
                        )
                        Text(
                            text = "EdBenTracker v2.1.0",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = "Desarrollador: Eduardo Benítez",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Este terminal de rastreo y odómetro digital está programado utilizando arquitectura offline-first con Jetpack Compose y Room SQLite. Ofrece visualización en tiempo real de telemetría, alertas sonoras programables con un buzzer virtual, mapeo topográfico con Canvas vectorial y un registro completo de viajes histórico exportable en formato CSV.",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    themeColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = themeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            content()
        }
    }
}

@Composable
fun ThemeOptionSelector(
    label: String,
    baseColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (isSelected) baseColor.copy(alpha = 0.12f) else CyberBlack, RoundedCornerShape(2.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) baseColor else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(2.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(baseColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AlarmConfigRow(
    label: String,
    speedLimit: Double,
    isEnabled: Boolean,
    hexColor: String,
    onSpeedChange: (Double) -> Unit,
    onToggle: (Boolean) -> Unit,
    onColorChange: (String) -> Unit,
    themeColor: Color,
    tagPrefix: String
) {
    val indicatorColor = try { Color(android.graphics.Color.parseColor(hexColor)) } catch (e: Exception) { themeColor }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBlack, RoundedCornerShape(2.dp))
            .border(0.5.dp, themeColor.copy(alpha = 0.1f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Label + Switch trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(indicatorColor, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (isEnabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = indicatorColor,
                    checkedTrackColor = indicatorColor.copy(alpha = 0.35f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = CyberDarkCard
                ),
                modifier = Modifier.testTag("${tagPrefix}_switch")
            )
        }

        if (isEnabled) {
            // Row 2: Speed Threshold slider selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${speedLimit.toInt()} KMH",
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = indicatorColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(52.dp)
                )
                
                Slider(
                    value = speedLimit.toFloat(),
                    onValueChange = { onSpeedChange(it.toDouble()) },
                    valueRange = 20f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = indicatorColor,
                        activeTrackColor = indicatorColor,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f).testTag("${tagPrefix}_slider")
                )
            }

            // Row 3: Alert Color triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "COLOR ALERTA:",
                    fontSize = 8.sp,
                    color = Color.Gray
                )

                // Render options for colors: RED, ORANGE, MAGENTA, CYAN, GREEN, YELLOW, BLUE, WHITE
                listOf(
                    "#FF1744" to "ROJO",
                    "#FF9100" to "NARANJA",
                    "#E040FB" to "VIOLET",
                    "#00FFFF" to "CYAN",
                    "#00E676" to "GREEN",
                    "#FFEA00" to "YELLOW",
                    "#2979FF" to "BLUE",
                    "#FFFFFF" to "WHITE"
                ).forEach { (colorHex, colorName) ->
                    val colorObj = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { themeColor }
                    val isColorSelected = hexColor.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colorObj, RoundedCornerShape(2.dp))
                            .border(
                                width = if (isColorSelected) 1.5.dp else 0.5.dp,
                                color = if (isColorSelected) Color.White else Color.Gray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable { onColorChange(colorHex) }
                    )
                }
            }
        }
    }
}

@Composable
fun SensorSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeColor: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBlack, RoundedCornerShape(2.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                fontSize = 8.sp,
                color = Color.Gray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = themeColor,
                checkedTrackColor = themeColor.copy(alpha = 0.35f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = CyberDarkCard
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun borderStrokeGlow(
    enabled: Boolean,
    color: Color
): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = if (enabled) color else color.copy(alpha = 0.25f)
    )
}
