package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ContentTypeFilter
import com.example.model.MediaFilterConfig
import com.example.model.SizePreset
import com.example.model.SizeUnit

/**
 * Persisted application settings manager using SharedPreferences.
 * Controls startup screen behavior, video file visibility, default navigation, search history, and content/file-size filters.
 */
class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var showHomeScreenOnStartup: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HOME_ON_STARTUP, false) // Default: false (open directly to Library)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_HOME_ON_STARTUP, value).apply()

    var defaultStartupScreen: Int
        get() = prefs.getInt(KEY_DEFAULT_STARTUP_SCREEN, 1) // 1: Library, 0: Home, 2: Playlists, 3: Equalizer
        set(value) = prefs.edit().putInt(KEY_DEFAULT_STARTUP_SCREEN, value).apply()

    var rememberLastScreen: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_LAST_SCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_LAST_SCREEN, value).apply()

    var lastSelectedTab: Int
        get() = prefs.getInt(KEY_LAST_SELECTED_TAB, 1) // Default: 1 (Library)
        set(value) = prefs.edit().putInt(KEY_LAST_SELECTED_TAB, value).apply()

    var showVideoFiles: Boolean
        get() = prefs.getBoolean(KEY_SHOW_VIDEO_FILES, false) // Default: false (videos hidden by default)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_VIDEO_FILES, value).apply()

    var autoPlayVideos: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PLAY_VIDEOS, false) // Default: false (require explicit tap)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PLAY_VIDEOS, value).apply()

    var resumePreviousPlayback: Boolean
        get() = prefs.getBoolean(KEY_RESUME_PREVIOUS_PLAYBACK, true) // Default: true
        set(value) = prefs.edit().putBoolean(KEY_RESUME_PREVIOUS_PLAYBACK, value).apply()

    var searchHistoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_SEARCH_HISTORY_ENABLED, true) // Default: true
        set(value) = prefs.edit().putBoolean(KEY_SEARCH_HISTORY_ENABLED, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, true) // Default: true (no popups)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    fun getMediaFilterConfig(): MediaFilterConfig {
        val contentTypeName = prefs.getString(
            KEY_FILTER_CONTENT_TYPE,
            if (showVideoFiles) ContentTypeFilter.BOTH.name else ContentTypeFilter.AUDIO_ONLY.name
        ) ?: ContentTypeFilter.AUDIO_ONLY.name
        val presetName = prefs.getString(KEY_FILTER_SIZE_PRESET, SizePreset.ALL_SIZES.name) ?: SizePreset.ALL_SIZES.name
        val includeAll = prefs.getBoolean(KEY_FILTER_INCLUDE_ALL_SIZES, true)
        val minVal = prefs.getFloat(KEY_FILTER_MIN_SIZE_VAL, 0f).toDouble()
        val maxVal = prefs.getFloat(KEY_FILTER_MAX_SIZE_VAL, 500f).toDouble()
        val minUnitName = prefs.getString(KEY_FILTER_MIN_SIZE_UNIT, SizeUnit.MB.name) ?: SizeUnit.MB.name
        val maxUnitName = prefs.getString(KEY_FILTER_MAX_SIZE_UNIT, SizeUnit.MB.name) ?: SizeUnit.MB.name

        val contentType = runCatching { ContentTypeFilter.valueOf(contentTypeName) }.getOrDefault(ContentTypeFilter.AUDIO_ONLY)
        val preset = runCatching { SizePreset.valueOf(presetName) }.getOrDefault(SizePreset.ALL_SIZES)
        val minUnit = runCatching { SizeUnit.valueOf(minUnitName) }.getOrDefault(SizeUnit.MB)
        val maxUnit = runCatching { SizeUnit.valueOf(maxUnitName) }.getOrDefault(SizeUnit.MB)

        return MediaFilterConfig(
            contentType = contentType,
            sizePreset = preset,
            includeAllSizes = includeAll,
            minSizeValue = minVal,
            minSizeUnit = minUnit,
            maxSizeValue = maxVal,
            maxSizeUnit = maxUnit
        )
    }

    fun saveMediaFilterConfig(config: MediaFilterConfig) {
        prefs.edit()
            .putString(KEY_FILTER_CONTENT_TYPE, config.contentType.name)
            .putString(KEY_FILTER_SIZE_PRESET, config.sizePreset.name)
            .putBoolean(KEY_FILTER_INCLUDE_ALL_SIZES, config.includeAllSizes)
            .putFloat(KEY_FILTER_MIN_SIZE_VAL, config.minSizeValue.toFloat())
            .putFloat(KEY_FILTER_MAX_SIZE_VAL, config.maxSizeValue.toFloat())
            .putString(KEY_FILTER_MIN_SIZE_UNIT, config.minSizeUnit.name)
            .putString(KEY_FILTER_MAX_SIZE_UNIT, config.maxSizeUnit.name)
            .apply()
    }

    fun getSearchHistory(): List<String> {
        val raw = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    fun addSearchQuery(query: String) {
        if (!searchHistoryEnabled) return
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getSearchHistory().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val trimmedList = current.take(15) // Keep last 15 searches
        prefs.edit().putString(KEY_SEARCH_HISTORY, trimmedList.joinToString("|||")).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = getSearchHistory().toMutableList()
        current.remove(query)
        prefs.edit().putString(KEY_SEARCH_HISTORY, current.joinToString("|||")).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    companion object {
        private const val PREFS_NAME = "audi_player_preferences"
        private const val KEY_SHOW_HOME_ON_STARTUP = "show_home_on_startup"
        private const val KEY_DEFAULT_STARTUP_SCREEN = "default_startup_screen"
        private const val KEY_REMEMBER_LAST_SCREEN = "remember_last_screen"
        private const val KEY_LAST_SELECTED_TAB = "last_selected_tab"
        private const val KEY_SHOW_VIDEO_FILES = "show_video_files"
        private const val KEY_AUTO_PLAY_VIDEOS = "auto_play_videos"
        private const val KEY_RESUME_PREVIOUS_PLAYBACK = "resume_previous_playback"
        private const val KEY_SEARCH_HISTORY_ENABLED = "search_history_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SEARCH_HISTORY = "recent_search_history"
        private const val KEY_FILTER_CONTENT_TYPE = "filter_content_type"
        private const val KEY_FILTER_SIZE_PRESET = "filter_size_preset"
        private const val KEY_FILTER_INCLUDE_ALL_SIZES = "filter_include_all_sizes"
        private const val KEY_FILTER_MIN_SIZE_VAL = "filter_min_size_val"
        private const val KEY_FILTER_MAX_SIZE_VAL = "filter_max_size_val"
        private const val KEY_FILTER_MIN_SIZE_UNIT = "filter_min_size_unit"
        private const val KEY_FILTER_MAX_SIZE_UNIT = "filter_max_size_unit"
    }
}
