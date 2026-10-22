package com.example.driving

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.player.AuraPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Background service that continuously enforces the driving safety policy.
 * Restricts video playback while the car is in motion, guarantees continuous
 * audio streaming, and coordinates warning toasts and UI notifications.
 */
class DrivingRestrictionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = AuraPlayerController.activeInstance ?: AuraPlayerController.getOrCreate(applicationContext)
        val drivingManager = controller.drivingModeManager

        // Observe motion changes and enforce video restrictions in real-time
        serviceScope.launch {
            drivingManager.isVehicleMoving.collectLatest { isMoving ->
                if (isMoving && drivingManager.isDrivingRestrictionEnabled.value) {
                    val currentMedia = controller.currentMedia.value
                    if (currentMedia != null && currentMedia.isVideo) {
                        // Vehicle is moving: automatically switch video to audio-only streaming
                        controller.setAudioOnlyForVideo(true)
                        controller.setDrivingWarning(
                            "Vehicle in motion (${drivingManager.currentSpeedKmh.value.toInt()} km/h): Video display prohibited for safety. Audio streaming is active."
                        )
                        drivingManager.notifyProhibitedVideoInteraction(
                            "⚠️ Vehicle in motion: Video display prohibited for safety. Audio streaming will continue."
                        )
                    }
                }
            }
        }

        // Also observe when new media items start playing while vehicle is already moving
        serviceScope.launch {
            controller.currentMedia.collectLatest { media ->
                if (media != null && media.isVideo && drivingManager.isVehicleMoving.value && drivingManager.isDrivingRestrictionEnabled.value) {
                    controller.setAudioOnlyForVideo(true)
                    controller.setDrivingWarning(
                        "Vehicle in motion: Video playback restricted while driving. Playing Audio Only."
                    )
                    drivingManager.notifyProhibitedVideoInteraction(
                        "⚠️ Vehicle in motion: Video playback restricted while driving. Audio streaming will continue."
                    )
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun startService(context: Context) {
            try {
                val intent = Intent(context, DrivingRestrictionService::class.java)
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, DrivingRestrictionService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }
}
