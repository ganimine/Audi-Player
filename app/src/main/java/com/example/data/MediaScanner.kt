package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.MediaItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaScanner {

    private val CALL_RECORDING_KEYWORDS = listOf(
        "call", "recording", "call_rec", "recorder", "callrec",
        "phone_call", "voice_rec", "voicerecord", "sound_recorder",
        "callrecordings", "call_recording", "audio_record", "call_history",
        "call@", "sound recorder", "voice memo"
    )

    fun isCallRecording(displayName: String, path: String? = null, album: String? = null): Boolean {
        val text = "${displayName.lowercase()} ${path?.lowercase().orEmpty()} ${album?.lowercase().orEmpty()}"
        return CALL_RECORDING_KEYWORDS.any { text.contains(it) }
    }

    fun isTarget4to5Mb(sizeBytes: Long): Boolean {
        val minBytes = 4L * 1024 * 1024       // 4.0 MB
        val maxBytes = 5L * 1024 * 1024 + (512 * 1024) // up to ~5.5 MB
        return sizeBytes in minBytes..maxBytes
    }

    /**
     * Resolves rich metadata, album name, embedded album art, codecs, and container info
     * for any selected Uri from Storage Access Framework or external file pickers.
     * Enforces the requirement: only imports 4MB-5MB files, and strictly excludes call recordings.
     */
    suspend fun resolveMediaFromUri(
        context: Context,
        uri: Uri,
        strict4to5Mb: Boolean = true,
        excludeCallRecordings: Boolean = true
    ): MediaItemData = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName = "Imported Media"
        var sizeBytes = 0L

        // Read display name and size from content resolver
        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}

        // Fallback file size if file uri
        if (sizeBytes <= 0 && uri.scheme == "file") {
            uri.path?.let { path ->
                val f = File(path)
                if (f.exists()) sizeBytes = f.length()
            }
        }

        val mimeType = resolver.getType(uri) ?: getMimeTypeFromExtension(displayName)
        val isVideo = mimeType.startsWith("video/") || displayName.endsWith(".mp4", true) ||
                displayName.endsWith(".mkv", true) || displayName.endsWith(".webm", true) ||
                displayName.endsWith(".ts", true) || displayName.endsWith(".avi", true)

        var durationMs = 0L
        var artist = if (isVideo) "Video File" else "Unknown Artist"
        var album = if (isVideo) "Video Collection" else "Single Track"
        var genre = if (isVideo) "Video" else "Audio"
        var year: String? = null
        var resolution: String? = null
        var bitrate = "320 kbps"
        var codec = if (isVideo) "H.264 / AVC" else "AAC / MP3"
        var sampleRate = "48 kHz"
        var artworkPath: String? = null

        // Extract ID3 tags, album name, embedded album picture
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durStr != null) durationMs = durStr.toLongOrNull() ?: 0L

            val artStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            if (!artStr.isNullOrBlank()) artist = artStr

            val albStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            if (!albStr.isNullOrBlank()) album = albStr

            val genStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            if (!genStr.isNullOrBlank()) genre = genStr

            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            if (!yearStr.isNullOrBlank()) year = yearStr.take(4)

            val titleStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!titleStr.isNullOrBlank()) displayName = titleStr

            val bitStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (bitStr != null) {
                val b = bitStr.toLongOrNull() ?: 0L
                if (b > 0) bitrate = "${b / 1000} kbps"
            }

            if (isVideo) {
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                if (width != null && height != null) {
                    resolution = "${width}x${height}"
                }
                val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                if (mime != null) codec = mime

                // Extract video frame thumbnail
                try {
                    val frameBitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frameBitmap != null) {
                        artworkPath = saveBitmapToCache(context, frameBitmap, "vthumb_${displayName.hashCode()}")
                    }
                } catch (_: Exception) {}
            } else {
                // Extract embedded album art bytes for audio files
                try {
                    val picBytes = retriever.embeddedPicture
                    if (picBytes != null && picBytes.isNotEmpty()) {
                        artworkPath = saveBytesToCache(context, picBytes, "art_${displayName.hashCode()}")
                    }
                } catch (_: Exception) {}
            }

            val sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            if (sampleRateStr != null) sampleRate = "${sampleRateStr.toIntOrNull()?.div(1000) ?: 48} kHz"

            retriever.release()
        } catch (_: Exception) {}

        // Enforce: Exclude Call Recordings
        if (excludeCallRecordings && (isCallRecording(displayName, uri.path, album) || isCallRecording(artist, null, genre))) {
            throw IllegalArgumentException("Call recordings are strictly excluded from AUDI Player.")
        }

        // Enforce: Only import 4MB to 5MB sized files
        if (strict4to5Mb && sizeBytes > 0 && !isTarget4to5Mb(sizeBytes)) {
            val mb = sizeBytes / (1024.0 * 1024.0)
            throw IllegalArgumentException("Only 4MB to 5MB sized files can be imported (Selected: ${String.format("%.1f", mb)} MB).")
        }

        val container = displayName.substringAfterLast('.', if (isVideo) "MP4" else "AUDIO").uppercase()

        MediaItemData(
            id = "custom_${UUID.randomUUID()}",
            title = displayName.substringBeforeLast('.'),
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = uri.toString(),
            isVideo = isVideo,
            fileSize = sizeBytes,
            bitrate = bitrate,
            resolution = resolution,
            codec = codec,
            container = container,
            sampleRate = sampleRate,
            channels = "Stereo 2.0",
            genre = genre,
            year = year,
            artworkUrl = artworkPath,
            gradientColorIndex = ((displayName.hashCode() % 6) + 6) % 6
        )
    }

    /**
     * Query device MediaStore for local music and video library files,
     * resolving album name, artist, year, and album artwork Uris.
     * Enforces the 4MB-5MB file filter and excludes call recordings.
     */
    suspend fun queryDeviceMedia(
        context: Context,
        strict4to5Mb: Boolean = true,
        excludeCallRecordings: Boolean = true,
        includeVideos: Boolean = false
    ): List<MediaItemData> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemData>()
        val contentResolver = context.contentResolver

        // 1. Audio query
        val audioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val audioProjection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR
        )

        try {
            contentResolver.query(audioUri, audioProjection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Track $id"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Local Music"
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val year = if (yearCol != -1) cursor.getString(yearCol) else null
                    val contentUri = Uri.withAppendedPath(audioUri, id.toString())

                    // Exclude call recordings strictly
                    if (excludeCallRecordings && (isCallRecording(title, null, album) || isCallRecording(artist, null, null))) {
                        continue
                    }

                    // Enforce 4MB-5MB size filter if enabled
                    if (strict4to5Mb && size > 0 && !isTarget4to5Mb(size)) {
                        continue
                    }

                    // Construct official MediaStore Album Art Uri
                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                    } else null

                    list.add(
                        MediaItemData(
                            id = "local_audio_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            uri = contentUri.toString(),
                            isVideo = false,
                            fileSize = size,
                            bitrate = if (size > 15 * 1024 * 1024) "1,411 kbps (Lossless)" else "320 kbps",
                            codec = if (size > 20 * 1024 * 1024) "FLAC Lossless" else "MP3/AAC",
                            container = "AUDIO",
                            genre = "Music",
                            year = year,
                            artworkUrl = albumArtUri,
                            gradientColorIndex = ((id % 6).toInt() + 6) % 6
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Video query (User-controlled: only scanned when includeVideos is enabled)
        if (includeVideos) {
            val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.RESOLUTION
            )

            try {
                contentResolver.query(videoUri, videoProjection, null, null, "${MediaStore.Video.Media.TITLE} ASC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val resCol = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Video $id"
                        val duration = cursor.getLong(durCol)
                        val size = cursor.getLong(sizeCol)
                        val res = if (resCol != -1) cursor.getString(resCol) else "1080p"
                        val contentUri = Uri.withAppendedPath(videoUri, id.toString())

                        // Exclude call recordings/voice notes
                        if (excludeCallRecordings && isCallRecording(title, null, null)) {
                            continue
                        }

                        // Enforce 4MB-5MB size filter if enabled
                        if (strict4to5Mb && size > 0 && !isTarget4to5Mb(size)) {
                            continue
                        }

                        list.add(
                            MediaItemData(
                                id = "local_video_$id",
                                title = title,
                                artist = "Local Video",
                                album = "Device Storage",
                                durationMs = duration,
                                uri = contentUri.toString(),
                                isVideo = true,
                                fileSize = size,
                                bitrate = "4,200 kbps",
                                resolution = res ?: "1080p",
                                codec = "H.264 / AVC",
                                container = "MP4",
                                genre = "Video",
                                artworkUrl = null,
                                gradientColorIndex = ((id % 6).toInt() + 6) % 6
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        list
    }

    private fun saveBytesToCache(context: Context, bytes: ByteArray, prefix: String): String? {
        return try {
            val cacheDir = File(context.cacheDir, "album_art")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "${prefix}.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String): String? {
        return try {
            val cacheDir = File(context.cacheDir, "album_art")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "${prefix}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "ts" -> "video/mp2t"
            "flac" -> "audio/flac"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg", "opus" -> "audio/ogg"
            "m4a", "aac" -> "audio/aac"
            else -> "*/*"
        }
    }
}
