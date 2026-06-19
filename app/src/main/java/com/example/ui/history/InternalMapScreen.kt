package com.example.ui.history

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple
import com.example.ui.viewmodel.MainViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InternalMapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tripId by viewModel.selectedTripIdForMap.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    var csvData by remember { mutableStateOf<String?>(null) }

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

    var filePathCallbackState by remember { mutableStateOf<android.webkit.ValueCallback<Array<android.net.Uri>>?>(null) }

    val fileChooserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        filePathCallbackState?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        filePathCallbackState = null
    }

    LaunchedEffect(tripId) {
        tripId?.let { id ->
            csvData = viewModel.getTripCsvString(id)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val webSettings = this.settings
                    webSettings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            filePathCallbackState?.onReceiveValue(null)
                            filePathCallbackState = filePathCallback
                            try {
                                fileChooserLauncher.launch("*/*")
                            } catch (e: Exception) {
                                filePathCallbackState?.onReceiveValue(null)
                                filePathCallbackState = null
                                return false
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            csvData?.let { data ->
                                val escapedCsv = data
                                    .replace("\\", "\\\\")
                                    .replace("`", "\\`")
                                    .replace("$", "\\$")
                                    .replace("\r", "")
                                
                                val jsCode = "if (typeof parseCSV === 'function') { parseCSV(`$escapedCsv`); }"
                                view?.evaluateJavascript(jsCode, null)
                            }
                        }
                    }
                    loadUrl("file:///android_asset/visualizador.html")
                }
            },
            update = { webView ->
                csvData?.let { data ->
                    val escapedCsv = data
                        .replace("\\", "\\\\")
                        .replace("`", "\\`")
                        .replace("$", "\\$")
                        .replace("\r", "")
                    
                    val jsCode = "if (typeof parseCSV === 'function') { parseCSV(`$escapedCsv`); }"
                    webView.evaluateJavascript(jsCode, null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Small stylish minimalist HUD back arrow floating top-left (<-)
        IconButton(
            onClick = {
                viewModel.activeScreen.value = "history"
                viewModel.selectedTripIdForMap.value = null
            },
            modifier = Modifier
                .padding(14.dp)
                .align(Alignment.TopStart)
                .size(38.dp)
                .background(
                    color = CyberBlack.copy(alpha = 0.82f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = currentThemeColor.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = currentThemeColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
