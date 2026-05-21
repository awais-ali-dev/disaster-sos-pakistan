// FILE: app/src/main/java/com/disastersos/app/DisasterSOSApplication.kt
// ─────────────────────────────────────────────────────────────────
// Application class — runs BEFORE any Activity or screen opens.
// This is the correct place to copy bundled tiles because:
//   - Runs once per app process start
//   - Completes before MainActivity.onCreate()
//   - User never sees grey tiles even on first launch
// ─────────────────────────────────────────────────────────────────

package com.disastersos.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DisasterSOSApplication : Application() {

    // Application-scoped coroutine scope
    // SupervisorJob means one failure doesn't cancel other coroutines
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // ── Copy bundled map tiles in background on first launch ──
        // Dispatchers.IO = background thread, never blocks UI
        // This starts immediately when app process starts — before
        // any screen is shown — so tiles are ready by the time
        // the user navigates to the map screen
        applicationScope.launch(Dispatchers.IO) {
            Log.d("AppStartup", "Starting background tile copy…")
            copyBundledTiles(applicationContext)
            Log.d("AppStartup", "Background tile copy complete")
        }
    }
}