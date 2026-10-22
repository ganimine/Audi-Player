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
 * Screen displaying the car audio library categories and tracks on the Android Auto car screen.
 * Supports "All Songs", "4MB - 5MB", and "Playlists" categories with oversized touch targets.
 */
class AudiCarLibraryScreen(
    carContext: CarContext,
    private val category: String = "All Songs"
) : Screen(carContext) {

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
        val rawTracks = when {
            playerController.libraryTracks.value.isNotEmpty() -> playerController.libraryTracks.value
            playerController.queue.value.isNotEmpty() -> playerController.queue.value
            else -> DemoMediaData.SAMPLE_TRACKS
        }
        val audioTracks = rawTracks.filter { !it.isVideo }

        val tracks = when (category) {
            "4MB - 5MB" -> audioTracks.filter { it.isTargetSize4to5Mb }
            else -> audioTracks
        }

        val current = playerController.currentMedia.value
        val isPlaying = playerController.isPlaying.value

        val itemListBuilder = ItemList.Builder()

        if (category == "Playlists") {
            // Curated albums / playlists view
            val albumGroups = audioTracks.groupBy { it.album }
            albumGroups.entries.take(6).forEach { (albumName, albumTracks) ->
                val sampleTrack = albumTracks.first()
                val row = Row.Builder()
                    .setTitle(albumName)
                    .addText("${sampleTrack.artist} • ${albumTracks.size} tracks")
                    .setOnClickListener {
                        playerController.playMedia(sampleTrack, albumTracks)
                        screenManager.push(AudiCarNowPlayingScreen(carContext))
                    }
                    .build()
                itemListBuilder.addItem(row)
            }
        } else {
            // Track list (take up to 6 tracks to safely stay within Android Auto template limits)
            tracks.take(6).forEach { mediaItem ->
                val isCurrent = current?.id == mediaItem.id
                val prefix = if (isCurrent) {
                    if (isPlaying) "▶ " else "❚❚ "
                } else ""

                val subtitleText = "${mediaItem.artist} • ${mediaItem.formattedDuration} • ${mediaItem.codec}"

                val row = Row.Builder()
                    .setTitle("$prefix${mediaItem.title}")
                    .addText(subtitleText)
                    .setOnClickListener {
                        playerController.playMedia(mediaItem, tracks)
                        screenManager.push(AudiCarNowPlayingScreen(carContext))
                    }
                    .build()

                itemListBuilder.addItem(row)
            }
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Now Playing")
                    .setOnClickListener {
                        screenManager.push(AudiCarNowPlayingScreen(carContext))
                    }
                    .build()
            )
            .build()

        val title = when (category) {
            "4MB - 5MB" -> "4MB - 5MB Audio Tracks"
            "Playlists" -> "Curated Playlists & Albums"
            else -> "AUDI Audio Library"
        }

        return ListTemplate.Builder()
            .setTitle(title)
            .setHeaderAction(Action.BACK)
            .setSingleList(itemListBuilder.build())
            .setActionStrip(actionStrip)
            .build()
    }
}
