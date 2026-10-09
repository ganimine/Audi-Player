package com.example.data

import com.example.model.MediaItemData
import com.example.model.Playlist

object DemoMediaData {

    val SAMPLE_TRACKS = listOf(
        MediaItemData(
            id = "demo_video_1",
            title = "Big Buck Bunny (1080p 60fps)",
            artist = "Blender Open Movie",
            album = "Cinematic Showcase",
            durationMs = 596000L,
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            isVideo = true,
            fileSize = 158 * 1024 * 1024L, // 158 MB > 2.5MB
            bitrate = "4,500 kbps",
            resolution = "1920x1080 @ 60fps",
            codec = "H.264 / AAC High Profile",
            container = "MP4 / ISO Media",
            sampleRate = "48 kHz",
            channels = "5.1 Surround",
            artworkUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800",
            gradientColorIndex = 0
        ),
        MediaItemData(
            id = "demo_audio_1",
            title = "Cosmic Horizon (Lossless FLAC)",
            artist = "Aura Sound Lab",
            album = "Starlight Odyssey",
            durationMs = 245000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            isVideo = false,
            fileSize = 42 * 1024 * 1024L, // 42 MB > 2.5MB
            bitrate = "1,411 kbps (Lossless)",
            resolution = null,
            codec = "FLAC 24-bit / 96kHz",
            container = "FLAC Audio",
            sampleRate = "96 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
            gradientColorIndex = 1
        ),
        MediaItemData(
            id = "demo_video_2",
            title = "Tears of Steel (VFX Remaster)",
            artist = "Ian Hubert & Blender",
            album = "Sci-Fi Cinema Collection",
            durationMs = 734000L,
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            isVideo = true,
            fileSize = 240 * 1024 * 1024L, // 240 MB > 2.5MB
            bitrate = "8,200 kbps (High Tier)",
            resolution = "3840x2160 UHD",
            codec = "H.265 / HEVC Main 10",
            container = "Matroska MKV / MP4",
            sampleRate = "48 kHz",
            channels = "7.1 Atmos Ready",
            artworkUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
            gradientColorIndex = 2
        ),
        MediaItemData(
            id = "demo_audio_2",
            title = "Cyberpunk Overdrive (Hi-Res Master)",
            artist = "Synthetix & Pulse",
            album = "Neon Metropolis",
            durationMs = 312000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            isVideo = false,
            fileSize = 18 * 1024 * 1024L, // 18 MB > 2.5MB
            bitrate = "320 kbps CBR",
            resolution = null,
            codec = "Opus / Vorbis 48kHz",
            container = "OGG Container",
            sampleRate = "48 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800",
            gradientColorIndex = 3
        ),
        MediaItemData(
            id = "demo_audio_3",
            title = "Midnight Lo-Fi Chill",
            artist = "Apple Chillwave Project",
            album = "Cozy Nights in Tokyo",
            durationMs = 210000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            isVideo = false,
            fileSize = 9 * 1024 * 1024L, // 9 MB > 2.5MB
            bitrate = "256 kbps AAC-LC",
            resolution = null,
            codec = "MPEG-4 AAC / ALAC",
            container = "M4A Apple Audio",
            sampleRate = "44.1 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
            gradientColorIndex = 4
        ),
        MediaItemData(
            id = "demo_video_3",
            title = "For Bigger Blazes (HDR Demo)",
            artist = "Google Media Labs",
            album = "Ultra Performance Demo",
            durationMs = 15000L,
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            isVideo = true,
            fileSize = 48 * 1024 * 1024L, // 48 MB > 2.5MB
            bitrate = "6,400 kbps",
            resolution = "1920x1080 HDR",
            codec = "VP9 / Opus WebM",
            container = "WebM / MP4",
            sampleRate = "48 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
            gradientColorIndex = 5
        ),
        MediaItemData(
            id = "demo_audio_4",
            title = "Beethoven: Moonlight Sonata",
            artist = "Vienna Philharmonic Orchestra",
            album = "Classical Masterworks Vol. 1",
            durationMs = 380000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            isVideo = false,
            fileSize = 34 * 1024 * 1024L, // 34 MB > 2.5MB
            bitrate = "1,536 kbps Pure Master",
            resolution = null,
            codec = "FLAC Lossless 24-bit",
            container = "FLAC",
            sampleRate = "96 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1520523839898-50712825e3a7?w=800",
            gradientColorIndex = 0
        )
    )

    val INITIAL_PLAYLISTS = listOf(
        Playlist(
            id = "playlist_favorites",
            name = "Favorites & Lossless",
            description = "High resolution audio masters & top 4K videos",
            mediaIds = listOf("demo_video_1", "demo_audio_1", "demo_video_2", "demo_audio_4"),
            coverGradientIndex = 1
        ),
        Playlist(
            id = "playlist_vlc_theater",
            name = "VLC Cinema & Clips",
            description = "High bitrate full HD & 4K video playback",
            mediaIds = listOf("demo_video_1", "demo_video_2", "demo_video_3"),
            coverGradientIndex = 2
        ),
        Playlist(
            id = "playlist_chill_study",
            name = "Aura Chillwave & Lo-Fi",
            description = "Relaxing spatial audio tracks with Apple Music clarity",
            mediaIds = listOf("demo_audio_2", "demo_audio_3", "demo_audio_4"),
            coverGradientIndex = 3
        )
    )
}
