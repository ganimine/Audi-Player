package com.example.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Android Auto CarAppService defining the car app lifecycle and sessions,
 * using ScreenManager to manage screens on the car dashboard touch screen.
 */
class AudiCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                val action = intent.action
                if (action == "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK") {
                    return AudiCarNowPlayingScreen(carContext)
                }
                return AudiCarDashboardScreen(carContext)
            }

            override fun onNewIntent(intent: Intent) {
                super.onNewIntent(intent)
                val action = intent.action
                if (action == "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK") {
                    val screenManager = carContext.getCarService(androidx.car.app.ScreenManager::class.java)
                    if (screenManager.top !is AudiCarNowPlayingScreen) {
                        screenManager.push(AudiCarNowPlayingScreen(carContext))
                    }
                }
            }
        }
    }
}
