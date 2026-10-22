package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import com.example.model.MediaItemData
import com.example.player.AuraPlayerController

/**
 * In-car screen displayed when a user touches any video format on the vehicle touch screen.
 * Displays the driving mode error message:
 * "While driving not possible to open because in driving mode."
 * Offers safe Audio-Only playback or returning to the audio dashboard.
 */
class AudiCarVideoDrivingWarningScreen(
    carContext: CarContext,
    private val videoMedia: MediaItemData,
    private val queue: List<MediaItemData>
) : Screen(carContext) {

    private val playerController: AuraPlayerController
        get() = AuraPlayerController.activeInstance ?: AuraPlayerController.getOrCreate(carContext.applicationContext)

    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder(
            "While driving not possible to open because in driving mode.\n\n" +
            "Video formats cannot be displayed on the car screen for vehicle and passenger safety. " +
            "You can listen to the audio track of this media safely through the AUDI vehicle sound system."
        )
            .setTitle("Driving Mode • Video Restricted")
            .setHeaderAction(Action.BACK)
            .addAction(
                Action.Builder()
                    .setTitle("▶ Play Audio Only")
                    .setOnClickListener {
                        playerController.setAudioOnlyForVideo(true)
                        playerController.setDrivingWarning("While driving not possible to open video because in driving mode. Playing Audio Only.")
                        playerController.playMedia(videoMedia, queue)
                        screenManager.push(AudiCarNowPlayingScreen(carContext))
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Back to Dashboard")
                    .setOnClickListener {
                        screenManager.pop()
                    }
                    .build()
            )
            .build()
    }
}
