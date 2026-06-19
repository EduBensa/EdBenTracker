package com.example

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.history.HistoryScreen
import com.example.ui.history.InternalMapScreen
import com.example.ui.hud.HudDashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainCockpitContainer()
            }
        }
    }
}

@Composable
fun MainCockpitContainer() {
    val viewModel: MainViewModel = viewModel()
    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val gpsSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

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

    val context = LocalContext.current

    // Manage Keep Screen On flag (REQ 2 & 3)
    val activity = context as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn && window != null) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (window != null) {
                try {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Permission request logic
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocation || coarseLocation) {
            viewModel.startGpsUpdates()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    viewModel.startGpsUpdates()
                } else {
                    permissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                // Keep tracking during STOP/minimization, only clear on destroy
                viewModel.stopGpsUpdates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack),
        topBar = {
            if (activeScreen != "map_view") {
                TopCockpitHeader(
                    activeScreen = activeScreen,
                    themeColor = currentThemeColor,
                    isTracking = isTracking,
                    isSimulating = isSimulating,
                    keepScreenOn = keepScreenOn,
                    onToggleKeepScreenOn = { viewModel.keepScreenOn.value = !viewModel.keepScreenOn.value },
                    onScreenChange = { viewModel.activeScreen.value = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBlack)
        ) {
            when (activeScreen) {
                "dashboard" -> HudDashboardScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
                "history" -> HistoryScreen(viewModel = viewModel)
                "map_view" -> InternalMapScreen(viewModel = viewModel)
                else -> HudDashboardScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TopCockpitHeader(
    activeScreen: String,
    themeColor: Color,
    isTracking: Boolean,
    isSimulating: Boolean,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onScreenChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.5f),
                        themeColor.copy(alpha = 0.1f),
                        themeColor.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title Cyberpunk HUD version print
            Row(
                modifier = Modifier.weight(1.1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(if (isTracking) NeonGreen else themeColor, RoundedCornerShape(1.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TERMINAL EdBenTracker",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Central Navigation Bar Tab Buttons
            Row(
                modifier = Modifier
                    .weight(1.7f)
                    .background(CyberDarkCard, RoundedCornerShape(2.dp))
                    .border(0.5.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    .padding(1.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "dashboard" to "PRINCIPAL",
                    "settings" to "AJUSTES",
                    "history" to "HISTORIAL"
                ).forEach { (screenId, label) ->
                    val isSelected = activeScreen == screenId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) themeColor.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable { onScreenChange(screenId) }
                            .padding(vertical = 4.dp)
                            .testTag("nav_tab_$screenId"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else themeColor.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Quick Status Indicator & Checkbox
            Row(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(start = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keep Screen On tactile miniature checkbox (REQ 2)
                Row(
                    modifier = Modifier
                        .clickable { onToggleKeepScreenOn() }
                        .padding(end = 5.dp)
                        .testTag("keep_active_checkbox"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .border(0.5.dp, themeColor.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                            .background(if (keepScreenOn) themeColor.copy(alpha = 0.25f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (keepScreenOn) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(themeColor)
                            )
                        }
                    }
                    Text(
                        text = "ACTIVA",
                        color = if (keepScreenOn) Color.White else themeColor.copy(alpha = 0.4f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, if (isSimulating) NeonOrange else NeonGreen, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isSimulating) "SIM" else "GPS",
                        color = if (isSimulating) NeonOrange else NeonGreen,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BottomTelemFooter(
    themeColor: Color,
    gpsSpeed: Double,
    isSimulating: Boolean
) {
    val context = LocalContext.current
    var currentTimeStr by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            currentTimeStr = date
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .border(width = 1.dp, color = themeColor.copy(alpha = 0.15f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // UTC lock - Removed per user request, using tactical indicator
            Text(
                text = "EQUIPO DE SEGUIMIENTO ACTIVO",
                fontSize = 11.sp,
                color = themeColor.copy(alpha = 0.6f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            // Diagnostic core telemetries
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FRECUENCIA: 1.0Hz",
                    fontSize = 10.sp,
                    color = themeColor.copy(alpha = 0.4f)
                )
                Text(
                    text = "ESTADO: CONECTADO",
                    fontSize = 10.sp,
                    color = NeonGreen
                )
            }
        }
    }
}
