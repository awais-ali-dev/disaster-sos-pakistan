package com.disastersos.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// ─────────────────────────────────────────────────────────────────────────────
// Theme
// ─────────────────────────────────────────────────────────────────────────────

private val EmergencyDark = darkColorScheme(
    primary      = Color(0xFFFF3B30),
    onPrimary    = Color.White,
    secondary    = Color(0xFF30D158),
    onSecondary  = Color.Black,
    background   = Color(0xFF1C1C1E),
    surface      = Color(0xFF2C2C2E),
    onBackground = Color.White,
    onSurface    = Color.White,
    error        = Color(0xFFFF453A)
)

// ─────────────────────────────────────────────────────────────────────────────
// Required permissions — version-aware
// ─────────────────────────────────────────────────────────────────────────────

private fun getRequiredPermissions(): List<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        @Suppress("DEPRECATION") add(Manifest.permission.BLUETOOTH)
        @Suppress("DEPRECATION") add(Manifest.permission.BLUETOOTH_ADMIN)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = EmergencyDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    DisasterSOSApp(viewModel = viewModel, onVibrate = ::vibrateDevice)
                }
            }
        }
    }

    private fun vibrateDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)
                .defaultVibrator
                .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1))
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
                .vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable — permission gate
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DisasterSOSApp(viewModel: MainViewModel, onVibrate: () -> Unit) {
    val permState = rememberMultiplePermissionsState(permissions = getRequiredPermissions())
    if (permState.allPermissionsGranted) {
        MainScreen(viewModel = viewModel, onVibrate = onVibrate)
    } else {
        PermissionsScreen(permState)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permissions screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(permState: MultiplePermissionsState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Warning, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            "Permissions Required",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Disaster SOS needs Location and Bluetooth to discover nearby phones " +
                    "and share SOS via the offline mesh network.",
            style       = MaterialTheme.typography.bodyLarge,
            textAlign   = TextAlign.Center,
            color       = Color.Gray
        )
        Spacer(Modifier.height(12.dp))
        val denied = permState.permissions.filter { it.status != PermissionStatus.Granted }
        if (denied.isNotEmpty()) {
            Text(
                "Still needed:\n" + denied.joinToString("\n") { "• " + it.permission.substringAfterLast('.') },
                style     = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color     = Color(0xFFFF9F0A)
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick  = { permState.launchMultiplePermissionRequest() },
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Grant Permissions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "If nothing happens: Settings → Apps → Disaster SOS → Permissions",
            style     = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color     = Color.Gray
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main screen — with map navigation
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onVibrate: () -> Unit) {
    val connectedPeers   by viewModel.connectedEndpoints.collectAsStateWithLifecycle()
    val receivedMessages by viewModel.receivedMessages.collectAsStateWithLifecycle()
    val meshStatus       by viewModel.meshStatus.collectAsStateWithLifecycle()
    val locationStatus   by viewModel.locationStatus.collectAsStateWithLifecycle()
    val isSending        by viewModel.isSending.collectAsStateWithLifecycle()
    var meshStarted      by remember { mutableStateOf(false) }

    // ── Map navigation state ──────────────────────────────────────────────
    // null = show main screen | non-null = show map for that SOS message
    var selectedSos by remember { mutableStateOf<SosMessage?>(null) }

    // ── If an SOS is selected, show map screen instead ───────────────────
    selectedSos?.let { sos ->
        MapScreen(
            message        = sos,
            onNavigateBack = { selectedSos = null }
        )
        return
    }

    // ── Main screen ───────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🆘 Disaster SOS POC",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF1C1C1E),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                MeshStatusCard(
                    peerCount   = connectedPeers.size,
                    statusText  = meshStatus,
                    meshStarted = meshStarted,
                    onStartMesh = { meshStarted = true;  viewModel.startMesh() },
                    onStopMesh  = { meshStarted = false; viewModel.stopMesh()  }
                )
            }
            item {
                SOSButton(
                    isSending      = isSending,
                    peersConnected = connectedPeers.isNotEmpty(),
                    locationStatus = locationStatus,
                    onClick        = { onVibrate(); viewModel.sendSOS() }
                )
            }
            item { ReceivedMessagesHeader(count = receivedMessages.size) }
            if (receivedMessages.isEmpty()) {
                item { EmptyMessagesPlaceholder() }
            } else {
                items(
                    receivedMessages,
                    key = { "${it.senderName}-${it.timestampMs}" }
                ) { msg ->
                    // Tap card → open map at sender's GPS location
                    SosMessageCard(
                        message = msg,
                        onClick = { selectedSos = msg }
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mesh status card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MeshStatusCard(
    peerCount: Int, statusText: String, meshStarted: Boolean,
    onStartMesh: () -> Unit, onStopMesh: () -> Unit
) {
    val peerColor by animateColorAsState(
        targetValue   = if (peerCount > 0) Color(0xFF30D158) else Color(0xFFFF9F0A),
        animationSpec = tween(500),
        label         = "peerColor"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment         = Alignment.CenterVertically,
                horizontalArrangement     = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = if (peerCount > 0) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint               = peerColor,
                        modifier           = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Mesh Network",
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp
                    )
                }
                Box(
                    Modifier
                        .background(peerColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$peerCount peer${if (peerCount != 1) "s" else ""}",
                        color      = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                statusText,
                color      = Color.Gray,
                fontSize   = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = onStartMesh,
                    enabled  = !meshStarted,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Color(0xFF30D158),
                        disabledContainerColor = Color(0xFF3A3A3C)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (meshStarted) "✓ Running" else "▶ Start Mesh",
                        color      = if (meshStarted) Color.Gray else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick  = onStopMesh,
                    enabled  = meshStarted,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⏹ Stop", color = if (meshStarted) Color.White else Color.Gray)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SOS button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SOSButton(
    isSending: Boolean,
    peersConnected: Boolean,
    locationStatus: String,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSending) 0.92f else 1f,
        animationSpec = tween(150),
        label         = "sosScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.size(180.dp).scale(scale)
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        color = if (peersConnected) Color(0xFF3A0000) else Color(0xFF2C2C00),
                        shape = CircleShape
                    )
                    .border(
                        width = 4.dp,
                        color = if (peersConnected) Color(0xFFFF3B30) else Color(0xFFFF9F0A),
                        shape = CircleShape
                    )
            )
            Button(
                onClick  = onClick,
                enabled  = !isSending,
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFFFF3B30),
                    disabledContainerColor = Color(0xFF8B1A1A)
                ),
                modifier = Modifier.size(160.dp).semantics {
                    contentDescription = "Emergency SOS — sends GPS location to all nearby phones"
                }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Sending…", color = Color.White, fontSize = 14.sp)
                    } else {
                        Icon(
                            Icons.Filled.Warning,
                            null,
                            tint     = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "SOS",
                            color      = Color.White,
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!peersConnected) {
            Text(
                "⚠ No peers — tap Start Mesh first",
                color     = Color(0xFFFF9F0A),
                fontSize  = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.LocationOn,
                null,
                tint     = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                locationStatus,
                color      = Color.Gray,
                fontSize   = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Received messages
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReceivedMessagesHeader(count: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            "Received SOS Messages",
            color      = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 16.sp
        )
        if (count > 0) {
            Box(
                Modifier
                    .background(Color(0xFFFF3B30), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("$count", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyMessagesPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📡", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("Listening for SOS…", color = Color.Gray, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "When the other phone taps SOS,\ntheir GPS coordinates will appear here.",
                color     = Color.DarkGray,
                textAlign = TextAlign.Center,
                fontSize  = 13.sp
            )
        }
    }
}

// ── SosMessageCard — tappable, opens map on click ────────────────────────────
@Composable
fun SosMessageCard(message: SosMessage, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },          // ← tap to open map
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF3A0000))
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(48.dp).background(Color(0xFFFF3B30), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🆘", fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "SOS from ${message.senderName}",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        null,
                        tint     = Color(0xFFFF3B30),
                        modifier = Modifier.size(14.dp)
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
                Spacer(Modifier.height(2.dp))
                Text(message.timeAgoText, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                // Hint that card opens the map
                Text(
                    "👆 Tap to view on map",
                    color      = Color(0xFF30D158),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}