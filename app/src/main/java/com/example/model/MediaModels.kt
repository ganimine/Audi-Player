package com.example.model

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class ContentTypeFilter(val label: String, val description: String) {
    AUDIO_ONLY("Audio Files", "Music, songs & lossless audio tracks"),
    VIDEO_ONLY("Video Files", "4K/HD videos & music videos"),
    BOTH("Both Audio & Video", "All audio tracks and video files")
}

enum class SizeUnit(val symbol: String, val bytesMultiplier: Long) {
    KB("KB", 1024L),
    MB("MB", 1024L * 1024L),
    GB("GB", 1024L * 1024L * 1024L)
}

enum class SizePreset(val label: String, val subtitle: String) {
    ALL_SIZES("All Sizes", "Include all file sizes"),
    SMALL("Small", "Under 5 MB"),
    TARGET_4_TO_5_MB("4 MB – 5 MB", "Standard 4–5.5 MB tracks"),
    MEDIUM("Medium", "5 MB to 50 MB"),
    LARGE("Large", "Over 50 MB"),
    CUSTOM("Custom", "Specify min & max limits")
}

data class MediaFilterConfig(
    val contentType: ContentTypeFilter = ContentTypeFilter.AUDIO_ONLY,
    val sizePreset: SizePreset = SizePreset.ALL_SIZES,
    val includeAllSizes: Boolean = true,
    val minSizeValue: Double = 0.0,
    val minSizeUnit: SizeUnit = SizeUnit.MB,
    val maxSizeValue: Double = 500.0,
    val maxSizeUnit: SizeUnit = SizeUnit.MB
) {
    val minBytes: Long
        get() = if (includeAllSizes || sizePreset == SizePreset.ALL_SIZES) {
            0L
        } else {
            (minSizeValue.coerceAtLeast(0.0) * minSizeUnit.bytesMultiplier).toLong()
        }

    val maxBytes: Long
        get() = if (includeAllSizes || sizePreset == SizePreset.ALL_SIZES) {
            Long.MAX_VALUE
        } else {
            (maxSizeValue.coerceAtLeast(0.0) * maxSizeUnit.bytesMultiplier).toLong()
        }

    val isValid: Boolean
        get() = if (includeAllSizes || sizePreset == SizePreset.ALL_SIZES) {
            true
        } else {
            minSizeValue >= 0.0 && maxSizeValue > 0.0 && minBytes <= maxBytes
        }

    val validationErrorMessage: String?
        get() = when {
            includeAllSizes || sizePreset == SizePreset.ALL_SIZES -> null
            minSizeValue < 0.0 || maxSizeValue <= 0.0 -> "File size limits must be greater than 0."
            minBytes > maxBytes -> "Minimum file size cannot exceed maximum file size."
            else -> null
        }

    val isCustomOrNonDefault: Boolean
        get() = contentType != ContentTypeFilter.AUDIO_ONLY ||
                (!includeAllSizes && sizePreset != SizePreset.ALL_SIZES)

    val summaryLabel: String
        get() {
            val typePart = when (contentType) {
                ContentTypeFilter.AUDIO_ONLY -> "Audio"
                ContentTypeFilter.VIDEO_ONLY -> "Video"
                ContentTypeFilter.BOTH -> "Audio & Video"
            }
            val sizePart = if (includeAllSizes || sizePreset == SizePreset.ALL_SIZES) {
                "All Sizes"
            } else if (sizePreset != SizePreset.CUSTOM) {
                sizePreset.label
            } else {
                "${formatValue(minSizeValue)} ${minSizeUnit.symbol} – ${formatValue(maxSizeValue)} ${maxSizeUnit.symbol}"
            }
            return "$typePart • $sizePart"
        }

    fun matches(item: MediaItemData): Boolean {
        val matchesType = when (contentType) {
            ContentTypeFilter.AUDIO_ONLY -> !item.isVideo
            ContentTypeFilter.VIDEO_ONLY -> item.isVideo
            ContentTypeFilter.BOTH -> true
        }
        if (!matchesType) return false

        if (includeAllSizes || sizePreset == SizePreset.ALL_SIZES) return true

        // Stream items with 0 fileSize are included unless strict size filter is applied to local files
        if (item.fileSize <= 0L) return true

        return item.fileSize in minBytes..maxBytes
    }

    private fun formatValue(v: Double): String {
        return if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.1f", v)
    }

    companion object {
        fun fromPreset(contentType: ContentTypeFilter, preset: SizePreset): MediaFilterConfig {
            return when (preset) {
                SizePreset.ALL_SIZES -> MediaFilterConfig(
                    contentType = contentType,
                    sizePreset = SizePreset.ALL_SIZES,
                    includeAllSizes = true,
                    minSizeValue = 0.0,
                    minSizeUnit = SizeUnit.MB,
                    maxSizeValue = 500.0,
                    maxSizeUnit = SizeUnit.MB
                )
                SizePreset.SMALL -> MediaFilterConfig(
                    contentType = contentType,
                    sizePreset = SizePreset.SMALL,
                    includeAllSizes = false,
                    minSizeValue = 0.0,
                    minSizeUnit = SizeUnit.KB,
                    maxSizeValue = 5.0,
                    maxSizeUnit = SizeUnit.MB
                )
                SizePreset.TARGET_4_TO_5_MB -> MediaFilterConfig(
                    contentType = contentType,
                    sizePreset = SizePreset.TARGET_4_TO_5_MB,
                    includeAllSizes = false,
                    minSizeValue = 4.0,
                    minSizeUnit = SizeUnit.MB,
                    maxSizeValue = 5.5,
                    maxSizeUnit = SizeUnit.MB
                )
                SizePreset.MEDIUM -> MediaFilterConfig(
                    contentType = contentType,
                    sizePreset = SizePreset.MEDIUM,
                    includeAllSizes = false,
                    minSizeValue = 5.0,
                    minSizeUnit = SizeUnit.MB,
                    maxSizeValue = 50.0,
                    maxSizeUnit = SizeUnit.MB
                )
                SizePreset.LARGE -> MediaFilterConfig(
                    contentType = contentType,
                    sizePreset = SizePreset.LARGE,
                    includeAllSizes = false,
                    minSizeValue = 50.0,
                    minSizeUnit = SizeUnit.MB,
                    maxSizeValue = 10.0,
                    maxSizeUnit = SizeUnit.GB
                )
                SizePreset.CUSTOM -> MediaFilterConfig(
                    contentType = contentType,
                    sizePreset = SizePreset.CUSTOM,
                    includeAllSizes = false,
                    minSizeValue = 1.0,
                    minSizeUnit = SizeUnit.MB,
                    maxSizeValue = 100.0,
                    maxSizeUnit = SizeUnit.MB
                )
            }
        }
    }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit Screen"),
    FILL("Fill / Zoom"),
    FIXED_16_9("16:9"),
    FIXED_4_3("4:3"),
    STRETCH("Stretch")
}

enum class VisualizerStyle(val displayName: String) {
    WAVEFORM("Liquid Wave"),
    SPECTRUM("AUDI Bars"),
    VINYL("Vinyl Disc"),
    MINIMAL_PULSE("Ambient Glow")
}

enum class AccentTheme(val displayName: String, val hexColor: Long) {
    APPLE_RED("Apple Crimson", 0xFFFA233B),
    AUDI_ORANGE("AUDI Amber", 0xFFFF6A00),
    NEON_CYAN("Cyber Cyan", 0xFF00E5FF),
    ELECTRIC_PURPLE("Aura Purple", 0xFFAF52DE),
    EMERALD_GREEN("Neon Mint", 0xFF30D158),
    SUNSET_GOLD("Sunset Gold", 0xFFFFD60A)
}

enum class SkinPreset(
    val id: String,
    val title: String,
    val description: String,
    val backgroundColor: Long,
    val surfaceColor: Long,
    val accentColor: Long,
    val textColor: Long,
    val secondaryTextColor: Long,
    val blurOpacity: Float,
    val cornerRadiusDp: Int,
    val backgroundPhotoUrl: String? = null
) {
    MIDNIGHT_GLASS(
        id = "midnight_glass",
        title = "Midnight Glass",
        description = "Premium frosted glass with deep indigo shadows",
        backgroundColor = 0xFF0A0C16,
        surfaceColor = 0x24FFFFFF,
        accentColor = 0xFFFA233B,
        textColor = 0xFFFFFFFF,
        secondaryTextColor = 0xFF9CA3AF,
        blurOpacity = 0.85f,
        cornerRadiusDp = 20
    ),
    APPLE_MINIMAL(
        id = "apple_minimal",
        title = "Apple-Inspired Minimal",
        description = "Spacious layout, crisp typography and refined curves",
        backgroundColor = 0xFF0F1117,
        surfaceColor = 0x2EFFFFFF,
        accentColor = 0xFFFF2D55,
        textColor = 0xFFFFFFFF,
        secondaryTextColor = 0xFFA1A1AA,
        blurOpacity = 0.90f,
        cornerRadiusDp = 18
    ),
    AURORA(
        id = "aurora",
        title = "Aurora",
        description = "Cosmic Northern lights glow with vibrant cyan & violet",
        backgroundColor = 0xFF060B19,
        surfaceColor = 0x330E2042,
        accentColor = 0xFF00F2FE,
        textColor = 0xFFF0FDF4,
        secondaryTextColor = 0xFF7DD3FC,
        blurOpacity = 0.80f,
        cornerRadiusDp = 22,
        backgroundPhotoUrl = "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?w=1200"
    ),
    AMOLED_BLACK(
        id = "amoled_black",
        title = "AMOLED Black",
        description = "Pure pitch-black pixels for ultimate contrast and battery savings",
        backgroundColor = 0xFF000000,
        surfaceColor = 0xFF121212,
        accentColor = 0xFF30D158,
        textColor = 0xFFFFFFFF,
        secondaryTextColor = 0xFF71717A,
        blurOpacity = 1.0f,
        cornerRadiusDp = 12
    ),
    CINEMA_MODE(
        id = "cinema_mode",
        title = "Cinema Mode",
        description = "High-contrast dark theater aesthetic tuned for 4K video",
        backgroundColor = 0xFF08080C,
        surfaceColor = 0xFF181822,
        accentColor = 0xFFFF9F0A,
        textColor = 0xFFF8FAFC,
        secondaryTextColor = 0xFF94A3B8,
        blurOpacity = 0.95f,
        cornerRadiusDp = 16
    ),
    VINYL_STUDIO(
        id = "vinyl_studio",
        title = "Vinyl Studio",
        description = "Warm analog turntable warmth with bronze accents",
        backgroundColor = 0xFF161311,
        surfaceColor = 0xFF241D19,
        accentColor = 0xFFFFB340,
        textColor = 0xFFFFFBEB,
        secondaryTextColor = 0xFFA8A29E,
        blurOpacity = 0.88f,
        cornerRadiusDp = 20,
        backgroundPhotoUrl = "https://images.unsplash.com/photo-1539375665275-f9de415ef9ac?w=1200"
    ),
    NEON_WAVE(
        id = "neon_wave",
        title = "Neon Wave",
        description = "Futuristic synthwave glowing cybernetic accents",
        backgroundColor = 0xFF0C0719,
        surfaceColor = 0x383B1066,
        accentColor = 0xFFD946EF,
        textColor = 0xFFFDF4FF,
        secondaryTextColor = 0xFFC084FC,
        blurOpacity = 0.80f,
        cornerRadiusDp = 24,
        backgroundPhotoUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=1200"
    ),
    MATERIAL_CLEAN(
        id = "material_clean",
        title = "Material Clean",
        description = "Simple, functional, distraction-free surfaces",
        backgroundColor = 0xFF13151C,
        surfaceColor = 0xFF1E222E,
        accentColor = 0xFF38BDF8,
        textColor = 0xFFF1F5F9,
        secondaryTextColor = 0xFF94A3B8,
        blurOpacity = 0.90f,
        cornerRadiusDp = 16
    ),
    NATURE_CANVAS(
        id = "nature_canvas",
        title = "Nature Canvas",
        description = "Calming organic forest tones and emerald atmosphere",
        backgroundColor = 0xFF0B1410,
        surfaceColor = 0x33142B21,
        accentColor = 0xFF34D399,
        textColor = 0xFFECFDF5,
        secondaryTextColor = 0xFF6EE7B7,
        blurOpacity = 0.85f,
        cornerRadiusDp = 20,
        backgroundPhotoUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1200"
    ),
    CUSTOM_STUDIO(
        id = "custom_studio",
        title = "Custom Studio",
        description = "User-customized skin with configurable blur, colors and wallpaper",
        backgroundColor = 0xFF0D0F18,
        surfaceColor = 0x29FFFFFF,
        accentColor = 0xFFFA233B,
        textColor = 0xFFFFFFFF,
        secondaryTextColor = 0xFF9CA3AF,
        blurOpacity = 0.85f,
        cornerRadiusDp = 18
    )
}

data class CustomSkinConfig(
    val customWallpaperUrl: String? = null,
    val wallpaperOpacity: Float = 0.35f,
    val wallpaperBlurRadius: Float = 16f,
    val accentHex: Long = 0xFFFA233B,
    val cornerRadius: Int = 18,
    val surfaceOpacity: Float = 0.15f
)

data class WidgetSettingsConfig(
    val widgetTheme: String = "Matches Skin", // "Matches Skin", "Apple Frosted Glass", "AMOLED Black", "AUDI Amber"
    val widgetOpacity: Float = 0.90f,
    val showArtwork: Boolean = true,
    val showProgressBar: Boolean = true,
    val showEqualizerBars: Boolean = true,
    val cornerRadiusDp: Int = 18,
    val compactMode: Boolean = false
)

data class MediaItemData(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "AUDI Library",
    val durationMs: Long = 0L,
    val uri: String,
    val isVideo: Boolean = false,
    val fileSize: Long = 0L, // in bytes
    val bitrate: String = "320 kbps",
    val resolution: String? = null, // e.g. "1080p 60fps"
    val codec: String = if (isVideo) "H.264 / AAC" else "FLAC Lossless",
    val container: String = if (isVideo) "MP4" else "FLAC",
    val sampleRate: String = "48 kHz",
    val channels: String = "Stereo 2.0",
    val genre: String = "Media",
    val year: String? = null,
    val artworkUrl: String? = null,
    val gradientColorIndex: Int = 0,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedFileSize: String
        get() {
            if (fileSize <= 0) return "Stream / Direct"
            val mb = fileSize / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }

    val isLargeFile: Boolean
        get() = fileSize >= 2.5 * 1024 * 1024

    val isTargetSize4to5Mb: Boolean
        get() = fileSize in (4L * 1024 * 1024)..(5L * 1024 * 1024 + 512 * 1024)

    fun isCallRecording(): Boolean {
        val keywords = listOf(
            "call", "recording", "call_rec", "recorder", "callrec",
            "phone_call", "voice_rec", "voicerecord", "sound_recorder",
            "callrecording", "audio_record", "call_history", "call@"
        )
        val text = "${title.lowercase()} ${artist.lowercase()} ${album.lowercase()}"
        return keywords.any { text.contains(it) }
    }

    fun toMedia3MediaItem(): androidx.media3.common.MediaItem {
        val autoExtras = android.os.Bundle().apply {
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1) // List layout
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1) // List layout
            putBoolean("android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS", true)
            putBoolean("android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT", true)
        }

        val metadataBuilder = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setArtist(artist)
            .setAlbumArtist(artist)
            .setAlbumTitle(album)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(
                if (isVideo) androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO
                else androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
            )
            .setExtras(autoExtras)

        if (!artworkUrl.isNullOrBlank()) {
            try {
                metadataBuilder.setArtworkUri(android.net.Uri.parse(artworkUrl))
            } catch (_: Exception) {}
        }

        val targetUri = when {
            uri.startsWith("content://") || uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("file://") || uri.startsWith("android.resource://") -> {
                android.net.Uri.parse(uri)
            }
            uri.startsWith("/") -> {
                android.net.Uri.fromFile(java.io.File(uri))
            }
            else -> {
                android.net.Uri.parse(uri)
            }
        }

        return androidx.media3.common.MediaItem.Builder()
            .setMediaId(id)
            .setUri(targetUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
}

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val mediaIds: List<String> = emptyList(),
    val coverGradientIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class EqualizerBand(
    val bandIndex: Int,
    val centerFreqHz: Int,
    val levelDb: Float, // -10.0 to +10.0
    val minDb: Float = -10f,
    val maxDb: Float = 10f
) {
    val freqDisplay: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000}kHz" else "${centerFreqHz}Hz"
}

data class EqualizerPreset(
    val name: String,
    val bandLevels: List<Float> // 5 values for 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
)

val DEFAULT_EQUALIZER_PRESETS = listOf(
    EqualizerPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset("Bass Booster", listOf(6f, 4f, 1f, 0f, -1f)),
    EqualizerPreset("Vocal Booster", listOf(-2f, 1f, 5f, 3f, 1f)),
    EqualizerPreset("Rock & Metal", listOf(4.5f, 2.5f, -1f, 3f, 4.5f)),
    EqualizerPreset("Pop Acoustic", listOf(-1f, 2f, 4f, 2f, -1f)),
    EqualizerPreset("Electronic Dance", listOf(5f, 3f, 0f, 2f, 4f)),
    EqualizerPreset("Jazz Lounge", listOf(3f, 1f, 0f, 2f, 3f)),
    EqualizerPreset("Classical Symphony", listOf(3f, 2f, -1f, 2f, 3f))
)
