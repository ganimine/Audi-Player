package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.DemoMediaData
import com.example.model.MediaItemData
import com.example.player.AuraPlayerController
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MediaLibraryService providing full Android Auto USB / Wireless support,
 * car dashboard touch screen browse & playback controls (Play, Pause, Skip Next, Skip Previous),
 * steering wheel buttons, and lockscreen/notification MediaStyle controls with volume adjustments.
 */
@OptIn(UnstableApi::class)
class AudiMediaPlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var notificationManager: NotificationManager? = null
    private var audioManager: AudioManager? = null

    private var currentAlbumArtBitmap: Bitmap? = null
    private var lastArtUrl: String? = null

    // Android Auto hierarchy constants
    private val ROOT_ID = "[ROOT]"
    private val TAB_SONGS_ID = "[SONGS]"
    private val TAB_PLAYLISTS_ID = "[PLAYLISTS]"
    private val TAB_VIDEOS_ID = "[VIDEOS]"
    private val TAB_4TO5MB_ID = "[4TO5MB]"

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        createNotificationChannel()

        val controller = AuraPlayerController.activeInstance ?: AuraPlayerController.getOrCreate(applicationContext)
        if (controller.queue.value.isEmpty()) {
            controller.setInitialQueue(DemoMediaData.SAMPLE_TRACKS)
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val libraryCallback = createLibraryCallback(controller)

        mediaLibrarySession = MediaLibrarySession.Builder(this, controller.exoPlayer, libraryCallback)
            .setSessionActivity(sessionActivityPendingIntent)
            .setId("AudiPlayerSession")
            .build()

        startForegroundServiceWithNotification(controller)
        observePlayerChanges(controller)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AUDI Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows now playing music notification with playback and audio controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun observePlayerChanges(controller: AuraPlayerController) {
        serviceScope.launch {
            controller.isPlaying.collectLatest {
                updateNotification(controller)
            }
        }

        serviceScope.launch {
            controller.currentMedia.collectLatest { media ->
                loadArtwork(media)
                updateNotification(controller)
            }
        }

        serviceScope.launch {
            controller.repeatMode.collectLatest {
                updateNotification(controller)
            }
        }

        serviceScope.launch {
            controller.isShuffle.collectLatest {
                updateNotification(controller)
            }
        }

        serviceScope.launch {
            controller.drivingWarning.collectLatest {
                updateNotification(controller)
            }
        }
    }

    private suspend fun loadArtwork(media: MediaItemData?) {
        val artPath = media?.artworkUrl
        if (artPath.isNullOrEmpty()) {
            currentAlbumArtBitmap = null
            lastArtUrl = null
            return
        }

        if (artPath == lastArtUrl && currentAlbumArtBitmap != null) {
            return
        }

        lastArtUrl = artPath
        currentAlbumArtBitmap = withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(artPath)
                if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path)
                } else if (uri.scheme == "content") {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else if (uri.scheme == "http" || uri.scheme == "https") {
                    val connection = java.net.URL(artPath).openConnection()
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.getInputStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val file = File(artPath)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = AuraPlayerController.activeInstance ?: AuraPlayerController.getOrCreate(applicationContext)

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                controller.togglePlayPause()
                updateNotification(controller)
            }
            ACTION_NEXT -> {
                controller.playNext()
                updateNotification(controller)
            }
            ACTION_PREVIOUS -> {
                controller.playPrevious()
                updateNotification(controller)
            }
            ACTION_VOL_UP -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                updateNotification(controller)
            }
            ACTION_VOL_DOWN -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                updateNotification(controller)
            }
            ACTION_STOP_SERVICE -> {
                controller.exoPlayer.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundServiceWithNotification(controller: AuraPlayerController) {
        val notification = buildMediaNotification(controller)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }

    fun updateNotification(controller: AuraPlayerController) {
        val notification = buildMediaNotification(controller)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildMediaNotification(controller: AuraPlayerController): Notification {
        val media = controller.currentMedia.value
        val isPlaying = controller.isPlaying.value

        val title = media?.title ?: "AUDI Player"
        val artist = media?.artist ?: "High-Fidelity Audio Engine"

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // PendingIntents for actions
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudiMediaPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AudiMediaPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AudiMediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val volDownPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, AudiMediaPlaybackService::class.java).apply { action = ACTION_VOL_DOWN },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val volUpPendingIntent = PendingIntent.getService(
            this,
            5,
            Intent(this, AudiMediaPlaybackService::class.java).apply { action = ACTION_VOL_UP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            6,
            Intent(this, AudiMediaPlaybackService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Get current system stream volume level
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val volPct = if (maxVol > 0) ((currentVol.toFloat() / maxVol) * 100).toInt() else 100

        // Subtext / info line
        val drivingWarning = controller.drivingWarning.value
        val subText = if (drivingWarning != null) {
            "Driving Mode • Video blocked • Audio Only"
        } else if (media != null) {
            "${media.codec} • ${media.formattedFileSize} • Vol $volPct%"
        } else {
            "AUDI Engine • Vol $volPct%"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(subText)
            .setSmallIcon(R.drawable.ic_audi_music_note)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)

        if (currentAlbumArtBitmap != null) {
            builder.setLargeIcon(currentAlbumArtBitmap)
        }

        // Action indices:
        // 0: Previous
        // 1: Play/Pause
        // 2: Next
        // 3: Volume Down (Audio Control)
        // 4: Volume Up (Audio Control)
        builder.addAction(R.drawable.ic_skip_previous, "Previous", prevPendingIntent)

        if (isPlaying) {
            builder.addAction(R.drawable.ic_pause, "Pause", playPausePendingIntent)
        } else {
            builder.addAction(R.drawable.ic_play, "Play", playPausePendingIntent)
        }

        builder.addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent)
        builder.addAction(R.drawable.ic_volume_down, "Vol -", volDownPendingIntent)
        builder.addAction(R.drawable.ic_volume_up, "Vol +", volUpPendingIntent)

        // Configure MediaStyle linked to the active MediaSession
        val session = mediaLibrarySession
        if (session != null) {
            val mediaStyle = MediaStyleNotificationHelper.MediaStyle(session)
                .setShowActionsInCompactView(0, 1, 2)
            builder.setStyle(mediaStyle)
        }

        return builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    /**
     * MediaLibrarySession.Callback implementation powering Android Auto car screen browsing,
     * track list browsing, and car steering wheel / touch screen controls.
     */
    private fun createLibraryCallback(controller: AuraPlayerController): MediaLibrarySession.Callback {
        return object : MediaLibrarySession.Callback {

            override fun onConnect(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                val availablePlayerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SET_SPEED_AND_PITCH)
                    .add(Player.COMMAND_SET_SHUFFLE_MODE)
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableSessionCommands.build())
                    .setAvailablePlayerCommands(availablePlayerCommands)
                    .build()
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                // Media Browser Auto Styling: Informs Android Auto of content styling and button reservations
                val autoExtras = Bundle().apply {
                    putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                    putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1) // 1: List hint for categories
                    putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1) // 1: List hint for playable tracks
                    putBoolean("android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS", true)
                    putBoolean("android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT", true)
                    putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
                }

                val libraryParams = LibraryParams.Builder()
                    .setExtras(autoExtras)
                    .build()

                val rootMetadata = MediaMetadata.Builder()
                    .setTitle("AUDI Player")
                    .setDisplayTitle("AUDI Player")
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setExtras(autoExtras)
                    .build()

                val rootMediaItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(rootMetadata)
                    .build()

                return Futures.immediateFuture(LibraryResult.ofItem(rootMediaItem, libraryParams))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val items = mutableListOf<MediaItem>()

                when (parentId) {
                    ROOT_ID -> {
                        // Categories for Android Auto with rich content styling (Driving-safe, audio only)
                        items.add(createFolderItem(TAB_SONGS_ID, "All Songs", "Audio Library", isGrid = false))
                        items.add(createFolderItem(TAB_PLAYLISTS_ID, "Curated Playlists & Albums", "Favorites & Lossless Albums", isGrid = true))
                        items.add(createFolderItem(TAB_4TO5MB_ID, "4MB - 5MB Audio Files", "Strict Size Filter Tracks", isGrid = false))
                    }
                    TAB_SONGS_ID -> {
                        val allTracks = getAvailableTracks(controller)
                        allTracks.filter { !it.isVideo }.forEach { media ->
                            items.add(media.toMedia3MediaItem())
                        }
                    }
                    TAB_PLAYLISTS_ID -> {
                        DemoMediaData.INITIAL_PLAYLISTS.forEach { playlist ->
                            items.add(createFolderItem("[PLAYLIST_${playlist.id}]", playlist.name, playlist.description, isGrid = true))
                        }
                    }
                    TAB_4TO5MB_ID -> {
                        val allTracks = getAvailableTracks(controller)
                        allTracks.filter { it.isTargetSize4to5Mb && !it.isVideo }.forEach { media ->
                            items.add(media.toMedia3MediaItem())
                        }
                    }
                    else -> {
                        if (parentId.startsWith("[PLAYLIST_")) {
                            val playlistId = parentId.removePrefix("[PLAYLIST_").removeSuffix("]")
                            val playlist = DemoMediaData.INITIAL_PLAYLISTS.firstOrNull { it.id == playlistId }
                            val allTracks = getAvailableTracks(controller)
                            playlist?.mediaIds?.forEach { mId ->
                                allTracks.firstOrNull { it.id == mId }?.let { media ->
                                    items.add(media.toMedia3MediaItem())
                                }
                            }
                        } else {
                            // Check if a specific track is requested
                            val allTracks = getAvailableTracks(controller)
                            allTracks.firstOrNull { it.id == parentId }?.let { media ->
                                items.add(media.toMedia3MediaItem())
                            }
                        }
                    }
                }

                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val allTracks = getAvailableTracks(controller)
                val found = allTracks.firstOrNull { it.id == mediaId }
                return if (found != null) {
                    Futures.immediateFuture(LibraryResult.ofItem(found.toMedia3MediaItem(), null))
                } else {
                    Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val allTracks = getAvailableTracks(controller)
                val resolvedItems = mediaItems.map { requestedItem ->
                    val found = allTracks.firstOrNull { it.id == requestedItem.mediaId }
                    found?.toMedia3MediaItem() ?: requestedItem
                }

                // If user selected a track in Android Auto car screen, sync current track in controller
                val targetIndex = if (startIndex in resolvedItems.indices) startIndex else 0
                val targetItem = resolvedItems.getOrNull(targetIndex)
                if (targetItem != null) {
                    val matchingMediaData = allTracks.firstOrNull { it.id == targetItem.mediaId }
                    if (matchingMediaData != null) {
                        if (matchingMediaData.isVideo) {
                            // User selected a video on car screen: switch to audio-only mode with driving safety notice
                            controller.setAudioOnlyForVideo(true)
                            controller.setDrivingWarning("While driving not possible to open video because in driving mode. Playing Audio Only.")
                            controller.drivingModeManager.notifyProhibitedVideoInteraction(
                                "⚠️ While driving not possible to open video because in driving mode. Switched to Audio Only playback."
                            )
                        } else {
                            controller.clearDrivingWarning()
                        }
                        controller.playMedia(matchingMediaData, allTracks)
                    }
                }

                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        resolvedItems,
                        targetIndex,
                        startPositionMs
                    )
                )
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                val allTracks = getAvailableTracks(controller)
                val resolvedItems = mediaItems.map { requestedItem ->
                    val found = allTracks.firstOrNull { it.id == requestedItem.mediaId }
                    found?.toMedia3MediaItem() ?: requestedItem
                }.toMutableList()
                return Futures.immediateFuture(resolvedItems)
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controllerInfo: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val allTracks = getAvailableTracks(controller)
                val current = controller.currentMedia.value ?: allTracks.firstOrNull()
                val items = if (allTracks.isNotEmpty()) {
                    allTracks.map { it.toMedia3MediaItem() }
                } else if (current != null) {
                    listOf(current.toMedia3MediaItem())
                } else {
                    emptyList()
                }
                val index = if (current != null) {
                    val found = allTracks.indexOfFirst { it.id == current.id }
                    if (found >= 0) found else 0
                } else 0
                val posMs = controller.positionMs.value

                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        items,
                        index,
                        posMs
                    )
                )
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                // Ensure all car touchscreen and steering wheel playback commands succeed cleanly
                when (playerCommand) {
                    Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        if (!controller.exoPlayer.hasNextMediaItem()) {
                            controller.playNext()
                        }
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        if (!controller.exoPlayer.hasPreviousMediaItem() || controller.exoPlayer.currentPosition <= 3000L) {
                            controller.playPrevious()
                        }
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_PLAY_PAUSE -> {
                        controller.togglePlayPause()
                        return SessionResult.RESULT_SUCCESS
                    }
                }
                return SessionResult.RESULT_SUCCESS
            }

            override fun onSearch(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<Void>> {
                session.notifySearchResultChanged(browser, query, getAvailableTracks(controller).size, params)
                return Futures.immediateFuture(LibraryResult.ofVoid(params))
            }

            override fun onGetSearchResult(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val allTracks = getAvailableTracks(controller)
                val filtered = allTracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }.map { it.toMedia3MediaItem() }
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(filtered), params))
            }

            override fun onCustomCommand(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }
    }

    private fun createFolderItem(id: String, title: String, subtitle: String, isGrid: Boolean = false): MediaItem {
        val folderExtras = Bundle().apply {
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", if (isGrid) 2 else 1)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", if (isGrid) 2 else 1)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setSubtitle(subtitle)
            .setIsPlayable(false)
            .setIsBrowsable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .setExtras(folderExtras)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun getAvailableTracks(controller: AuraPlayerController): List<MediaItemData> {
        val tracks = when {
            controller.libraryTracks.value.isNotEmpty() -> controller.libraryTracks.value
            controller.queue.value.isNotEmpty() -> controller.queue.value
            else -> DemoMediaData.SAMPLE_TRACKS
        }
        return tracks.filter { !it.isVideo }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.run {
            // Note: Never release controller's ExoPlayer here - AuraPlayerController owns player lifecycle
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "audi_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.service.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.service.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.service.ACTION_PREVIOUS"
        const val ACTION_VOL_UP = "com.example.service.ACTION_VOL_UP"
        const val ACTION_VOL_DOWN = "com.example.service.ACTION_VOL_DOWN"
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, AudiMediaPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudiMediaPlaybackService::class.java)
            context.stopService(intent)
        }
    }
}
