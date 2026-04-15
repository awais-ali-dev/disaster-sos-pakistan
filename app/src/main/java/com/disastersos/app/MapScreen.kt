// FILE: app/src/main/java/com/disastersos/app/MapScreen.kt

package com.disastersos.app

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

// ─────────────────────────────────────────────────────────────────
// Map Screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    message: SosMessage,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    remember {
        initOsmdroid(context)
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SOS Location — ${message.senderName}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF1C1C1E),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OsmdroidMapView(
                latitude  = message.latitude,
                longitude = message.longitude,
                title     = "SOS from ${message.senderName}",
                modifier  = Modifier.fillMaxSize()
            )
            SosInfoCard(
                message  = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// osmdroid MapView wrapped for Jetpack Compose
// ─────────────────────────────────────────────────────────────────

@Composable
fun OsmdroidMapView(
    latitude  : Double,
    longitude : Double,
    title     : String,
    modifier  : Modifier = Modifier
) {
    val context  = LocalContext.current
    val geoPoint = remember(latitude, longitude) { GeoPoint(latitude, longitude) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled   = false
            controller.setZoom(15.0)
            controller.setCenter(geoPoint)

            val marker = Marker(this).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                this.title = title
                snippet    = "$latitude, $longitude"
            }
            overlays.add(marker)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(
        factory  = { mapView },
        modifier = modifier,
        update   = { map -> map.controller.animateTo(geoPoint) }
    )
}

// ─────────────────────────────────────────────────────────────────
// SOS info card at bottom of map
// ─────────────────────────────────────────────────────────────────

@Composable
fun SosInfoCard(message: SosMessage, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🆘 SOS from ${message.senderName}",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint     = Color(0xFFFF3B30),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    message.coordinatesText,
                    color      = Color(0xFFFF6B61),
                    fontSize   = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                message.timeAgoText,
                color    = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "📡 Offline tiles: Sukkur zoom 10–14 bundled in app",
                color    = Color(0xFF30D158),
                fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// osmdroid initialization
// ─────────────────────────────────────────────────────────────────

fun initOsmdroid(context: Context) {
    copyBundledTiles(context)

    Configuration.getInstance().apply {
        userAgentValue = "DisasterSOSApp/1.0"
        osmdroidBasePath  = context.filesDir
        osmdroidTileCache = File(context.filesDir, "tiles")

        // 500MB cache — never trim bundled tiles
        tileFileSystemCacheMaxBytes  = 500L * 1024 * 1024
        tileFileSystemCacheTrimBytes = 450L * 1024 * 1024

        // Never expire — critical for offline disaster use
        expirationOverrideDuration = Long.MAX_VALUE

        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }
}

// ─────────────────────────────────────────────────────────────────
// Copy bundled tiles from APK assets to internal storage
// Runs only once on first launch — skipped if already copied
// ─────────────────────────────────────────────────────────────────

fun copyBundledTiles(context: Context) {
    val tileDir  = File(context.filesDir, "tiles")
    val tileFile = File(tileDir, "sukkur.sqlite")

    // Skip if already copied (check size to ensure it's complete)
    if (tileFile.exists() && tileFile.length() > 100_000) {
        Log.d("MapTiles", "✅ Bundled tiles already in storage (${tileFile.length() / 1024}KB)")
        return
    }

    tileDir.mkdirs()

    try {
        Log.d("MapTiles", "Copying Sukkur tiles from APK assets…")
        context.assets.open("tiles/sukkur.sqlite").use { input ->
            tileFile.outputStream().use { output ->
                val bytes = input.copyTo(output)
                Log.d("MapTiles", "✅ Copied ${bytes / 1024}KB to internal storage")
            }
        }
    } catch (e: Exception) {
        // Asset not found — app still works with online/cached tiles
        Log.e("MapTiles", "❌ Could not copy tiles: ${e.message}")
    }
}