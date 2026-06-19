package com.example.ui.hud

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SettingsRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.HudLatLng
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale
import kotlin.math.*

@Composable
fun HudDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val maxSpeed by viewModel.maxSpeed.collectAsStateWithLifecycle()
    val avgSpeed by viewModel.avgSpeed.collectAsStateWithLifecycle()
    val distanceKm by viewModel.distanceKm.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    
    val speedHistoryList by viewModel.speedGraphHistory.collectAsStateWithLifecycle()
    val zoomPointsCount by viewModel.graphZoomPoints.collectAsStateWithLifecycle()
    
    val gX by viewModel.gForceX.collectAsStateWithLifecycle()
    val gY by viewModel.gForceY.collectAsStateWithLifecycle()
    val gZ by viewModel.gForceZ.collectAsStateWithLifecycle()
    val azimuthDegrees by viewModel.compassAzimuth.collectAsStateWithLifecycle()
    
    val showMiniMapRadar by viewModel.showMiniMapRadar.collectAsStateWithLifecycle()
    val activeAlarmColor by viewModel.activeAlarmColor.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    
    val routePath by viewModel.currentRoutePath.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val simulatedSpeedTarget by viewModel.simulatedTargetSpeed.collectAsStateWithLifecycle()

    // Alarm Warning flashing effect
    val infiniteTransition = rememberInfiniteTransition(label = "Alarm Warning Flash")
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alarm Warning Alpha"
    )

    // Dynamic color helper based on active user-defined theme
    val themeColor = if (settings.hudColorTheme.startsWith("#")) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Draw background grid lines (Cyberpunk Matrix lines)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 48.dp.toPx()
                val width = size.width
                val height = size.height
                
                // Vertical lines
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = CyberGrid.copy(alpha = 0.12f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }
                
                // Horizontal lines
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = CyberGrid.copy(alpha = 0.12f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }
            }

            // Warning screen-border flash if alarm triggers
            if (activeAlarmColor != null) {
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(activeAlarmColor))
                } catch (e: Exception) {
                    NeonRed
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(parsedColor.copy(alpha = warningAlpha))
                ) {
                    Text(
                        text = "WARNING: SPEED THRESHOLD EXCEEDED",
                        color = parsedColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(CyberBlack.copy(alpha = 0.8f))
                            .padding(vertical = 4.dp)
                            .border(1.dp, parsedColor, RoundedCornerShape(2.dp))
                    )
                }
            }

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // LANDSCAPE MODE: Left column fixed big gauge, right column 2x2 matrix of widgets
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Big Odometer Gauge + Controls (Fixed)
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        OdometerGauge(
                            speed = currentSpeed,
                            themeColor = themeColor,
                            maxLimit = when {
                                settings.alarmCEnabled -> settings.alarmCSpeed
                                settings.alarmBEnabled -> settings.alarmBSpeed
                                settings.alarmAEnabled -> settings.alarmASpeed
                                else -> 140.0
                            },
                            showSpeedDecimals = settings.showSpeedDecimals,
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxWidth()
                        )

                        CampaignControls(
                            isTracking = isTracking,
                            isSimulating = isSimulating,
                            simulatedSpeed = simulatedSpeedTarget,
                            themeColor = themeColor,
                            onStart = { viewModel.startCampaign() },
                            onPause = { viewModel.pauseCampaign() },
                            onReset = { viewModel.resetCampaign() },
                            onToggleSimulation = { viewModel.setSimulation(it) },
                            onSimulationSpeedChanged = { viewModel.setSimulatedSpeedTarget(it) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }

                    // Right Column: grid of widgets
                    Column(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Speed Chart + Stats Panel
                        Row(
                            modifier = Modifier.weight(1.1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (settings.showSpeedChart) {
                                    SpeedGraphWidget(
                                        speedHistory = speedHistoryList,
                                        zoomPoints = zoomPointsCount,
                                        themeColor = themeColor,
                                        onZoomIn = { viewModel.increaseZoom() },
                                        onZoomOut = { viewModel.decreaseZoom() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            }
                            
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (settings.showStatsPanel) {
                                    StatsWidget(
                                        maxSpeed = maxSpeed,
                                        avgSpeed = avgSpeed,
                                        distanceKm = distanceKm,
                                        durationSeconds = durationSeconds,
                                        themeColor = themeColor,
                                        onResetStats = { viewModel.resetTripStatsOnly() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }

                        // Row 2: Compact Alarms + Map/Compass Radar
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                AlarmsWidget(
                                    settings = settings,
                                    currentSpeed = currentSpeed,
                                    themeColor = themeColor,
                                    onToggleAlarmA = { viewModel.updateSettings(settings.copy(alarmAEnabled = it)) },
                                    onToggleAlarmB = { viewModel.updateSettings(settings.copy(alarmBEnabled = it)) },
                                    onToggleAlarmC = { viewModel.updateSettings(settings.copy(alarmCEnabled = it)) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (settings.showSensorsCompass || settings.showGForceTelemetry) {
                                    MapCompassWidget(
                                        showRadar = showMiniMapRadar,
                                        routePath = routePath,
                                        azimuth = azimuthDegrees,
                                        gX = gX,
                                        gY = gY,
                                        gZ = gZ,
                                        themeColor = themeColor,
                                        onToggleRadar = { viewModel.showMiniMapRadar.value = it },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            } else {
                // PORTRAIT MODE: Stacked elements
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OdometerGauge(
                        speed = currentSpeed,
                        themeColor = themeColor,
                        maxLimit = when {
                            settings.alarmCEnabled -> settings.alarmCSpeed
                            settings.alarmBEnabled -> settings.alarmBSpeed
                            settings.alarmAEnabled -> settings.alarmASpeed
                            else -> 140.0
                        },
                        showSpeedDecimals = settings.showSpeedDecimals,
                        modifier = Modifier
                            .height(280.dp) // Considerably enlarged speedometer dial
                            .fillMaxWidth()
                    )

                    CampaignControls(
                        isTracking = isTracking,
                        isSimulating = isSimulating,
                        simulatedSpeed = simulatedSpeedTarget,
                        themeColor = themeColor,
                        onStart = { viewModel.startCampaign() },
                        onPause = { viewModel.pauseCampaign() },
                        onReset = { viewModel.resetCampaign() },
                        onToggleSimulation = { viewModel.setSimulation(it) },
                        onSimulationSpeedChanged = { viewModel.setSimulatedSpeedTarget(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (settings.showStatsPanel) {
                        StatsWidget(
                            maxSpeed = maxSpeed,
                            avgSpeed = avgSpeed,
                            distanceKm = distanceKm,
                            durationSeconds = durationSeconds,
                            themeColor = themeColor,
                            onResetStats = { viewModel.resetTripStatsOnly() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp) // Optimized 2-row layout with zero visual waste
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.5f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AlarmsWidget(
                                settings = settings,
                                currentSpeed = currentSpeed,
                                themeColor = themeColor,
                                onToggleAlarmA = { viewModel.updateSettings(settings.copy(alarmAEnabled = it)) },
                                onToggleAlarmB = { viewModel.updateSettings(settings.copy(alarmBEnabled = it)) },
                                onToggleAlarmC = { viewModel.updateSettings(settings.copy(alarmCEnabled = it)) },
                                modifier = Modifier.height(48.dp) // Highly compact alarm triggers row
                            )

                            if (settings.showSpeedChart) {
                                SpeedGraphWidget(
                                    speedHistory = speedHistoryList,
                                    zoomPoints = zoomPointsCount,
                                    themeColor = themeColor,
                                    onZoomIn = { viewModel.increaseZoom() },
                                    onZoomOut = { viewModel.decreaseZoom() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (settings.showSensorsCompass || settings.showGForceTelemetry) {
                            MapCompassWidget(
                                showRadar = showMiniMapRadar,
                                routePath = routePath,
                                azimuth = azimuthDegrees,
                                gX = gX,
                                gY = gY,
                                gZ = gZ,
                                themeColor = themeColor,
                                onToggleRadar = { viewModel.showMiniMapRadar.value = it },
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 1. MAIN DIAL AND MEASURING DIGITAL SPEED RING
@Composable
fun OdometerGauge(
    speed: Double,
    themeColor: Color,
    maxLimit: Double,
    showSpeedDecimals: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(290.dp)
                .fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) * 0.44f
            
            // Draw dual background circles (cyber rings with sweeping gradients)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(themeColor.copy(alpha = 0.02f), themeColor.copy(alpha = 0.18f), themeColor.copy(alpha = 0.02f)),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = 8f)
            )
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(themeColor.copy(alpha = 0.2f), themeColor.copy(alpha = 0.02f), themeColor.copy(alpha = 0.2f)),
                    center = center
                ),
                radius = radius - 15f,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Segment dash marks for a tactical speedometer gauge appearance
            val totalTicks = 60
            for (i in 0 until totalTicks) {
                // Only render ticks across 270 degree sweep
                val angleRad = Math.toRadians((135.0 + (i * 270.0 / totalTicks))).toFloat()
                val tickLen = if (i % 5 == 0) 24f else 12f
                val strokeW = if (i % 5 == 0) 3.5f else 1.8f
                
                val p1 = Offset(
                    center.x + (radius - tickLen) * cos(angleRad),
                    center.y + (radius - tickLen) * sin(angleRad)
                )
                val p2 = Offset(
                    center.x + radius * cos(angleRad),
                    center.y + radius * sin(angleRad)
                )
                
                drawStrokeWithGlow(
                    start = p1,
                    end = p2,
                    strokeColor = themeColor.copy(alpha = if (i % 5 == 0) 0.65f else 0.35f),
                    thickness = strokeW
                )
            }

            // Filled sweep representing current speed relative to max warning limit
            val speedPercentage = (speed / maxLimit).coerceIn(0.0, 1.0).toFloat()
            val fillSweepAngle = speedPercentage * 270f
            
            val speedArcBrush = Brush.linearGradient(
                colors = listOf(
                    themeColor.copy(alpha = 0.15f),
                    themeColor.copy(alpha = 0.55f),
                    themeColor
                ),
                start = Offset(center.x - radius, center.y + radius),
                end = Offset(center.x + radius, center.y - radius / 2f)
            )

            // Thicker lines with beautiful gradients (degradé)
            drawArc(
                brush = speedArcBrush,
                startAngle = 135f,
                sweepAngle = fillSweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )

            drawArc(
                brush = speedArcBrush,
                startAngle = 135f,
                sweepAngle = fillSweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 7.5f, cap = StrokeCap.Round)
            )

            // Draw glowing core micro points
            drawCircle(
                color = themeColor.copy(alpha = 0.45f),
                radius = 6f,
                center = center
            )
        }

        // Inner digital readout elements
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "VELOCIDAD",
                fontSize = 11.sp,
                color = themeColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val speedText = if (showSpeedDecimals) {
                String.format(Locale.US, "%05.1f", speed)
            } else {
                String.format(Locale.US, "%03d", speed.toInt())
            }

            Text(
                text = speedText,
                fontSize = 68.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.testTag("current_speed_readout")
            )
            
            Text(
                text = "KM/H",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))
            
            // Cyberpunk lock status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (speed > 0.1) NeonGreen else themeColor.copy(alpha = 0.4f), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (speed > 0.1) "SEÑAL ADQUIRIDA" else "SEÑAL INACTIVA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (speed > 0.1) NeonGreen else themeColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// 2. STATISTICS QUICK PANEL PANEL
@Composable
fun StatsWidget(
    maxSpeed: Double,
    avgSpeed: Double,
    distanceKm: Double,
    durationSeconds: Long,
    themeColor: Color,
    onResetStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hrs = durationSeconds / 3600
    val mins = (durationSeconds % 3600) / 60
    val secs = durationSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)

    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.55f),
                        themeColor.copy(alpha = 0.08f),
                        themeColor.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            // Header with custom HUD reset button
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MÉTRICAS",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Box(
                    modifier = Modifier
                        .border(1.dp, NeonRed.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .clickable { onResetStats() }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .testTag("reset_stats_button")
                ) {
                    Text(
                        text = "RST",
                        color = NeonRed,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 1: Speed stats
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MAX: ", color = themeColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.1f km/h", maxSpeed),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PROM: ", color = themeColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.1f km/h", avgSpeed),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Row 2: Distance & Time
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DST: ", color = themeColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.3f km", distanceKm),
                        color = themeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TPO: ", color = themeColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formattedTime,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 3. SEPARATE INDEPENDENT ALARM TRIGGER PANEL
@Composable
fun AlarmsWidget(
    settings: SettingsRecord,
    currentSpeed: Double,
    themeColor: Color,
    onToggleAlarmA: (Boolean) -> Unit,
    onToggleAlarmB: (Boolean) -> Unit,
    onToggleAlarmC: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.55f),
                        themeColor.copy(alpha = 0.08f),
                        themeColor.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // We show 3 speed button options
                val alarms = listOf(
                    Triple(settings.alarmASpeed, settings.alarmAEnabled, try { Color(android.graphics.Color.parseColor(settings.alarmAColor)) } catch (e: Exception) { NeonRed }),
                    Triple(settings.alarmBSpeed, settings.alarmBEnabled, try { Color(android.graphics.Color.parseColor(settings.alarmBColor)) } catch (e: Exception) { NeonOrange }),
                    Triple(settings.alarmCSpeed, settings.alarmCEnabled, try { Color(android.graphics.Color.parseColor(settings.alarmCColor)) } catch (e: Exception) { NeonPurple })
                )
                
                alarms.forEachIndexed { index, alarm ->
                    val speedVal = alarm.first
                    val isEnabled = alarm.second
                    val limitColor = alarm.third
                    val isTriggered = isEnabled && currentSpeed >= speedVal
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "Alarm btn $index")
                    val btnFlashColor by infiniteTransition.animateColor(
                        initialValue = limitColor.copy(alpha = 0.15f),
                        targetValue = limitColor.copy(alpha = 0.7f),
                        animationSpec = infiniteRepeatable(
                            animation = tween(300, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Alarm btn flash $index"
                    )
                    
                    val bg = when {
                        isTriggered -> btnFlashColor
                        isEnabled -> limitColor.copy(alpha = 0.25f)
                        else -> CyberBlack
                    }
                    
                    val borderCol = if (isEnabled) limitColor else Color.Gray.copy(alpha = 0.3f)
                    val textCol = if (isEnabled) Color.White else Color.Gray
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, borderCol, RoundedCornerShape(2.dp))
                            .background(bg, RoundedCornerShape(2.dp))
                            .clickable {
                                when (index) {
                                    0 -> onToggleAlarmA(!settings.alarmAEnabled)
                                    1 -> onToggleAlarmB(!settings.alarmBEnabled)
                                    2 -> onToggleAlarmC(!settings.alarmCEnabled)
                                }
                            }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${speedVal.toInt()}",
                            color = textCol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// 4. REAL-TIME SPEED HISTORICAL GRAPH WIDGET
@Composable
fun SpeedGraphWidget(
    speedHistory: List<Float>,
    zoomPoints: Int,
    themeColor: Color,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.55f),
                        themeColor.copy(alpha = 0.1f),
                        themeColor.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header + Zoom limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRÍA",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, themeColor.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                            .clickable { onZoomIn() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, themeColor.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                            .clickable { onZoomOut() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Canvas drawing line graph
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CyberBlack)
                    .border(0.5.dp, themeColor.copy(alpha = 0.15f))
            ) {
                val width = size.width
                val height = size.height

                // Draw micro horizontal grid lines inside chart
                val linesCount = 4
                for (j in 1..linesCount) {
                    val yLine = (height / (linesCount + 1)) * j
                    drawLine(
                        color = themeColor.copy(alpha = 0.08f),
                        start = Offset(0f, yLine),
                        end = Offset(width, yLine),
                        strokeWidth = 1f
                    )
                }

                if (speedHistory.isNotEmpty()) {
                    // Extract latest segment according to zoom level
                    val visibleSegment = speedHistory.takeLast(zoomPoints)
                    val maxSpeedInList = (visibleSegment.maxOrNull() ?: 100f).coerceAtLeast(30f)
                    
                    val path = Path()
                    val glowPath = Path()
                    
                    val stepX = width / (zoomPoints - 1).coerceAtLeast(1)
                    
                    visibleSegment.forEachIndexed { idx, speed ->
                        // Invert Y coordinate since Canvas starts drawing top-down
                        val xPos = idx * stepX
                        val yNormalized = 1f - (speed / maxSpeedInList)
                        val yPos = yNormalized * (height - 12f) + 6f
                        
                        if (idx == 0) {
                            path.moveTo(xPos, yPos)
                            glowPath.moveTo(xPos, yPos)
                        } else {
                            path.lineTo(xPos, yPos)
                            glowPath.lineTo(xPos, yPos)
                        }
                    }

                    val graphGradientBrush = Brush.verticalGradient(
                        colors = listOf(themeColor, themeColor.copy(alpha = 0.4f))
                    )
                    // Render beautiful double-layered neon glow line with gradients
                    drawPath(
                        path = glowPath,
                        brush = graphGradientBrush,
                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path,
                        brush = graphGradientBrush,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )

                    // Draw end cursor indicator point
                    val lastX = (visibleSegment.size - 1) * stepX
                    val lastYNormalized = 1f - (visibleSegment.last() / maxSpeedInList)
                    val lastY = lastYNormalized * (height - 12f) + 6f
                    
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(lastX, lastY)
                    )
                    drawCircle(
                        color = themeColor,
                        radius = 8f,
                        center = Offset(lastX, lastY),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }
    }
}

// 5. MAP GRID PLOTTER / ACCELEROMETER COMPASS ROUTER
@Composable
fun MapCompassWidget(
    showRadar: Boolean,
    routePath: List<HudLatLng>,
    azimuth: Float,
    gX: Float,
    gY: Float,
    gZ: Float,
    themeColor: Color,
    onToggleRadar: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.55f),
                        themeColor.copy(alpha = 0.08f),
                        themeColor.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // High tech switch tabs header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack, RoundedCornerShape(2.dp))
                    .border(0.5.dp, themeColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (showRadar) themeColor.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(2.dp))
                        .clickable { onToggleRadar(true) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RADAR VECTORIAL",
                        color = if (showRadar) Color.White else themeColor.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (!showRadar) themeColor.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(2.dp))
                        .clickable { onToggleRadar(false) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BRÚJULA GIROSCÓPICA",
                        color = if (!showRadar) Color.White else themeColor.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CyberBlack)
                    .border(0.5.dp, themeColor.copy(alpha = 0.15f))
            ) {
                if (showRadar) {
                    // CYAN VEG VECTOR PATH PLOTTER RADAR CANVAS
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val minDim = min(width, height)
                        val center = Offset(width / 2, height / 2)

                        // 1. Draw diagnostic concentric circles with gradient brushing
                        val radarBrushA = Brush.radialGradient(
                            colors = listOf(themeColor.copy(alpha = 0.01f), themeColor.copy(alpha = 0.15f)),
                            center = center,
                            radius = minDim * 0.44f
                        )
                        val radarBrushB = Brush.radialGradient(
                            colors = listOf(themeColor.copy(alpha = 0.01f), themeColor.copy(alpha = 0.22f)),
                            center = center,
                            radius = minDim * 0.32f
                        )
                        val radarBrushC = Brush.radialGradient(
                            colors = listOf(themeColor.copy(alpha = 0.01f), themeColor.copy(alpha = 0.32f)),
                            center = center,
                            radius = minDim * 0.18f
                        )

                        drawCircle(
                            brush = radarBrushA,
                            radius = minDim * 0.44f,
                            center = center,
                            style = Stroke(width = 1.8f)
                        )
                        drawCircle(
                            brush = radarBrushB,
                            radius = minDim * 0.32f,
                            center = center,
                            style = Stroke(width = 2.2f)
                        )
                        drawCircle(
                            brush = radarBrushC,
                            radius = minDim * 0.18f,
                            center = center,
                            style = Stroke(width = 1.8f)
                        )

                        // Crosshair axis lines
                        drawLine(
                            color = themeColor.copy(alpha = 0.08f),
                            start = Offset(0f, center.y),
                            end = Offset(width, center.y),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = themeColor.copy(alpha = 0.08f),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, height),
                            strokeWidth = 1f
                        )

                        // 2. Draw GPS routing coordinates trails
                        if (routePath.size > 1) {
                            // Find bounds to automatically center and scale the trail to fit the canvas neatly
                            val minLat = routePath.minOf { it.latitude }
                            val maxLat = routePath.maxOf { it.latitude }
                            val minLng = routePath.minOf { it.longitude }
                            val maxLng = routePath.maxOf { it.longitude }

                            val latDelta = (maxLat - minLat).coerceAtLeast(0.0001)
                            val lngDelta = (maxLng - minLng).coerceAtLeast(0.0001)

                            val pathBrush = Brush.linearGradient(
                                colors = listOf(themeColor.copy(alpha = 0.3f), themeColor)
                            )

                            val polylinePath = Path()
                            routePath.forEachIndexed { index, point ->
                                // Map coordinates to canvas space (retaining padding margins)
                                val xPos = 12f + ((point.longitude - minLng) / lngDelta * (width - 24f)).toFloat()
                                val yPos = 12f + ((1f - (point.latitude - minLat) / latDelta) * (height - 24f)).toFloat()

                                if (index == 0) {
                                    polylinePath.moveTo(xPos, yPos)
                                } else {
                                    polylinePath.lineTo(xPos, yPos)
                                }
                            }

                            drawPath(
                                path = polylinePath,
                                brush = pathBrush,
                                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                            )

                            // Current marker point
                            routePath.lastOrNull()?.let { last ->
                                val finalX = 12f + ((last.longitude - minLng) / lngDelta * (width - 24f)).toFloat()
                                val finalY = 12f + ((1f - (last.latitude - minLat) / latDelta) * (height - 24f)).toFloat()

                                drawCircle(
                                    color = Color.White,
                                    radius = 4f,
                                    center = Offset(finalX, finalY)
                                )
                            }
                        } else {
                            // Empty state indicator
                            drawCircle(
                                color = themeColor.copy(alpha = 0.25f),
                                radius = 3f,
                                center = center
                            )
                        }
                    }
                } else {
                    // DIGITAL MAG COMPASS + G-FORCE CROSSHAIR SIDE-BY-SIDE
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left half: G-force telemetry grid
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize(0.9f)) {
                                val sizeMin = min(size.width, size.height)
                                val centerPt = Offset(size.width / 2, size.height / 2)
                                
                                // G-Force bullseye target rings (gradient styling)
                                val gBrushA = Brush.radialGradient(
                                    colors = listOf(themeColor.copy(alpha = 0.02f), themeColor.copy(alpha = 0.18f)),
                                    center = centerPt,
                                    radius = sizeMin * 0.45f
                                )
                                val gBrushB = Brush.radialGradient(
                                    colors = listOf(themeColor.copy(alpha = 0.02f), themeColor.copy(alpha = 0.25f)),
                                    center = centerPt,
                                    radius = sizeMin * 0.25f
                                )

                                drawCircle(
                                    brush = gBrushA,
                                    radius = sizeMin * 0.45f,
                                    center = centerPt,
                                    style = Stroke(width = 1.8f)
                                )
                                drawCircle(
                                    brush = gBrushB,
                                    radius = sizeMin * 0.25f,
                                    center = centerPt,
                                    style = Stroke(width = 2f)
                                )

                                drawLine(
                                    color = themeColor.copy(alpha = 0.1f),
                                    start = Offset(0f, centerPt.y),
                                    end = Offset(size.width, centerPt.y)
                                )
                                drawLine(
                                    color = themeColor.copy(alpha = 0.1f),
                                    start = Offset(centerPt.x, 0f),
                                    end = Offset(centerPt.x, size.height)
                                )

                                // Current G-force coordinates displacement (clamping values)
                                val scale = (sizeMin * 0.4f)
                                val finalX = (centerPt.x + (gX * scale)).coerceIn(0f, size.width)
                                val finalY = (centerPt.y + (gY * scale)).coerceIn(0f, size.height)

                                drawCircle(
                                    color = NeonOrange,
                                    radius = 5f,
                                    center = Offset(finalX, finalY)
                                )
                                drawLine(
                                    color = NeonOrange.copy(alpha = 0.35f),
                                    start = centerPt,
                                    end = Offset(finalX, finalY),
                                    strokeWidth = 2f
                                )
                            }
                            
                            // G-force numeric labels Overlay (translated and expanded)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "G Lat: %.2f G", gX),
                                    fontSize = 12.sp,
                                    color = NeonOrange,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "G Lon: %.2f G", gY),
                                    fontSize = 12.sp,
                                    color = NeonOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right half: Compass sweep azimuth
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize(0.9f)) {
                                val radiusCompass = min(size.width, size.height) * 0.44f
                                val centerCompass = Offset(size.width / 2, size.height / 2)

                                // Rotate entire drawing based on azimuth degree (negative because dial moves opposite to yaw rotation)
                                rotate(
                                    degrees = -azimuth,
                                    pivot = centerCompass
                                ) {
                                    drawCircle(
                                        color = themeColor.copy(alpha = 0.15f),
                                        radius = radiusCompass,
                                        center = centerCompass,
                                        style = Stroke(width = 1.5f)
                                    )

                                    // Compass cardinal ticks N, E, S, W
                                    val cardinals = listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)
                                    cardinals.forEach { (label, angle) ->
                                        val angleRad = Math.toRadians(angle - 90.0).toFloat()
                                        val tickStart = Offset(
                                            centerCompass.x + (radiusCompass - 14f) * cos(angleRad),
                                            centerCompass.y + (radiusCompass - 14f) * sin(angleRad)
                                        )
                                        val tickEnd = Offset(
                                            centerCompass.x + radiusCompass * cos(angleRad),
                                            centerCompass.y + radiusCompass * sin(angleRad)
                                        )
                                        
                                        drawLine(
                                            color = if (label == "N") NeonRed else themeColor,
                                            start = tickStart,
                                            end = tickEnd,
                                            strokeWidth = if (label == "N") 3f else 1.5f
                                        )
                                    }
                                }

                                // static center arrow indicator always pointing raw North/upwards
                                val arrowPath = Path().apply {
                                    moveTo(centerCompass.x, centerCompass.y - radiusCompass + 6f)
                                    lineTo(centerCompass.x - 8f, centerCompass.y - radiusCompass + 22f)
                                    lineTo(centerCompass.x + 8f, centerCompass.y - radiusCompass + 22f)
                                    close()
                                }
                                drawPath(
                                    path = arrowPath,
                                    color = NeonRed
                                )
                            }

                            // Center Azimuth print readable Text
                            Text(
                                text = "${azimuth.toInt()}°",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 6. ADAPTERS FOR ACTION CONTROL TERMINAL BUTTON ROW
@Composable
fun CampaignControls(
    isTracking: Boolean,
    isSimulating: Boolean,
    simulatedSpeed: Double,
    themeColor: Color,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onSimulationSpeedChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CyberDarkCard)
            .border(0.5.dp, themeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Track start / pause actions button
            Button(
                onClick = { if (isTracking) onPause() else onStart() },
                modifier = Modifier
                    .weight(1.5f)
                    .height(38.dp)
                    .testTag("start_pause_campaign_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) NeonOrange.copy(alpha = 0.15f) else themeColor.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(2.dp),
                border = borderStrokeGlow(true, if (isTracking) NeonOrange else themeColor)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Odometer toggle",
                        tint = if (isTracking) NeonOrange else themeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isTracking) "PAUSAR" else "ACTIVAR",
                        color = if (isTracking) NeonOrange else themeColor,
                        fontSize = 11.sp, // slightly larger
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Reset action button
            Button(
                onClick = { onReset() },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("reset_campaign_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(2.dp),
                border = borderStrokeGlow(true, NeonRed),
                enabled = true
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Odometer reset",
                        tint = NeonRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "REINICIAR",
                        color = NeonRed,
                        fontSize = 11.sp, // slightly larger
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Speed Emulator module (Absolutely key for static mock screen browsers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, themeColor.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                .background(CyberBlack)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isSimulating,
                    onCheckedChange = { onToggleSimulation(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = themeColor,
                        checkmarkColor = Color.Black,
                        uncheckedColor = themeColor.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.size(24.dp).testTag("simulator_checkbox")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "MODO SIMULADOR",
                    fontSize = 9.sp, // slightly larger
                    color = if (isSimulating) themeColor else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isSimulating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(
                        text = "${simulatedSpeed.toInt()}\nKMH",
                        fontSize = 7.sp,
                        color = Color.White,
                        lineHeight = 8.sp,
                        textAlign = TextAlign.Center
                    )
                    Slider(
                        value = simulatedSpeed.toFloat(),
                        onValueChange = { onSimulationSpeedChanged(it.toDouble()) },
                        valueRange = 0f..160f,
                        colors = SliderDefaults.colors(
                            thumbColor = themeColor,
                            activeTrackColor = themeColor,
                            inactiveTrackColor = themeColor.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f).height(18.dp).testTag("sim_speed_slider")
                    )
                }
            } else {
                Text(
                    text = "ACTIVAR PARA PRUEBA EN EMULADOR",
                    fontSize = 8.sp, // slightly larger
                    color = themeColor.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

// Inlined helper mapping parameters
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeWithGlow(
    start: Offset,
    end: Offset,
    strokeColor: Color,
    thickness: Float
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(strokeColor.copy(alpha = 0.25f), strokeColor, strokeColor.copy(alpha = 0.25f)),
        start = start,
        end = end
    )
    drawLine(
        brush = gradientBrush,
        start = start,
        end = end,
        strokeWidth = thickness * 3.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        brush = gradientBrush,
        start = start,
        end = end,
        strokeWidth = thickness,
        cap = StrokeCap.Round
    )
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
