package com.example

import com.example.data.MediaScanner
import com.example.model.MediaItemData
import com.example.ui.components.GOOGLE_PALETTE
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTarget4to5MbFilter() {
        val threeMb = 3L * 1024 * 1024
        val fourPointFiveMb = (4.5 * 1024 * 1024).toLong()
        val sixMb = 6L * 1024 * 1024

        assertFalse("3MB should be outside 4-5MB", MediaScanner.isTarget4to5Mb(threeMb))
        assertTrue("4.5MB should be inside 4-5MB", MediaScanner.isTarget4to5Mb(fourPointFiveMb))
        assertFalse("6MB should be outside 4-5MB", MediaScanner.isTarget4to5Mb(sixMb))
    }

    @Test
    fun testCallRecordingExclusion() {
        assertTrue(
            "Call recording file should be detected",
            MediaScanner.isCallRecording("Call@John_Doe_20261009.mp3")
        )
        assertTrue(
            "Voice recorder file should be detected",
            MediaScanner.isCallRecording("Sound_Recorder_001.m4a")
        )
        assertFalse(
            "Normal song should not be treated as call recording",
            MediaScanner.isCallRecording("Cosmic Horizon FLAC")
        )
    }

    @Test
    fun testGooglePalette() {
        assertEquals("Google palette should have 4 brand colors", 4, GOOGLE_PALETTE.size)
    }

    @Test
    fun testMediaFilterConfigValidationAndUnits() {
        val validConfig = com.example.model.MediaFilterConfig(
            contentType = com.example.model.ContentTypeFilter.BOTH,
            sizePreset = com.example.model.SizePreset.CUSTOM,
            includeAllSizes = false,
            minSizeValue = 500.0,
            minSizeUnit = com.example.model.SizeUnit.KB,
            maxSizeValue = 10.0,
            maxSizeUnit = com.example.model.SizeUnit.MB
        )
        assertTrue("500 KB to 10 MB should be valid", validConfig.isValid)
        assertNull(validConfig.validationErrorMessage)

        val invalidConfig = com.example.model.MediaFilterConfig(
            contentType = com.example.model.ContentTypeFilter.AUDIO_ONLY,
            sizePreset = com.example.model.SizePreset.CUSTOM,
            includeAllSizes = false,
            minSizeValue = 2.0,
            minSizeUnit = com.example.model.SizeUnit.GB,
            maxSizeValue = 50.0,
            maxSizeUnit = com.example.model.SizeUnit.MB
        )
        assertFalse("2 GB min > 50 MB max should be invalid", invalidConfig.isValid)
        assertNotNull(invalidConfig.validationErrorMessage)
    }

    @Test
    fun testMediaFilterConfigContentTypeAndSizeMatching() {
        val audio4Mb = MediaItemData(
            id = "a1",
            title = "Song 4.5MB",
            uri = "content://audio/1",
            isVideo = false,
            fileSize = (4.5 * 1024 * 1024).toLong()
        )
        val video120Mb = MediaItemData(
            id = "v1",
            title = "Video 120MB",
            uri = "content://video/1",
            isVideo = true,
            fileSize = 120L * 1024 * 1024
        )

        val audioOnlySmall = com.example.model.MediaFilterConfig.fromPreset(
            com.example.model.ContentTypeFilter.AUDIO_ONLY,
            com.example.model.SizePreset.SMALL
        )
        assertTrue(audioOnlySmall.matches(audio4Mb))
        assertFalse(audioOnlySmall.matches(video120Mb))

        val videoOnlyLarge = com.example.model.MediaFilterConfig.fromPreset(
            com.example.model.ContentTypeFilter.VIDEO_ONLY,
            com.example.model.SizePreset.LARGE
        )
        assertFalse(videoOnlyLarge.matches(audio4Mb))
        assertTrue(videoOnlyLarge.matches(video120Mb))

        val bothAllSizes = com.example.model.MediaFilterConfig.fromPreset(
            com.example.model.ContentTypeFilter.BOTH,
            com.example.model.SizePreset.ALL_SIZES
        )
        assertTrue(bothAllSizes.matches(audio4Mb))
        assertTrue(bothAllSizes.matches(video120Mb))
    }
}

