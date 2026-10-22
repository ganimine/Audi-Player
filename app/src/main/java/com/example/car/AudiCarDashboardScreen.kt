package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.DemoMediaData
import com.example.model.MediaItemData
import com.example.player.AuraPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android Auto Car Dashboard Screen for AUDI Player.
 * Displays interactive vehicle dashboard with Now Playing hero status,
 * car library category shortcuts, 1-tap song playback, and driving safety compliance.
 */
class AudiCarDashboardScreen(carContext: CarContext) : Screen(carContext) {

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
                screenScope.launch {
                    playerController.libraryTracks.collect {
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
        val rawTracks = when {
            playerController.libraryTracks.value.isNotEmpty() -> playerController.libraryTracks.value
            playerController.queue.value.isNotEmpty() -> playerController.queue.value
            else -> DemoMediaData.SAMPLE_TRACKS
        }
        // Exclude video files completely from driving mode templates
        val audioTracks = rawTracks.filter { !it.isVideo }

        val current = playerController.currentMedia.value
        val isPlaying = playerController.isPlaying.value

        val itemListBuilder = ItemList.Builder()

        // 1. Top Hero / Quick Player Status row
        if (current != null && !current.isVideo) {
            val statusIcon = if (isPlaying) "▶ " else "❚❚ "
            val heroRow = Row.Builder()
                .setTitle("$statusIcon${current.title}")
                .addText("${current.artist} • ${current.codec} • ${current.bitrate}")
                .addText(if (isPlaying) "Playing in vehicle • Tap to open controls" else "Paused • Tap to resume")
                .setOnClickListener {
                    if (!isPlaying) {
                        playerController.togglePlayPause()
                    }
                    screenManager.push(AudiCarNowPlayingScreen(carContext))
                }
                .build()
            itemListBuilder.addItem(heroRow)
        } else {
            val startRow = Row.Builder()
                .setTitle("▶ Start AUDI Sound Engine")
                .addText("Bang & Olufsen 3D Sound • Master Lossless Audio")
                .addText("Tap to begin playback with curated lossless demo")
                .setOnClickListener {
                    val firstTrack = audioTracks.firstOrNull() ?: DemoMediaData.SAMPLE_TRACKS.first { !it.isVideo }
                    playerController.playMedia(firstTrack, audioTracks)
                    screenManager.push(AudiCarNowPlayingScreen(carContext))
                }
                .build()
            itemListBuilder.addItem(startRow)
        }

        // 2. Library Categories Shortcuts for Vehicle Browsing
        val browseAllRow = Row.Builder()
            .setTitle("🎵 All Songs Library (${audioTracks.size} Tracks)")
            .addText("Browse all scanned audio files and high-res masters")
            .setOnClickListener {
                screenManager.push(AudiCarLibraryScreen(carContext, "All Songs"))
            }
            .build()
        itemListBuilder.addItem(browseAllRow)

        val strictSizeRow = Row.Builder()
            .setTitle("⚡ 4MB - 5MB Audio Tracks")
            .addText("Strict file size filter • Call recordings excluded")
            .setOnClickListener {
                screenManager.push(AudiCarLibraryScreen(carContext, "4MB - 5MB"))
            }
            .build()
        itemListBuilder.addItem(strictSizeRow)

        val playlistsRow = Row.Builder()
            .setTitle("📁 Curated Playlists & Albums")
            .addText("Favorites, Hi-Res Albums & Custom Playlists")
            .setOnClickListener {
                screenManager.push(AudiCarLibraryScreen(carContext, "Playlists"))
            }
            .build()
        itemListBuilder.addItem(playlistsRow)

        // 3. Direct 1-Tap Quick Play Song Rows (up to 4 songs to fit Android Auto template limits)
        audioTracks.take(4).forEach { mediaItem ->
            val isCurrent = current?.id == mediaItem.id
            val prefix = if (isCurrent) {
                if (isPlaying) "▶ " else "❚❚ "
            } else ""

            val row = Row.Builder()
                .setTitle("$prefix${mediaItem.title}")
                .addText("${mediaItem.artist} • ${mediaItem.formattedDuration} • ${mediaItem.codec}")
                .setOnClickListener {
                    playerController.playMedia(mediaItem, audioTracks)
                    screenManager.push(AudiCarNowPlayingScreen(carContext))
                }
                .build()

            itemListBuilder.addItem(row)
        }

        // ActionStrip for fast dashboard navigation and playback control
        val actionStripBuilder = ActionStrip.Builder()

        actionStripBuilder.addAction(
            Action.Builder()
                .setTitle("Now Playing")
                .setOnClickListener {
                    screenManager.push(AudiCarNowPlayingScreen(carContext))
                }
                .build()
        )

        actionStripBuilder.addAction(
            Action.Builder()
                .setTitle(if (isPlaying) "❚❚ Pause" else "▶ Play")
                .setOnClickListener {
                    if (current == null) {
                        val firstTrack = audioTracks.firstOrNull() ?: DemoMediaData.SAMPLE_TRACKS.first { !it.isVideo }
                        playerController.playMedia(firstTrack, audioTracks)
                    } else {
                        playerController.togglePlayPause()
                    }
                    invalidate()
                }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("AUDI Player Dashboard")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemListBuilder.build())
            .setActionStrip(actionStripBuilder.build())
            .build()
    }
}
