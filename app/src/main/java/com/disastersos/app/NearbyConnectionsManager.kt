// FILE: app/src/main/java/com/disastersos/app/NearbyConnectionsManager.kt
// ─────────────────────────────────────────────────────────────────────────
// Manages the entire Nearby Connections lifecycle:
//   1. Advertising  — "I exist, come connect to me"
//   2. Discovery    — "I'm looking for nearby advertisers"
//   3. Connection   — handshake between two phones
//   4. Messaging    — sending/receiving SOS payload bytes
//
// KEY CONCEPT: Both phones run BOTH advertising AND discovery simultaneously.
// This is called "P2P_STAR" strategy — any phone can be sender or receiver.
// ─────────────────────────────────────────────────────────────────────────

package com.disastersos.app

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "NearbyMesh"

// SERVICE_ID uniquely identifies YOUR app's mesh network.
// Two phones only see each other if they share the same SERVICE_ID.
// Use your package name — guaranteed unique.
private const val SERVICE_ID = "com.disastersos.app.poc"

// LOCAL_NAME is what other phones see when they discover us.
// In the full app we'll use the user's registered name.
private const val LOCAL_NAME = "DisasterSOS"

class NearbyConnectionsManager(private val context: Context) {

    // ── Public state (observed by ViewModel → UI) ─────────────────────────

    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    private val _statusMessage = MutableStateFlow("Idle — tap Start Mesh to begin")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<SosMessage>>(emptyList())
    val receivedMessages: StateFlow<List<SosMessage>> = _receivedMessages.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────

    // Endpoints we've DISCOVERED but not yet connected to
    private val pendingEndpoints = mutableSetOf<String>()

    // The Nearby Connections client — our entry point to the entire API
    private val connectionsClient = Nearby.getConnectionsClient(context)

    // ── Callbacks ─────────────────────────────────────────────────────────

    /**
     * ADVERTISING CALLBACK
     * Called when our advertisement starts/fails.
     * Less interesting than the connection callbacks — mostly for logging.
     */

    /**
     * DISCOVERY CALLBACK
     * Called when we find (or lose) a nearby phone that's advertising.
     *
     * IMPORTANT: Finding an endpoint ≠ being connected.
     * We must explicitly call requestConnection() after finding one.
     */
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            log("Discovered endpoint: $endpointId (${info.endpointName})")
            updateStatus("Found peer: ${info.endpointName} — connecting…")

            if (endpointId in pendingEndpoints || endpointId in _connectedEndpoints.value) {
                log("Already pending/connected to $endpointId — skipping")
                return
            }
            pendingEndpoints.add(endpointId)

            // Request a connection to the discovered endpoint.
            // LOCAL_NAME is what THEY will see as our name.
            connectionsClient
                .requestConnection(LOCAL_NAME, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener {
                    log("Connection request sent to $endpointId")
                }
                .addOnFailureListener { e ->
                    log("Connection request failed to $endpointId: ${e.message}")
                    pendingEndpoints.remove(endpointId)
                    updateStatus("Connection request failed: ${e.message}")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            log("Lost endpoint: $endpointId")
            pendingEndpoints.remove(endpointId)
            updateStatus("Peer lost — still advertising & discovering…")
        }
    }

    /**
     * CONNECTION LIFECYCLE CALLBACK
     * Handles the 3-phase connection handshake:
     *
     * Phase 1 — onConnectionInitiated:  someone wants to connect (or we do)
     * Phase 2 — acceptConnection():     we say "yes, allow it"
     * Phase 3 — onConnectionResult:     connection succeeded or failed
     *
     * We AUTO-ACCEPT all connections in the POC.
     * In the full app, show an authentication token for the user to verify.
     */
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            log("Connection initiated with $endpointId (${info.endpointName})")
            log("Auth token: ${info.authenticationDigits}") // Show this in prod UI!
            updateStatus("Handshaking with ${info.endpointName}…")

            // AUTO-ACCEPT: In POC we trust everyone on the mesh.
            // In production: show info.authenticationDigits to both users
            // and only accept if they match (prevents MITM attacks).
            connectionsClient
                .acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { e ->
                    log("acceptConnection failed: ${e.message}")
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pendingEndpoints.remove(endpointId)
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    log("✅ Connected to $endpointId")
                    _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                    updateStatus("✅ Connected to ${_connectedEndpoints.value.size} peer(s) — mesh active!")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    log("❌ Connection rejected by $endpointId")
                    updateStatus("Connection rejected — other phone may be at capacity")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    log("❌ Connection error with $endpointId: ${result.status.statusMessage}")
                    updateStatus("Connection error: ${result.status.statusMessage}")
                }
                else -> {
                    log("Unknown connection result: ${result.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            log("Disconnected from $endpointId")
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
            val count = _connectedEndpoints.value.size
            updateStatus(
                if (count > 0) "⚠ Peer disconnected — still connected to $count peer(s)"
                else "⚠ All peers disconnected — still advertising & discovering…"
            )
        }
    }

    /**
     * PAYLOAD CALLBACK
     * Called when we receive data from a connected endpoint.
     *
     * Nearby Connections splits large payloads into chunks.
     * BYTES payloads (≤32KB) arrive in one shot — perfect for SOS messages.
     * For the POC, we only use BYTES payloads, so onPayloadTransferUpdate
     * is not critical.
     */
    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            log("Payload received from $endpointId (type=${payload.type})")
            if (payload.type != Payload.Type.BYTES) {
                log("Ignoring non-BYTES payload")
                return
            }
            val bytes = payload.asBytes() ?: return
            val message = SosMessage.fromPayloadBytes(bytes)
            if (message != null) {
                log("✅ SOS received: ${message.coordinatesText}")
                // Prepend new message so latest appears at top
                _receivedMessages.value = listOf(message) + _receivedMessages.value
                updateStatus("🆘 SOS RECEIVED from ${message.senderName}!")
            } else {
                log("⚠ Received unrecognised payload: ${String(bytes)}")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // For BYTES payloads this fires once with status SUCCESS.
            // We don't need to handle it for the POC.
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Start advertising AND discovery simultaneously.
     *
     * STRATEGY: P2P_STAR
     *   - One phone acts as hub, others as spokes.
     *   - BUT — since both phones advertise + discover, they find each other
     *     regardless of who "started first."
     *   - Range: ~100m via WiFi Direct, ~30m via Bluetooth fallback.
     *   - Throughput: plenty for GPS coordinates (tiny messages).
     *
     * Call this once. Nearby Connections handles transport selection
     * (WiFi Direct vs Bluetooth) automatically.
     */
    fun start() {
        log("Starting advertising + discovery…")
        updateStatus("Starting mesh — searching for peers…")

        startAdvertising()
        startDiscovery()
    }

    /**
     * Stop all Nearby Connections activity and disconnect peers.
     * Call this when the user taps "Stop Mesh" or the app goes to background.
     */
    fun stop() {
        log("Stopping all connections")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptySet()
        pendingEndpoints.clear()
        updateStatus("Mesh stopped — tap Start Mesh to restart")
    }

    /**
     * Send an SOS message to ALL currently connected endpoints.
     *
     * @param message The SosMessage to broadcast (contains GPS + sender name)
     * @return Number of endpoints the message was sent to
     */
    fun sendSOS(message: SosMessage): Int {
        val endpoints = _connectedEndpoints.value
        if (endpoints.isEmpty()) {
            updateStatus("⚠ No peers connected — SOS not sent")
            return 0
        }

        val bytes = with(SosMessage) { message.toPayloadBytes() }
        val payload = Payload.fromBytes(bytes)

        var sentCount = 0
        endpoints.forEach { endpointId ->
            connectionsClient
                .sendPayload(endpointId, payload)
                .addOnSuccessListener {
                    sentCount++
                    log("✅ SOS sent to $endpointId")
                    updateStatus("✅ SOS sent to ${endpoints.size} peer(s)!")
                }
                .addOnFailureListener { e ->
                    log("❌ Failed to send SOS to $endpointId: ${e.message}")
                    updateStatus("❌ Send failed: ${e.message}")
                }
        }
        return endpoints.size
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient
            .startAdvertising(LOCAL_NAME, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener {
                log("Advertising started — other phones can now discover us")
            }
            .addOnFailureListener { e ->
                log("Advertising failed: ${e.message}")
                updateStatus("⚠ Advertising failed: ${e.message} — check permissions")
            }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                log("Discovery started — scanning for nearby peers")
                updateStatus("Scanning for nearby peers…")
            }
            .addOnFailureListener { e ->
                log("Discovery failed: ${e.message}")
                updateStatus("⚠ Discovery failed: ${e.message} — check permissions")
            }
    }

    private fun updateStatus(msg: String) {
        log(msg)
        _statusMessage.value = msg
    }

    private fun log(msg: String) = Log.d(TAG, msg)
}