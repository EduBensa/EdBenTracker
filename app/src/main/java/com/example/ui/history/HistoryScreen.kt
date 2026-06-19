package com.example.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TripRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.HudLatLng
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Modal view state for selected trip
    var selectedTripForDetails by remember { mutableStateOf<TripRecord?>(null) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header + CSV share trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REGISTRO DE VIAJES",
                            color = currentThemeColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "HISTORIAL DE TELEMETRÍA Y COORDENADAS",
                            color = currentThemeColor.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    if (trips.isNotEmpty()) {
                        Button(
                            onClick = { triggerCsvExport(context, viewModel) },
                            colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(2.dp),
                            border = borderStrokeGlow(true, currentThemeColor),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("export_csv_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share CSV data",
                                    tint = currentThemeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "EXPORTAR TODO",
                                    color = currentThemeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Divider Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(currentThemeColor.copy(alpha = 0.2f))
                )

                if (trips.isEmpty()) {
                    // Empty state FUI guide terminal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, currentThemeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .background(CyberDarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "!",
                                color = currentThemeColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                  text = "HISTORIAL VACÍO",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inicie el odómetro desde la pantalla principal para registrar estadísticas y geolocalización.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Chronological entries list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("trips_lazy_column"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(trips, key = { it.id }) { trip ->
                            TripLogItem(
                                trip = trip,
                                themeColor = currentThemeColor,
                                onClick = { selectedTripForDetails = trip },
                                onDelete = { viewModel.deleteTrip(trip.id) }
                            )
                        }
                    }
                }
            }

            // Modal detailed trajectory view overlay sheet
            if (selectedTripForDetails != null) {
                val coroutineScope = rememberCoroutineScope()
                TripDetailsDialog(
                    trip = selectedTripForDetails!!,
                    themeColor = currentThemeColor,
                    onDismiss = { selectedTripForDetails = null },
                    onExportSingle = {
                        coroutineScope.launch {
                            val uri = viewModel.exportSingleTripToCsv(context, selectedTripForDetails!!.id)
                            if (uri != null) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooserIntent = Intent.createChooser(sendIntent, "Exportar Registro Telemetría")
                                context.startActivity(chooserIntent)
                            }
                        }
                    },
                    onViewMap = {
                        viewModel.selectedTripIdForMap.value = selectedTripForDetails!!.id
                        viewModel.activeScreen.value = "map_view"
                        selectedTripForDetails = null
                    }
                )
            }
        }
    }
}

@Composable
fun TripLogItem(
    trip: TripRecord,
    themeColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Force unified dd/MM/yyyy date format
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val dateStr = dateFormatter.format(Date(trip.dateTime))

    val hrs = trip.durationSeconds / 3600
    val mns = (trip.durationSeconds % 3600) / 60
    val scs = trip.durationSeconds % 60
    val durationText = String.format(Locale.US, "%02d:%02d:%02d", hrs, mns, scs)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .testTag("trip_item_${trip.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // First row: Date + Delete + Session Type (Viaje vs Simulación)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "REG. #${trip.id} $dateStr",
                        color = themeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(
                                if (trip.isSimulation) NeonOrange.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f),
                                RoundedCornerShape(2.dp)
                            )
                            .border(
                                1.dp,
                                if (trip.isSimulation) NeonOrange.copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (trip.isSimulation) "SIMULACIÓN" else "VIAJE",
                            color = if (trip.isSimulation) NeonOrange else NeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(24.dp).testTag("delete_trip_${trip.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stats breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info block
                Column(modifier = Modifier.weight(1f)) {
                    Text("DISTANCIA", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = String.format(Locale.US, "%.3f km", trip.distanceKm),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("DURACIÓN", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("VEL MÁXIMA", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = String.format(Locale.US, "%.1f km/h", trip.maxSpeed),
                        color = themeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Route points label indicator
            if (trip.coordinatesPath.isNotEmpty() && trip.coordinatesPath.contains(",")) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map trail verified",
                        tint = themeColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RUTA GRABADA (TAP PARA MAPA VECTORIAL)",
                        color = themeColor.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsDialog(
    trip: TripRecord,
    themeColor: Color,
    onDismiss: () -> Unit,
    onExportSingle: () -> Unit,
    onViewMap: () -> Unit
) {
    val hrs = trip.durationSeconds / 3600
    val mns = (trip.durationSeconds % 3600) / 60
    val scs = trip.durationSeconds % 60
    val elapsed = String.format(Locale.US, "%02d:%02d:%02d", hrs, mns, scs)

    // Decode coordinate path: "lat,lng;lat,lng;..."
    val pathCoordinates = remember(trip.coordinatesPath) {
        if (trip.coordinatesPath.isEmpty()) {
            emptyList()
        } else {
            try {
                trip.coordinatesPath.split(";").mapNotNull {
                    val latLng = it.split(",")
                    if (latLng.size == 2) {
                        val lat = latLng[0].toDoubleOrNull()
                        val lng = latLng[1].toDoubleOrNull()
                        if (lat != null && lng != null) {
                            HudLatLng(lat, lng)
                        } else null
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier
            .border(2.dp, themeColor, RoundedCornerShape(4.dp))
            .testTag("trip_details_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(CyberBlack)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ANÁLISIS DIAGNÓSTICO DEL VIAJE",
                            color = themeColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "REPRESENTACIÓN VECTORIAL DE TELEMETRÍA",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("close_details_dialog")
                        ) {
                            Text(
                                text = "CERRAR",
                                color = themeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Grid stats details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, themeColor.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DISTANCIA TOTAL", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.3f km", trip.distanceKm), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("DURACIÓN TOTAL", fontSize = 9.sp, color = Color.Gray)
                            Text(elapsed, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("VELOCIDAD MÁXIMA", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.1f km/h", trip.maxSpeed), color = themeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("VELOCIDAD PROMEDIO", fontSize = 9.sp, color = Color.Gray)
                            Text(String.format(Locale.US, "%.1f km/h", trip.avgSpeed), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Plot map section if route path has elements
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "RADAR VECTORIAL HISTÓRICO",
                        color = themeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(CyberDarkCard)
                            .border(1.dp, themeColor.copy(alpha = 0.3f))
                    ) {
                        if (pathCoordinates.size > 1) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val center = Offset(width / 2, height / 2)

                                // Crosshairs target
                                drawLine(
                                    color = themeColor.copy(alpha = 0.08f),
                                    start = Offset(0f, center.y),
                                    end = Offset(width, center.y)
                                )
                                drawLine(
                                    color = themeColor.copy(alpha = 0.08f),
                                    start = Offset(center.x, 0f),
                                    end = Offset(center.x, height)
                                )

                                drawCircle(
                                    color = themeColor.copy(alpha = 0.05f),
                                    radius = min(width, height) * 0.35f,
                                    center = center,
                                    style = Stroke(width = 1f)
                                )

                                // Autocenter mapping path coords
                                val minLat = pathCoordinates.minOf { it.latitude }
                                val maxLat = pathCoordinates.maxOf { it.latitude }
                                val minLng = pathCoordinates.minOf { it.longitude }
                                val maxLng = pathCoordinates.maxOf { it.longitude }

                                val latDelta = (maxLat - minLat).coerceAtLeast(0.0001)
                                val lngDelta = (maxLng - minLng).coerceAtLeast(0.0001)

                                val polylinePath = Path()
                                pathCoordinates.forEachIndexed { i, coord ->
                                    val x = 16f + ((coord.longitude - minLng) / lngDelta * (width - 32f)).toFloat()
                                    val y = 16f + ((1f - (coord.latitude - minLat) / latDelta) * (height - 32f)).toFloat()

                                    if (i == 0) {
                                        polylinePath.moveTo(x, y)
                                    } else {
                                        polylinePath.lineTo(x, y)
                                    }
                                }

                                drawPath(
                                    path = polylinePath,
                                    color = themeColor,
                                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                                )

                                // Start anchor (green dot) and end anchor (red dot)
                                pathCoordinates.firstOrNull()?.let { start ->
                                    val startX = 16f + ((start.longitude - minLng) / lngDelta * (width - 32f)).toFloat()
                                    val startY = 16f + ((1f - (start.latitude - minLat) / latDelta) * (height - 32f)).toFloat()
                                    drawCircle(NeonGreen, radius = 5f, center = Offset(startX, startY))
                                }

                                pathCoordinates.lastOrNull()?.let { last ->
                                    val lastX = 16f + ((last.longitude - minLng) / lngDelta * (width - 32f)).toFloat()
                                    val lastY = 16f + ((1f - (last.latitude - minLat) / latDelta) * (height - 32f)).toFloat()
                                    drawCircle(Color.White, radius = 5f, center = Offset(lastX, lastY))
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SIN COORDENADAS GPS REGISTRADAS",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // BOTH ACTIONS: VER MAPA & EXPORT TRIP HUD BOTTOM BUTTON SIDE-BY-SIDE (REQ 1 & 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onViewMap() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(2.dp),
                        border = borderStrokeGlow(true, themeColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("view_map_external_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Ver Mapa",
                                tint = themeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "VER MAPA",
                                color = themeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onExportSingle() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(2.dp),
                        border = borderStrokeGlow(true, themeColor),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .testTag("export_single_trip_bottom_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Exportar Viaje",
                                tint = themeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "EXPORTAR CSV",
                                color = themeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    )
}

private fun triggerCsvExport(context: Context, viewModel: MainViewModel) {
    val csvUri = viewModel.exportTripsToCsv(context)
    if (csvUri != null) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, csvUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Exportar Registro Telemetría")
        context.startActivity(chooserIntent)
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
