package com.example.data

import com.example.model.MediaItemData
import com.example.model.Playlist

object DemoMediaData {

    val SAMPLE_TRACKS = listOf(
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
        ),
        MediaItemData(
            id = "demo_audio_5",
            title = "Golden Hour Acoustic (4.5 MB)",
            artist = "Aura Acoustic Ensemble",
            album = "Sunset Serenade Sessions",
            durationMs = 188000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            isVideo = false,
            fileSize = 4_718_592L, // 4.5 MB (4 to 5 MB range)
            bitrate = "320 kbps VBR",
            resolution = null,
            codec = "MPEG Audio Layer 3 (MP3)",
            container = "MP3",
            sampleRate = "44.1 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=800",
            gradientColorIndex = 1
        ),
        MediaItemData(
            id = "demo_audio_6",
            title = "Prism Waves Studio Master (4.2 MB)",
            artist = "Neon Skyline",
            album = "Analog Horizons",
            durationMs = 175000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            isVideo = false,
            fileSize = 4_404_019L, // 4.2 MB (4 to 5 MB range)
            bitrate = "256 kbps AAC",
            resolution = null,
            codec = "AAC / Apple M4A",
            container = "M4A",
            sampleRate = "48 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800",
            gradientColorIndex = 3
        ),
        MediaItemData(
            id = "demo_video_1",
            title = "Big Buck Bunny (720p HD)",
            artist = "Blender Open Movie",
            album = "Cinematic Showcase",
            durationMs = 596000L,
            uri = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
            isVideo = true,
            fileSize = 4_999_379L, // ~4.99 MB (in 4MB-5MB range)
            bitrate = "4,500 kbps",
            resolution = "1280x720 HD",
            codec = "H.264 / AAC High Profile",
            container = "MP4 / ISO Media",
            sampleRate = "48 kHz",
            channels = "5.1 Surround",
            artworkUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800",
            gradientColorIndex = 0
        ),
        MediaItemData(
            id = "demo_video_2",
            title = "Jellyfish Deep Ocean (720p HD)",
            artist = "Aura Cinematic Labs",
            album = "Sci-Fi Cinema Collection",
            durationMs = 734000L,
            uri = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_5MB.mp4",
            isVideo = true,
            fileSize = 5_241_877L,
            bitrate = "8,200 kbps (High Tier)",
            resolution = "1280x720 HD",
            codec = "H.264 / AAC High Profile",
            container = "MP4 / ISO Media",
            sampleRate = "48 kHz",
            channels = "7.1 Atmos Ready",
            artworkUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
            gradientColorIndex = 2
        ),
        MediaItemData(
            id = "demo_video_3",
            title = "Sintel Open Fantasy (720p HD)",
            artist = "Blender Animation Studio",
            album = "Ultra Performance Demo",
            durationMs = 52000L,
            uri = "https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_5MB.mp4",
            isVideo = true,
            fileSize = 5_242_780L,
            bitrate = "6,400 kbps",
            resolution = "1280x720 HD",
            codec = "H.264 / AAC WebM/MP4",
            container = "MP4 Container",
            sampleRate = "48 kHz",
            channels = "Stereo 2.0",
            artworkUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
            gradientColorIndex = 5
        )
    )

    val INITIAL_PLAYLISTS = listOf(
        Playlist(
            id = "playlist_favorites",
            name = "Favorites & Lossless",
            description = "High resolution audio masters & spatial audio tracks",
            mediaIds = listOf("demo_audio_1", "demo_audio_2", "demo_audio_4", "demo_audio_6"),
            coverGradientIndex = 1
        ),
        Playlist(
            id = "playlist_audi_theater",
            name = "AUDI Acoustic & Lossless",
            description = "Studio acoustic recordings and orchestral masterpieces",
            mediaIds = listOf("demo_audio_1", "demo_audio_4", "demo_audio_5"),
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
