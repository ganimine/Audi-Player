package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettingsManager
import com.example.data.DemoMediaData
import com.example.data.MediaScanner
import com.example.data.db.AppDatabase
import com.example.data.db.PlaybackHistoryEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistItemEntity
import com.example.headset.HeadsetController
import com.example.model.AccentTheme
import com.example.model.ContentTypeFilter
import com.example.model.CustomSkinConfig
import com.example.model.MediaFilterConfig
import com.example.model.MediaItemData
import com.example.model.Playlist
import com.example.model.SizePreset
import com.example.model.SkinPreset
import com.example.model.VisualizerStyle
import com.example.model.WidgetSettingsConfig
import com.example.player.AuraPlayerController
import com.example.widget.AudiPlayerWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val playlistDao = db.playlistDao()

    val settingsManager = AppSettingsManager(application)

    // App Settings: Startup Screen, Video Files Visibility, Search History
    private val _showHomeScreenOnStartup = MutableStateFlow(settingsManager.showHomeScreenOnStartup)
    val showHomeScreenOnStartup: StateFlow<Boolean> = _showHomeScreenOnStartup.asStateFlow()

    private val _defaultStartupScreen = MutableStateFlow(settingsManager.defaultStartupScreen)
    val defaultStartupScreen: StateFlow<Int> = _defaultStartupScreen.asStateFlow()

    private val _rememberLastScreen = MutableStateFlow(settingsManager.rememberLastScreen)
    val rememberLastScreen: StateFlow<Boolean> = _rememberLastScreen.asStateFlow()

    private val _showVideoFiles = MutableStateFlow(settingsManager.showVideoFiles)
    val showVideoFiles: StateFlow<Boolean> = _showVideoFiles.asStateFlow()

    private val _autoPlayVideos = MutableStateFlow(settingsManager.autoPlayVideos)
    val autoPlayVideos: StateFlow<Boolean> = _autoPlayVideos.asStateFlow()

    private val _resumePreviousPlayback = MutableStateFlow(settingsManager.resumePreviousPlayback)
    val resumePreviousPlayback: StateFlow<Boolean> = _resumePreviousPlayback.asStateFlow()

    private val _searchHistoryEnabled = MutableStateFlow(settingsManager.searchHistoryEnabled)
    val searchHistoryEnabled: StateFlow<Boolean> = _searchHistoryEnabled.asStateFlow()

    private val _searchHistory = MutableStateFlow(settingsManager.getSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _isFullScreenSearchOpen = MutableStateFlow(false)
    val isFullScreenSearchOpen: StateFlow<Boolean> = _isFullScreenSearchOpen.asStateFlow()

    // Dedicated Content Type & File-Size Filter Configuration
    private val _mediaFilterConfig = MutableStateFlow(settingsManager.getMediaFilterConfig())
    val mediaFilterConfig: StateFlow<MediaFilterConfig> = _mediaFilterConfig.asStateFlow()

    private val _isFilterSheetOpen = MutableStateFlow(false)
    val isFilterSheetOpen: StateFlow<Boolean> = _isFilterSheetOpen.asStateFlow()

    val playerController = AuraPlayerController.getOrCreate(application)

    // Hardware Headset & Earphones Controller
    val headsetController = HeadsetController(
        context = application,
        onPlayPause = { playerController.togglePlayPause() },
        onNext = { playerController.playNext() },
        onPrevious = { playerController.playPrevious() },
        onIncomingCall = { playerController.interruptionManager.checkCallAndAudioInterruption() }
    )

    // Audio Interruption, Dialer/VoIP Call Auto-Stop & Notification Ducking
    val interruptionManager = playerController.interruptionManager
    val interruptionStatus: StateFlow<String?> = interruptionManager.interruptionStatus
    val isDucked: StateFlow<Boolean> = interruptionManager.isDucked
    val activeCallState: StateFlow<String> = interruptionManager.activeCallState
    val autoResumeOnCallEnd: StateFlow<Boolean> = interruptionManager.autoResumeOnCallEnd
    val duckVolumeFactor: StateFlow<Float> = interruptionManager.duckVolumeFactor
    val dedicatedCallInterruptionOnly: StateFlow<Boolean> = interruptionManager.dedicatedCallInterruptionOnly

    fun setDedicatedCallInterruptionOnly(enabled: Boolean) {
        interruptionManager.setDedicatedCallInterruptionOnly(enabled)
    }

    fun setAutoResumeOnCallEnd(enabled: Boolean) {
        interruptionManager.setAutoResumeOnCallEnd(enabled)
    }

    fun setDuckVolumeFactor(factor: Float) {
        interruptionManager.setDuckVolumeFactor(factor)
    }

    fun clearInterruptionStatus() {
        interruptionManager.clearStatus()
    }

    fun simulateDialerCall() {
        interruptionManager.simulateDialerCall()
    }

    fun simulateInAppVoipCall() {
        interruptionManager.simulateInAppVoipCall()
    }

    fun simulateNotificationDucking() {
        interruptionManager.simulateNotificationDucking()
    }

    // Vehicle Motion & Driving Mode Restriction Manager
    val drivingModeManager = playerController.drivingModeManager
    val isVehicleMoving: StateFlow<Boolean> = drivingModeManager.isVehicleMoving
    val currentSpeedKmh: StateFlow<Float> = drivingModeManager.currentSpeedKmh
    val motionStatusDescription: StateFlow<String> = drivingModeManager.motionStatusDescription
    val isDrivingRestrictionEnabled: StateFlow<Boolean> = drivingModeManager.isDrivingRestrictionEnabled
    val isSimulatingMotion: StateFlow<Boolean> = drivingModeManager.isSimulatingMotion
    val lastDrivingRestrictionMessage: StateFlow<String?> = drivingModeManager.lastRestrictionMessage

    fun setDrivingRestrictionEnabled(enabled: Boolean) {
        drivingModeManager.setDrivingRestrictionEnabled(enabled)
    }

    fun simulateSpeed(speedKmh: Float) {
        drivingModeManager.simulateSpeed(speedKmh)
    }

    fun simulateDrivingCycle(targetSpeedKmh: Float = 55.0f, durationSecs: Int = 15) {
        drivingModeManager.simulateDrivingCycle(targetSpeedKmh, durationSecs)
    }

    fun stopSimulation() {
        drivingModeManager.stopSimulation()
    }

    fun clearDrivingRestrictionMessage() {
        drivingModeManager.clearRestrictionMessage()
    }

    private val audioManager = application.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
    private val _streamVolume = MutableStateFlow(audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 10)
    val streamVolume: StateFlow<Int> = _streamVolume.asStateFlow()
    val maxStreamVolume: Int = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15

    fun updateStreamVolume() {
        audioManager?.let { am ->
            _streamVolume.value = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        }
    }

    fun setStreamVolume(volume: Int) {
        audioManager?.let { am ->
            val clamped = volume.coerceIn(0, maxStreamVolume)
            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, clamped, android.media.AudioManager.FLAG_SHOW_UI)
            _streamVolume.value = clamped
        }
    }

    fun adjustVolumeRelative(delta: Int) {
        audioManager?.let { am ->
            val direction = if (delta > 0) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER
            am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, android.media.AudioManager.FLAG_SHOW_UI)
            _streamVolume.value = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        }
    }

    private val _isHeadsetControlsDialogOpen = MutableStateFlow(false)
    val isHeadsetControlsDialogOpen: StateFlow<Boolean> = _isHeadsetControlsDialogOpen.asStateFlow()

    fun setHeadsetControlsDialogOpen(open: Boolean) {
        _isHeadsetControlsDialogOpen.value = open
    }

    // 4MB-5MB strict size filter toggle
    private val _strict4to5MbFilter = MutableStateFlow(
        _mediaFilterConfig.value.sizePreset == SizePreset.TARGET_4_TO_5_MB
    )
    val strict4to5MbFilter: StateFlow<Boolean> = _strict4to5MbFilter.asStateFlow()

    fun setStrict4to5MbFilter(enabled: Boolean) {
        _strict4to5MbFilter.value = enabled
        val currentConfig = _mediaFilterConfig.value
        val updatedConfig = if (enabled) {
            MediaFilterConfig.fromPreset(currentConfig.contentType, SizePreset.TARGET_4_TO_5_MB)
        } else {
            MediaFilterConfig.fromPreset(currentConfig.contentType, SizePreset.ALL_SIZES)
        }
        _mediaFilterConfig.value = updatedConfig
        settingsManager.saveMediaFilterConfig(updatedConfig)
    }

    fun setFilterSheetOpen(open: Boolean) {
        _isFilterSheetOpen.value = open
    }

    /**
     * Applies content type and file-size filters immediately without interrupting active playback,
     * and automatically closes the filter panel.
     */
    fun applyMediaFilter(config: MediaFilterConfig) {
        if (!config.isValid) return
        _mediaFilterConfig.value = config
        settingsManager.saveMediaFilterConfig(config)
        _strict4to5MbFilter.value = (config.sizePreset == SizePreset.TARGET_4_TO_5_MB)

        // Sync video file visibility if user selected Video or Both
        val videosAllowed = config.contentType == ContentTypeFilter.VIDEO_ONLY ||
                config.contentType == ContentTypeFilter.BOTH
        if (_showVideoFiles.value != videosAllowed) {
            _showVideoFiles.value = videosAllowed
            settingsManager.showVideoFiles = videosAllowed
        }

        // Sync quick chip filter without interrupting playback
        _activeFilter.value = when {
            config.sizePreset == SizePreset.TARGET_4_TO_5_MB -> "4MB - 5MB Only"
            config.contentType == ContentTypeFilter.VIDEO_ONLY -> "Video"
            config.contentType == ContentTypeFilter.AUDIO_ONLY && _activeFilter.value == "Video" -> "All"
            else -> if (_activeFilter.value == "Albums") "Albums" else "All"
        }

        // Update browsable library list on controller without stopping or restarting current media
        val updatedLibrary = _masterMediaList.value.filter { !it.isCallRecording() && config.matches(it) }
        playerController.setLibraryTracks(updatedLibrary)

        // Automatically close the filter panel after applying
        _isFilterSheetOpen.value = false
    }

    /**
     * Resets content and file-size filters to default (Audio Only, All Sizes) without interrupting playback.
     */
    fun resetMediaFilter() {
        val defaultConfig = MediaFilterConfig(
            contentType = if (_showVideoFiles.value) ContentTypeFilter.BOTH else ContentTypeFilter.AUDIO_ONLY,
            sizePreset = SizePreset.ALL_SIZES,
            includeAllSizes = true
        )
        _mediaFilterConfig.value = defaultConfig
        settingsManager.saveMediaFilterConfig(defaultConfig)
        _strict4to5MbFilter.value = false
        _activeFilter.value = "All"
        val updatedLibrary = _masterMediaList.value.filter { !it.isCallRecording() && defaultConfig.matches(it) }
        playerController.setLibraryTracks(updatedLibrary)
        _isFilterSheetOpen.value = false
    }

    // Import status notification message
    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    fun clearImportStatusMessage() {
        _importStatusMessage.value = null
    }

    // Master media list
    private val _masterMediaList = MutableStateFlow<List<MediaItemData>>(DemoMediaData.SAMPLE_TRACKS)
    val masterMediaList: StateFlow<List<MediaItemData>> = _masterMediaList.asStateFlow()

    // Search and filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow("All") // "All", "4MB - 5MB Only", "Audio", "Video", "Albums", "Large Files (>2.5MB)"
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    // Filtered media list combining master list, search query, quick chip filter, video visibility, and MediaFilterConfig
    val filteredMediaList: StateFlow<List<MediaItemData>> = combine(
        _masterMediaList,
        _searchQuery,
        _activeFilter,
        _showVideoFiles,
        _mediaFilterConfig
    ) { list, query, filter, showVideos, filterConfig ->
        list.filter { item ->
            // Always exclude call recording files
            if (item.isCallRecording()) return@filter false

            // Evaluate structured MediaFilterConfig (Content Type + Min/Max File Size)
            val allowsVideoByChipOrConfig = showVideos ||
                    filter == "Video" ||
                    filterConfig.contentType == ContentTypeFilter.VIDEO_ONLY ||
                    filterConfig.contentType == ContentTypeFilter.BOTH

            if (item.isVideo && !allowsVideoByChipOrConfig) {
                return@filter false
            }

            if (filterConfig.contentType == ContentTypeFilter.VIDEO_ONLY && !item.isVideo && filter != "Audio") {
                return@filter false
            }

            if (filterConfig.contentType == ContentTypeFilter.AUDIO_ONLY && item.isVideo && filter != "Video") {
                return@filter false
            }

            // File size check from MediaFilterConfig
            if (!filterConfig.includeAllSizes && filterConfig.sizePreset != SizePreset.ALL_SIZES) {
                if (item.fileSize > 0L && item.fileSize !in filterConfig.minBytes..filterConfig.maxBytes) {
                    return@filter false
                }
            }

            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.artist.contains(query, ignoreCase = true) ||
                    item.album.contains(query, ignoreCase = true) ||
                    item.genre.contains(query, ignoreCase = true) ||
                    item.codec.contains(query, ignoreCase = true) ||
                    item.container.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "4MB - 5MB Only" -> item.isTargetSize4to5Mb
                "Audio" -> !item.isVideo
                "Video" -> item.isVideo
                "Large Files (>2.5MB)" -> item.isLargeFile
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoMediaData.SAMPLE_TRACKS.filter { !it.isVideo })

    // Playlists Flow
    private val _playlists = MutableStateFlow<List<Playlist>>(DemoMediaData.INITIAL_PLAYLISTS)
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Navigation and Sheets State: Default startup screen (Library by default if Home on startup is disabled)
    private val _selectedTab = MutableStateFlow(
        when {
            settingsManager.showHomeScreenOnStartup -> 0
            settingsManager.rememberLastScreen -> {
                if (settingsManager.lastSelectedTab == 0) 1 else settingsManager.lastSelectedTab
            }
            else -> settingsManager.defaultStartupScreen
        }
    )
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _isCarDashboardOpen = MutableStateFlow(false)
    val isCarDashboardOpen: StateFlow<Boolean> = _isCarDashboardOpen.asStateFlow()

    private val _isCodecInfoOpen = MutableStateFlow(false)
    val isCodecInfoOpen: StateFlow<Boolean> = _isCodecInfoOpen.asStateFlow()

    private val _isQueueSheetOpen = MutableStateFlow(false)
    val isQueueSheetOpen: StateFlow<Boolean> = _isQueueSheetOpen.asStateFlow()

    private val _isStreamUrlDialogOpen = MutableStateFlow(false)
    val isStreamUrlDialogOpen: StateFlow<Boolean> = _isStreamUrlDialogOpen.asStateFlow()

    private val _isCreatePlaylistDialogOpen = MutableStateFlow(false)
    val isCreatePlaylistDialogOpen: StateFlow<Boolean> = _isCreatePlaylistDialogOpen.asStateFlow()

    private val _selectedPlaylistForAdd = MutableStateFlow<MediaItemData?>(null)
    val selectedPlaylistForAdd: StateFlow<MediaItemData?> = _selectedPlaylistForAdd.asStateFlow()

    // Customization & Themes
    private val _appName = MutableStateFlow("AUDI Player")
    val appName: StateFlow<String> = _appName.asStateFlow()

    private val _currentSkinPreset = MutableStateFlow(SkinPreset.MIDNIGHT_GLASS)
    val currentSkinPreset: StateFlow<SkinPreset> = _currentSkinPreset.asStateFlow()

    private val _customSkinConfig = MutableStateFlow(CustomSkinConfig())
    val customSkinConfig: StateFlow<CustomSkinConfig> = _customSkinConfig.asStateFlow()

    private val _widgetConfig = MutableStateFlow(WidgetSettingsConfig())
    val widgetConfig: StateFlow<WidgetSettingsConfig> = _widgetConfig.asStateFlow()

    private val _accentTheme = MutableStateFlow(AccentTheme.APPLE_RED)
    val accentTheme: StateFlow<AccentTheme> = _accentTheme.asStateFlow()

    private val _visualizerStyle = MutableStateFlow(VisualizerStyle.WAVEFORM)
    val visualizerStyle: StateFlow<VisualizerStyle> = _visualizerStyle.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        val initialList = if (_showVideoFiles.value) _masterMediaList.value else _masterMediaList.value.filter { !it.isVideo }
        playerController.setLibraryTracks(initialList)
        playerController.setInitialQueue(initialList)
        loadPersistedPlaylists()
        observeFavoritesAndHistory()
        observeWidgetSync()
    }

    fun setShowHomeScreenOnStartup(enabled: Boolean) {
        _showHomeScreenOnStartup.value = enabled
        settingsManager.showHomeScreenOnStartup = enabled
    }

    fun setDefaultStartupScreen(screen: Int) {
        _defaultStartupScreen.value = screen
        settingsManager.defaultStartupScreen = screen
    }

    fun setRememberLastScreen(enabled: Boolean) {
        _rememberLastScreen.value = enabled
        settingsManager.rememberLastScreen = enabled
    }

    fun setShowVideoFiles(enabled: Boolean) {
        _showVideoFiles.value = enabled
        settingsManager.showVideoFiles = enabled
        val list = if (enabled) _masterMediaList.value else _masterMediaList.value.filter { !it.isVideo }
        playerController.setLibraryTracks(list)
    }

    fun setAutoPlayVideos(enabled: Boolean) {
        _autoPlayVideos.value = enabled
        settingsManager.autoPlayVideos = enabled
    }

    fun setResumePreviousPlayback(enabled: Boolean) {
        _resumePreviousPlayback.value = enabled
        settingsManager.resumePreviousPlayback = enabled
    }

    fun setSearchHistoryEnabled(enabled: Boolean) {
        _searchHistoryEnabled.value = enabled
        settingsManager.searchHistoryEnabled = enabled
    }

    fun setFullScreenSearchOpen(open: Boolean) {
        _isFullScreenSearchOpen.value = open
    }

    fun addSearchHistory(query: String) {
        settingsManager.addSearchQuery(query)
        _searchHistory.value = settingsManager.getSearchHistory()
    }

    fun removeSearchHistory(query: String) {
        settingsManager.removeSearchQuery(query)
        _searchHistory.value = settingsManager.getSearchHistory()
    }

    fun clearSearchHistory() {
        settingsManager.clearSearchHistory()
        _searchHistory.value = emptyList()
    }

    private fun observeWidgetSync() {
        viewModelScope.launch {
            combine(
                playerController.currentMedia,
                playerController.isPlaying,
                playerController.positionMs
            ) { media, playing, pos ->
                AudiPlayerWidgetProvider.updateAllWidgets(
                    getApplication(),
                    media,
                    playing,
                    pos,
                    playerController.durationMs.value
                )
            }.collect {}
        }
    }

    private fun loadPersistedPlaylists() {
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { entities ->
                if (entities.isNotEmpty()) {
                    val loaded = entities.map { entity ->
                        Playlist(
                            id = entity.id,
                            name = entity.name,
                            description = entity.description,
                            coverGradientIndex = entity.coverGradientIndex,
                            createdAt = entity.createdAt
                        )
                    }
                    _playlists.value = loaded
                } else {
                    // Seed initial playlists
                    DemoMediaData.INITIAL_PLAYLISTS.forEach { p ->
                        playlistDao.insertPlaylist(
                            PlaylistEntity(
                                id = p.id,
                                name = p.name,
                                description = p.description,
                                coverGradientIndex = p.coverGradientIndex,
                                createdAt = p.createdAt
                            )
                        )
                        p.mediaIds.forEachIndexed { idx, mId ->
                            playlistDao.insertPlaylistItem(PlaylistItemEntity(p.id, mId, idx))
                        }
                    }
                }
            }
        }
    }

    private fun observeFavoritesAndHistory() {
        viewModelScope.launch {
            playlistDao.getFavorites().collect { favs ->
                val favIds = favs.map { it.mediaId }.toSet()
                _masterMediaList.value = _masterMediaList.value.map { item ->
                    item.copy(isFavorite = favIds.contains(item.id))
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        if (_rememberLastScreen.value) {
            settingsManager.lastSelectedTab = tabIndex
        }
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun setCarDashboardOpen(open: Boolean) {
        _isCarDashboardOpen.value = open
    }

    fun setCodecInfoOpen(open: Boolean) {
        _isCodecInfoOpen.value = open
    }

    fun setQueueSheetOpen(open: Boolean) {
        _isQueueSheetOpen.value = open
    }

    fun setStreamUrlDialogOpen(open: Boolean) {
        _isStreamUrlDialogOpen.value = open
    }

    fun setCreatePlaylistDialogOpen(open: Boolean) {
        _isCreatePlaylistDialogOpen.value = open
    }

    fun setSelectedPlaylistForAdd(media: MediaItemData?) {
        _selectedPlaylistForAdd.value = media
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun setAccentTheme(theme: AccentTheme) {
        _accentTheme.value = theme
    }

    fun setAppName(name: String) {
        if (name.isNotBlank()) _appName.value = name
    }

    fun setSkinPreset(preset: com.example.model.SkinPreset) {
        _currentSkinPreset.value = preset
    }

    fun updateCustomSkin(config: com.example.model.CustomSkinConfig) {
        _customSkinConfig.value = config
    }

    fun updateWidgetConfig(config: WidgetSettingsConfig) {
        _widgetConfig.value = config
        AudiPlayerWidgetProvider.updateAllWidgets(
            getApplication(),
            playerController.currentMedia.value,
            playerController.isPlaying.value,
            playerController.positionMs.value,
            playerController.durationMs.value
        )
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        _visualizerStyle.value = style
    }

    fun playMedia(media: MediaItemData, customQueue: List<MediaItemData>? = null) {
        if (media.isVideo) {
            val allowed = playerController.drivingModeManager.checkVideoAllowedAndNotify(showToast = true)
            if (!allowed) {
                playerController.setAudioOnlyForVideo(true)
                playerController.setDrivingWarning(
                    "While driving not possible to open video because in driving mode. Playing Audio Only."
                )
            } else {
                playerController.setAudioOnlyForVideo(false)
                playerController.clearDrivingWarning()
            }
        } else {
            playerController.setAudioOnlyForVideo(false)
            playerController.clearDrivingWarning()
        }
        val queueToUse = customQueue ?: _masterMediaList.value
        playerController.playMedia(media, queueToUse)
        viewModelScope.launch {
            playlistDao.updateHistory(
                PlaybackHistoryEntity(
                    mediaId = media.id,
                    lastPositionMs = 0L,
                    lastPlayedAt = System.currentTimeMillis(),
                    isFavorite = media.isFavorite
                )
            )
        }
    }

    fun toggleFavorite(media: MediaItemData) {
        val newFav = !media.isFavorite
        viewModelScope.launch {
            playlistDao.setFavorite(media.id, newFav)
            _masterMediaList.value = _masterMediaList.value.map {
                if (it.id == media.id) it.copy(isFavorite = newFav) else it
            }
        }
    }

    /**
     * Import a file chosen via SAF file picker.
     * Enforces size restriction (4MB-5MB) and rejects call recordings.
     */
    fun importFileUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val media = MediaScanner.resolveMediaFromUri(
                    getApplication(),
                    uri,
                    strict4to5Mb = _strict4to5MbFilter.value,
                    excludeCallRecordings = true
                )
                val current = _masterMediaList.value.toMutableList()
                current.add(0, media)
                _masterMediaList.value = current
                val audioFiltered = if (_showVideoFiles.value) current else current.filter { !it.isVideo }
                playerController.setLibraryTracks(audioFiltered)
                _importStatusMessage.value = "Imported: ${media.title} (${media.formattedFileSize})"
                playMedia(media)
                _isNowPlayingExpanded.value = true
            } catch (e: Exception) {
                _importStatusMessage.value = e.message ?: "Failed to import selected media."
            }
        }
    }

    fun openNetworkStream(url: String, title: String = "Network Stream") {
        if (url.isBlank()) return
        val isVideo = url.contains(".mp4") || url.contains(".mkv") || url.contains(".m3u8") || url.contains("video")
        val streamItem = MediaItemData(
            id = "stream_${UUID.randomUUID()}",
            title = if (title.isBlank()) "Stream (${url.substringAfterLast('/').take(20)})" else title,
            artist = "Network Stream / URL",
            album = "Live & Remote Media",
            durationMs = 0L,
            uri = url.trim(),
            isVideo = isVideo,
            fileSize = 0L,
            bitrate = "Adaptive HLS/DASH/Direct",
            resolution = if (isVideo) "Dynamic" else null,
            codec = if (isVideo) "H.264 / AAC Stream" else "AAC / MP3 Stream",
            container = "STREAM",
            sampleRate = "48 kHz",
            gradientColorIndex = 4
        )
        val current = _masterMediaList.value.toMutableList()
        current.add(0, streamItem)
        _masterMediaList.value = current
        val audioFiltered = if (_showVideoFiles.value) current else current.filter { !it.isVideo }
        playerController.setLibraryTracks(audioFiltered)
        playMedia(streamItem)
        _isNowPlayingExpanded.value = true
    }

    fun scanDeviceStorage(silent: Boolean = false) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val localItems = MediaScanner.queryDeviceMedia(
                    getApplication(),
                    strict4to5Mb = _strict4to5MbFilter.value,
                    excludeCallRecordings = true,
                    includeVideos = _showVideoFiles.value
                )
                if (localItems.isNotEmpty()) {
                    val combined = (localItems + _masterMediaList.value).distinctBy { it.uri }
                    _masterMediaList.value = combined
                    val audioFiltered = if (_showVideoFiles.value) combined else combined.filter { !it.isVideo }
                    playerController.setLibraryTracks(audioFiltered)
                    playerController.setInitialQueue(audioFiltered)
                    if (!silent) {
                        _importStatusMessage.value = "Scanned: Added ${localItems.size} tracks (4MB-5MB only, no call recordings)"
                    }
                } else {
                    if (!silent) {
                        _importStatusMessage.value = if (_strict4to5MbFilter.value) {
                            "Scan complete: No 4MB-5MB audio/video tracks found on device."
                        } else {
                            "Scan complete: No media found."
                        }
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _importStatusMessage.value = "Scan error: ${e.message}"
                }
            }
            _isScanning.value = false
        }
    }

    fun createPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = "pl_${UUID.randomUUID()}"
            val entity = PlaylistEntity(
                id = id,
                name = name,
                description = description,
                coverGradientIndex = (name.hashCode().coerceAtLeast(0)) % 6,
                createdAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylist(entity)
        }
    }

    fun addToPlaylist(playlistId: String, mediaId: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    mediaId = mediaId,
                    orderIndex = (System.currentTimeMillis() % 1000).toInt()
                )
            )
        }
    }

    fun removeFromPlaylist(playlistId: String, mediaId: String) {
        viewModelScope.launch {
            playlistDao.removeItemFromPlaylist(playlistId, mediaId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlistId)
            playlistDao.clearPlaylist(playlistId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
        headsetController.release()
    }
}
