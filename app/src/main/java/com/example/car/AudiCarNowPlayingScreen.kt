package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.DemoMediaData
import com.example.player.AuraPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android Auto Screen providing in-car touch screen Now Playing controls:
 * - Play / Pause
 * - Skip Next
 * - Skip Previous
 * - Real-time metadata: Title, Artist, Album, Bitrate & Codec
 * - Auto-reactive lifecycle updates
 */
class AudiCarNowPlayingScreen(carContext: CarContext) : Screen(carContext) {

    private val playerController: AuraPlayerController
        get() = AuraPlayerController.activeInstance ?: AuraPlayerController.getOrCreate(carContext.applicationContext)

    private val screenScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                screenScope.launch {
                    playerController.currentMedia.collect {
                        invalidate()
                    }
                }
                screenScope.launch {
                    playerController.isPlaying.collect {
                        invalidate()
                    }
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                screenScope.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val current = playerController.currentMedia.value
        val isPlaying = playerController.isPlaying.value

        if (current == null) {
            return MessageTemplate.Builder("No Media Playing")
                .setTitle("AUDI Player Dashboard")
                .setHeaderAction(Action.BACK)
                .addAction(
                    Action.Builder()
                        .setTitle("Select Song")
                        .setOnClickListener {
                            screenManager.pop()
                        }
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle("▶ Play Demo")
                        .setOnClickListener {
                            val demo = DemoMediaData.SAMPLE_TRACKS.first()
                            playerController.playMedia(demo, DemoMediaData.SAMPLE_TRACKS)
                            invalidate()
                        }
                        .build()
                )
                .build()
        }

        val videoModeNotice = if (current.isVideo) " • Audio Only (Driving Safe)" else " • Master Lossless"
        val statusText = if (isPlaying) "Playing$videoModeNotice" else "Paused$videoModeNotice"

        val metadataRow = Row.Builder()
            .setTitle(current.title)
            .addText("${current.artist} — ${current.album}")
            .addText("$statusText • ${current.codec} • ${current.bitrate}")
            .build()

        val paneBuilder = Pane.Builder()
            .addRow(metadataRow)
            .addAction(
                Action.Builder()
                    .setTitle(if (isPlaying) "❚❚ Pause" else "▶ Play")
                    .setOnClickListener {
                        playerController.togglePlayPause()
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("⏭ Next")
                    .setOnClickListener {
                        playerController.playNext()
                        invalidate()
                    }
                    .build()
            )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("⏮ Prev")
                    .setOnClickListener {
                        playerController.playPrevious()
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Dashboard")
                    .setOnClickListener {
                        screenManager.pop()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("AUDI Player Dashboard")
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }
}
