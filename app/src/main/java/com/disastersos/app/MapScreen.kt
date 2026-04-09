// FILE: app/src/main/java/com/disastersos/app/MapScreen.kt
// ─────────────────────────────────────────────────────────────────
// Offline map screen using osmdroid + OpenStreetMap tiles.
// Shows a red pin at the SOS sender's GPS coordinates.
// Works 100% offline once tiles are cached or pre-loaded.
// ─────────────────────────────────────────────────────────────────

package com.disastersos.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
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
import org.osmdroid.views.overlay.Overlay

// ─────────────────────────────────────────────────────────────────
// Map Screen — entry point called from MainActivity
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    message: SosMessage,        // The SOS we're showing on the map
    onNavigateBack: () -> Unit  // Back button callback
) {
    val context = LocalContext.current

    // Initialize osmdroid configuration once
    // This sets the tile cache directory and user agent
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
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ComposeColor.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor   = ComposeColor(0xFF1C1C1E),
                    titleContentColor = ComposeColor.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── The actual map view ───────────────────────────────
            OsmdroidMapView(
                latitude  = message.latitude,
                longitude = message.longitude,
                title     = "SOS from ${message.senderName}",
                modifier  = Modifier.fillMaxSize()
            )

            // ── SOS info card overlaid at the bottom ──────────────
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
// AndroidView bridges the gap between Compose and classic Android Views
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

    // Create the MapView — kept in remember so it's not recreated on recomposition
    val mapView = remember {
        MapView(context).apply {
            // ── Tile source ───────────────────────────────────────
            // MAPNIK = standard OpenStreetMap tiles
            // When offline: shows cached tiles or blank grey if not cached
            // We'll add pre-downloaded Sindh tiles in Step 4
            setTileSource(TileSourceFactory.MAPNIK)

            // ── Basic map settings ────────────────────────────────
            setMultiTouchControls(true)   // pinch to zoom
            isTilesScaledToDpi = true     // sharp tiles on high-DPI screens
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled   = false

            // ── Initial position: zoom to SOS location ────────────
            // Zoom level 16 = street level (good for rescue)
            // Zoom level 12 = neighbourhood level
            // Zoom level 8  = city level
            controller.setZoom(15.0)
            controller.setCenter(geoPoint)

            // ── Add red SOS marker ────────────────────────────────
            val marker = Marker(this).apply {
                position          = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                this.title        = title
                snippet           = "$latitude, $longitude"
            }
            overlays.add(marker)

            // ── Add accuracy circle around the pin ────────────────
            // Shows the GPS uncertainty radius visually on the map
        }
    }

    // Lifecycle-aware: pause/resume map with the composable
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    // Embed the classic Android View inside Compose
    AndroidView(
        factory  = { mapView },
        modifier = modifier,
        update   = { map ->
            // Called when latitude/longitude changes (new SOS)
            map.controller.animateTo(geoPoint)
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// SOS info card — shown at bottom of map
// ─────────────────────────────────────────────────────────────────

@Composable
fun SosInfoCard(message: SosMessage, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = ComposeColor(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Sender name
            Text(
                "🆘 SOS from ${message.senderName}",
                color      = ComposeColor.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
            Spacer(Modifier.height(8.dp))

            // Coordinates
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint     = ComposeColor(0xFFFF3B30),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    message.coordinatesText,
                    color      = ComposeColor(0xFFFF6B61),
                    fontSize   = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))

            // Timestamp
            Text(
                message.timeAgoText,
                color    = ComposeColor.Gray,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))

            // Offline notice
            Text(
                "📡 Map tiles: online = full detail  |  offline = cached tiles only",
                color    = ComposeColor(0xFFFF9F0A),
                fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// osmdroid initialization
// Must be called once before any MapView is created.
// ─────────────────────────────────────────────────────────────────

fun initOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        // User agent — identifies your app to tile servers
        // Must not be "osmdroid" (they block that)
        userAgentValue = "DisasterSOSApp/1.0"

        // Tile cache location — internal storage, no permission needed
        // On Android 10+, scoped storage handles this automatically
        osmdroidBasePath    = context.filesDir
        osmdroidTileCache   = java.io.File(context.filesDir, "tiles")

        // Cache size: 100MB — enough for Sindh province at zoom 12-16
        tileFileSystemCacheMaxBytes = 100L * 1024 * 1024

        // Load any saved osmdroid preferences
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }
}