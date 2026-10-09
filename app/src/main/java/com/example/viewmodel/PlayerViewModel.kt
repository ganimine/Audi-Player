package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DemoMediaData
import com.example.data.MediaScanner
import com.example.data.db.AppDatabase
import com.example.data.db.PlaybackHistoryEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistItemEntity
import com.example.model.AccentTheme
import com.example.model.CustomSkinConfig
import com.example.model.MediaItemData
import com.example.model.Playlist
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

    val playerController = AuraPlayerController(application)

    // Master media list
    private val _masterMediaList = MutableStateFlow<List<MediaItemData>>(DemoMediaData.SAMPLE_TRACKS)
    val masterMediaList: StateFlow<List<MediaItemData>> = _masterMediaList.asStateFlow()

    // Search and filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow("All") // "All", "Audio", "Video", "Large Files (>2.5MB)"
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    // Filtered media list
    val filteredMediaList: StateFlow<List<MediaItemData>> = combine(
        _masterMediaList,
        _searchQuery,
        _activeFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.artist.contains(query, ignoreCase = true) ||
                    item.codec.contains(query, ignoreCase = true) ||
                    item.container.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "Audio" -> !item.isVideo
                "Video" -> item.isVideo
                "Large Files (>2.5MB)" -> item.isLargeFile
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoMediaData.SAMPLE_TRACKS)

    // Playlists Flow
    private val _playlists = MutableStateFlow<List<Playlist>>(DemoMediaData.INITIAL_PLAYLISTS)
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Navigation and Sheets State
    private val _selectedTab = MutableStateFlow(0) // 0: Listen Now, 1: Library, 2: Playlists, 3: Equalizer, 4: Settings
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

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
        loadPersistedPlaylists()
        observeFavoritesAndHistory()
        observeWidgetSync()
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
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
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
     * Fully reads metadata and file sizes > 2.5MB!
     */
    fun importFileUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val media = MediaScanner.resolveMediaFromUri(getApplication(), uri)
                val current = _masterMediaList.value.toMutableList()
                current.add(0, media)
                _masterMediaList.value = current
                playMedia(media)
                _isNowPlayingExpanded.value = true
            } catch (_: Exception) {}
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
        playMedia(streamItem)
        _isNowPlayingExpanded.value = true
    }

    fun scanDeviceStorage() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val localItems = MediaScanner.queryDeviceMedia(getApplication())
                if (localItems.isNotEmpty()) {
                    val combined = (localItems + _masterMediaList.value).distinctBy { it.uri }
                    _masterMediaList.value = combined
                }
            } catch (_: Exception) {}
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
    }
}
