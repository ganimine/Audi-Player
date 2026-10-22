package com.example.headset

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallStatus(val label: String) {
    IDLE("Idle • Music Active"),
    RINGING("Incoming Call (1-Tap Answer • 2-Tap Reject)"),
    ACTIVE_CALL("In Call (2-Tap Disconnect)")
}

sealed class HeadsetAction {
    object SingleTapPlayPause : HeadsetAction()
    object DoubleTapNext : HeadsetAction()
    object TripleTapPrevious : HeadsetAction()
    object AnswerCall : HeadsetAction()
    object DisconnectCall : HeadsetAction()
}

class HeadsetController(
    private val context: Context,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onIncomingCall: (() -> Unit)? = null
) {
    private val handler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private val multiTapTimeoutMs = 380L

    private val _callStatus = MutableStateFlow(CallStatus.IDLE)
    val callStatus: StateFlow<CallStatus> = _callStatus.asStateFlow()

    private val _lastDetectedGesture = MutableStateFlow("Ready for Headset input")
    val lastDetectedGesture: StateFlow<String> = _lastDetectedGesture.asStateFlow()

    private val _isHeadsetPlugged = MutableStateFlow(false)
    val isHeadsetPlugged: StateFlow<Boolean> = _isHeadsetPlugged.asStateFlow()

    private var wasPlayingBeforeCall = false

    // Headphone Unplugged (Becoming Noisy) Receiver
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                _lastDetectedGesture.value = "Headphones Disconnected • Paused"
                onPlayPause()
            } else if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", 0)
                _isHeadsetPlugged.value = state == 1
                _lastDetectedGesture.value = if (state == 1) "Headset Connected" else "Headset Unplugged"
            }
        }
    }

    private val tapEvaluator = Runnable {
        when (tapCount) {
            1 -> {
                // 1 Tap
                if (_callStatus.value == CallStatus.RINGING) {
                    _lastDetectedGesture.value = "1-Tap: Answered Call"
                    answerCall()
                } else {
                    _lastDetectedGesture.value = "1-Tap: Play / Pause"
                    onPlayPause()
                }
            }
            2 -> {
                // 2 Taps
                if (_callStatus.value == CallStatus.RINGING || _callStatus.value == CallStatus.ACTIVE_CALL) {
                    _lastDetectedGesture.value = "2-Tap: Disconnected / Rejected Call"
                    disconnectCall()
                } else {
                    _lastDetectedGesture.value = "2-Tap: Next Track"
                    onNext()
                }
            }
            3 -> {
                // 3 Taps
                _lastDetectedGesture.value = "3-Tap: Previous Track"
                onPrevious()
            }
            else -> {
                if (tapCount > 3) {
                    _lastDetectedGesture.value = "$tapCount-Tap: Previous Track"
                    onPrevious()
                }
            }
        }
        tapCount = 0
    }

    init {
        registerAudioReceivers()
        setupTelephonyListener()
    }

    /**
     * Handle physical headset hook button press or in-app tap simulation
     */
    fun onHeadsetButtonEvent() {
        tapCount++
        handler.removeCallbacks(tapEvaluator)
        handler.postDelayed(tapEvaluator, multiTapTimeoutMs)
    }

    /**
     * Handle key event from Activity or MediaSession
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    onHeadsetButtonEvent()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    onHeadsetButtonEvent()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    _lastDetectedGesture.value = "Headset Next Key"
                    onNext()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    _lastDetectedGesture.value = "Headset Prev Key"
                    onPrevious()
                    return true
                }
            }
        }
        return false
    }

    /**
     * Call answering logic
     */
    fun answerCall() {
        _callStatus.value = CallStatus.ACTIVE_CALL
        _lastDetectedGesture.value = "Call in Progress • 2-Tap to Disconnect"
    }

    /**
     * Call disconnect logic
     */
    fun disconnectCall() {
        _callStatus.value = CallStatus.IDLE
        _lastDetectedGesture.value = "Call Ended • Resuming Music"
        if (wasPlayingBeforeCall) {
            onPlayPause()
            wasPlayingBeforeCall = false
        }
    }

    /**
     * Simulate an incoming call for testing headset 1-tap/2-tap controls
     */
    fun simulateIncomingCall(isCurrentlyPlaying: Boolean) {
        wasPlayingBeforeCall = isCurrentlyPlaying
        if (isCurrentlyPlaying) {
            onPlayPause() // Auto-pause when call rings
        }
        _callStatus.value = CallStatus.RINGING
        _lastDetectedGesture.value = "SIMULATION: Incoming Call Ringing..."
    }

    private fun registerAudioReceivers() {
        try {
            val filter = IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(Intent.ACTION_HEADSET_PLUG)
            }
            context.registerReceiver(noisyReceiver, filter)
        } catch (_: Exception) {}
    }

    private fun setupTelephonyListener() {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    telephonyManager.registerTelephonyCallback(
                        context.mainExecutor,
                        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                            override fun onCallStateChanged(state: Int) {
                                handleTelephonyState(state)
                            }
                        }
                    )
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.listen(
                        object : PhoneStateListener() {
                            @Deprecated("Deprecated in Java")
                            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                                handleTelephonyState(state)
                            }
                        },
                        PhoneStateListener.LISTEN_CALL_STATE
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleTelephonyState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                _callStatus.value = CallStatus.RINGING
                _lastDetectedGesture.value = "Phone Ringing • 1-Tap Answer, 2-Tap Reject"
                onIncomingCall?.invoke()
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                _callStatus.value = CallStatus.ACTIVE_CALL
                _lastDetectedGesture.value = "Call Connected • 2-Tap Hang Up"
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (_callStatus.value != CallStatus.IDLE) {
                    disconnectCall()
                }
            }
        }
    }

    fun release() {
        handler.removeCallbacks(tapEvaluator)
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
    }
}
