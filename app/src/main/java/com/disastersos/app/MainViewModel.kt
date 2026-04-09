// FILE: app/src/main/java/com/disastersos/app/MainViewModel.kt
// ─────────────────────────────────────────────────────────────────
// Bridges NearbyConnectionsManager ↔ Compose UI.
//
// Why ViewModel?
//   - Survives screen rotation (Activity recreates, ViewModel doesn't)
//   - Exposes StateFlow streams that Compose observes reactively
//   - Keeps MainActivity clean — no business logic there
// ─────────────────────────────────────────────────────────────────

package com.disastersos.app

import android.app.Application
import android.annotation.SuppressLint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ── Nearby Connections manager ────────────────────────────────────────
    private val nearbyManager = NearbyConnectionsManager(application)

    // ── Fused Location client (GPS) ───────────────────────────────────────
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    // ── UI State exposed to Compose ───────────────────────────────────────

    // Pass-through from NearbyConnectionsManager
    val connectedEndpoints: StateFlow<Set<String>> = nearbyManager.connectedEndpoints
    val receivedMessages  : StateFlow<List<SosMessage>> = nearbyManager.receivedMessages

    // meshStatus: combine manager status + our own messages
    val meshStatus: StateFlow<String> = nearbyManager.statusMessage
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = "Idle — tap Start Mesh to begin"
        )

    // Whether we're currently fetching GPS + sending SOS
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Human-readable GPS status shown below the SOS button
    private val _locationStatus = MutableStateFlow("GPS: not yet fetched")
    val locationStatus: StateFlow<String> = _locationStatus.asStateFlow()

    // ── Public actions (called from UI) ───────────────────────────────────

    /** Start advertising + discovery. Safe to call multiple times. */
    fun startMesh() {
        nearbyManager.start()
    }

    /** Stop all mesh activity. */
    fun stopMesh() {
        nearbyManager.stop()
    }

    /**
     * Fetch current GPS location and broadcast an SOS to all peers.
     *
     * Flow:
     *   1. Set isSending = true  (shows spinner on button)
     *   2. Request fresh GPS fix (PRIORITY_HIGH_ACCURACY)
     *   3. Build SosMessage from coordinates
     *   4. Send via NearbyConnectionsManager
     *   5. Set isSending = false
     *
     * The @SuppressLint is safe here because:
     *   - We only call this AFTER permissions are granted (MainActivity checks)
     *   - The permission check lives in the Compose permission gate
     */
    @SuppressLint("MissingPermission")
    fun sendSOS() {
        if (_isSending.value) return          // Prevent double-tap

        viewModelScope.launch {
            _isSending.value = true
            _locationStatus.value = "GPS: fetching…"

            try {
                // Request a fresh high-accuracy GPS fix.
                // CancellationTokenSource lets us cancel if ViewModel is cleared.
                val cts = CancellationTokenSource()
                val location = fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .await()

                if (location == null) {
                    // GPS fix failed (indoors, permissions issue, etc.)
                    _locationStatus.value = "GPS: fix failed — try outdoors"
                    _isSending.value = false
                    return@launch
                }

                // Update the location display under the SOS button
                val lat = String.format("%.4f", location.latitude)
                val lon = String.format("%.4f", location.longitude)
                _locationStatus.value = "GPS: $lat, $lon  ±${location.accuracy.toInt()}m"

                // Build the SOS message
                val sos = SosMessage(
                    senderName  = android.os.Build.MODEL,   // e.g. "Pixel 7"
                    latitude    = location.latitude,
                    longitude   = location.longitude,
                    accuracy    = location.accuracy,
                    timestampMs = System.currentTimeMillis()
                )

                // Broadcast to all connected peers
                val sentTo = nearbyManager.sendSOS(sos)
                if (sentTo == 0) {
                    _locationStatus.value = "GPS: $lat, $lon — but NO peers connected!"
                }

            } catch (e: Exception) {
                _locationStatus.value = "GPS error: ${e.message}"
            } finally {
                // Small delay so the spinner is visible even on fast GPS fix
                kotlinx.coroutines.delay(800)
                _isSending.value = false
            }
        }
    }

    /** Clean up when ViewModel is destroyed */
    override fun onCleared() {
        super.onCleared()
        nearbyManager.stop()
    }
}