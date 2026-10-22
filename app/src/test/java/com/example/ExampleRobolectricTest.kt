package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AUDI Player", appName)
  }

  @Test
  fun `test audio interruption manager call auto stop and notification ducking`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var stopped = false
    var duckedFactor = 1.0f
    var raised = false
    var isPlaying = true

    val manager = com.example.player.AudioInterruptionManager(
      context = context,
      onAutoStop = {
        stopped = true
        isPlaying = false
        true
      },
      onResume = {
        isPlaying = true
      },
      onDuck = { factor ->
        duckedFactor = factor
      },
      onRaise = {
        raised = true
      },
      isPlaying = { isPlaying }
    )

    // Verify initial states
    org.junit.Assert.assertFalse(manager.isDucked.value)
    org.junit.Assert.assertEquals("Idle (No Call)", manager.activeCallState.value)
    org.junit.Assert.assertTrue(manager.autoResumeOnCallEnd.value)

    // Test dialer call auto stop simulation
    manager.simulateDialerCall()
    org.junit.Assert.assertTrue("Playback should be stopped on incoming call", stopped)
    org.junit.Assert.assertTrue(manager.activeCallState.value.contains("Dialer Incoming Call"))

    // Test notification ducking simulation
    manager.simulateNotificationDucking()
    org.junit.Assert.assertTrue("isDucked should be true during notification", manager.isDucked.value)
    org.junit.Assert.assertEquals(0.20f, duckedFactor, 0.01f)
    org.junit.Assert.assertTrue(manager.interruptionStatus.value?.contains("Notification alert") == true)
  }
}
