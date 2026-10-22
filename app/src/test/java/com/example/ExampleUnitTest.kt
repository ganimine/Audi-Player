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
}

