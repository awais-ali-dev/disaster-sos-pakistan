// FILE: app/src/main/java/com/disastersos/app/SosMessage.kt
// ─────────────────────────────────────────────────────────────────
// Data model for a received SOS message.
// Kept intentionally simple for the POC — no encryption, no DB yet.
// ─────────────────────────────────────────────────────────────────

package com.disastersos.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents one SOS received over the mesh network.
 *
 * @param senderName     Human-readable endpoint name from Nearby Connections
 * @param latitude       GPS latitude of the sender
 * @param longitude      GPS longitude of the sender
 * @param accuracy       GPS accuracy in metres (so receiver knows how precise it is)
 * @param timestampMs    Unix timestamp in milliseconds when SOS was SENT
 */
data class SosMessage(
    val senderName  : String,
    val latitude    : Double,
    val longitude   : Double,
    val accuracy    : Float,
    val timestampMs : Long = System.currentTimeMillis()
) {
    /**
     * Ready-to-display coordinate string.
     * Example: "24.9056° N, 68.7778° E  ±12m"
     */
    val coordinatesText: String
        get() {
            val latDir = if (latitude  >= 0) "N" else "S"
            val lonDir = if (longitude >= 0) "E" else "W"
            val lat    = String.format(Locale.US, "%.4f", Math.abs(latitude))
            val lon    = String.format(Locale.US, "%.4f", Math.abs(longitude))
            val acc    = accuracy.toInt()
            return "$lat° $latDir, $lon° $lonDir  ±${acc}m"
        }

    /**
     * Human-readable time — shown under each SOS card.
     * Example: "Received at 14:32:05"
     */
    val timeAgoText: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return "Received at ${sdf.format(Date(timestampMs))}"
        }

    companion object {
        // ── Wire format ──────────────────────────────────────────────
        // We serialize to a simple pipe-delimited string to send over
        // Nearby Connections as a byte array. No JSON library needed
        // in the POC — keeps dependencies minimal.
        //
        // Format: "SOS|<senderName>|<lat>|<lon>|<accuracy>|<timestampMs>"
        // Example: "SOS|Pixel7|24.9056|68.7778|8.5|1712345678901"

        private const val PREFIX = "SOS"
        private const val SEPARATOR = "|"

        /**
         * Serialize an SosMessage into bytes for transmission.
         */
        fun SosMessage.toPayloadBytes(): ByteArray {
            val wire = listOf(
                PREFIX,
                senderName,
                latitude.toString(),
                longitude.toString(),
                accuracy.toString(),
                timestampMs.toString()
            ).joinToString(SEPARATOR)
            return wire.toByteArray(Charsets.UTF_8)
        }

        /**
         * Try to deserialize bytes received from Nearby Connections.
         * Returns null if the bytes are malformed or not an SOS message.
         */
        fun fromPayloadBytes(bytes: ByteArray): SosMessage? {
            return try {
                val parts = String(bytes, Charsets.UTF_8).split(SEPARATOR)
                if (parts.size < 6 || parts[0] != PREFIX) return null
                SosMessage(
                    senderName  = parts[1],
                    latitude    = parts[2].toDouble(),
                    longitude   = parts[3].toDouble(),
                    accuracy    = parts[4].toFloat(),
                    timestampMs = parts[5].toLong()
                )
            } catch (e: Exception) {
                // Malformed message — ignore silently in POC
                null
            }
        }
    }
}