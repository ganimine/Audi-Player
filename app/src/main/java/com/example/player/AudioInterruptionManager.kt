package com.example.player

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Intelligent Audio Interruption & Ducking Controller
 *
 * Guarantees:
 * 1. Automatic stop/pause when receiving phone calls from dialer or in-app VoIP calls (WhatsApp, Meet, Zoom, Telegram).
 * 2. Automatic audio ducking (smoothly lowering volume to 20%) when notification sounds/alerts arrive.
 * 3. Automatic volume restoration (smoothly raising volume back to 100%) once the notification alert finishes.
 * 4. Automatic playback resumption when call ends (if autoResume is enabled).
 * 5. Multi-layered detection:
 *    - Layer A: AudioFocus listener (handles AUDIOFOCUS_LOSS_TRANSIENT & AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK).
 *    - Layer B: AudioManager mode listener & active watchdog (MODE_RINGTONE, MODE_IN_CALL, MODE_IN_COMMUNICATION). Zero-permission fallback.
 *    - Layer C: TelephonyManager callback & broadcast receiver for incoming dialer calls.
 */
class AudioInterruptionManager(
    private val context: Context,
    private val onAutoStop: () -> Boolean, // Returns true if it was playing and stopped
    private val onResume: () -> Unit,
    private val onDuck: (Float) -> Unit,
    private val onRaise: () -> Unit,
    private val isPlaying: () -> Boolean
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // State flows for UI and indicators
    private val _isDucked = MutableStateFlow(false)
    val isDucked: StateFlow<Boolean> = _isDucked.asStateFlow()

    private val _interruptionStatus = MutableStateFlow<String?>(null)
    val interruptionStatus: StateFlow<String?> = _interruptionStatus.asStateFlow()

    private val _autoResumeOnCallEnd = MutableStateFlow(true)
    val autoResumeOnCallEnd: StateFlow<Boolean> = _autoResumeOnCallEnd.asStateFlow()

    private val _duckVolumeFactor = MutableStateFlow(0.20f) // 20% volume when ducked
    val duckVolumeFactor: StateFlow<Float> = _duckVolumeFactor.asStateFlow()

    // When true: Player only stops for actual incoming/active calls (telephony/VoIP) or user actions,
    // and lowers volume for notifications, while ignoring disruptive focus loss from transient apps.
    private val _dedicatedCallInterruptionOnly = MutableStateFlow(true)
    val dedicatedCallInterruptionOnly: StateFlow<Boolean> = _dedicatedCallInterruptionOnly.asStateFlow()

    private val _activeCallState = MutableStateFlow("Idle (No Call)")
    val activeCallState: StateFlow<String> = _activeCallState.asStateFlow()

    // Internal tracking
    private var wasPlayingBeforeInterruption = false
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var simulationJob: Job? = null
    private var watchdogJob: Job? = null
    private var modeChangedListener: Any? = null
    private var isReceiverRegistered = false

    // Audio focus listener
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        handleAudioFocusChange(focusChange)
    }

    // Dynamic broadcast receiver for dialer phone state changes & audio becoming noisy
    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    when (stateStr) {
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            handleCallStateChange(TelephonyManager.CALL_STATE_RINGING, isDialer = true)
                        }
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            handleCallStateChange(TelephonyManager.CALL_STATE_OFFHOOK, isDialer = true)
                        }
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            handleCallStateChange(TelephonyManager.CALL_STATE_IDLE, isDialer = true)
                        }
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    _interruptionStatus.value = "Headphones disconnected • Playback paused"
                    onAutoStop()
                }
            }
        }
    }

    init {
        setupTelephonyListener()
        setupAudioModeListener()
        registerPhoneStateBroadcastReceiver()
    }

    /**
     * Request system audio focus for playback.
     */
    fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false) // Required so we receive AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                .setOnAudioFocusChangeListener(focusChangeListener, mainHandler)
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    /**
     * Abandon system audio focus when playback is explicitly paused or stopped by user.
     */
    fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        hasAudioFocus = false
    }

    /**
     * Starts continuous low-overhead call watchdog while media is actively playing.
     * Checks AudioManager audio mode and TelephonyManager state every 200ms.
     * This provides guaranteed, zero-latency auto-stop even if OS event callbacks are delayed.
     */
    fun startCallMonitoring() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                checkCallAndAudioInterruption()
                delay(200L)
            }
        }
    }

    fun stopCallMonitoring() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    fun checkCallAndAudioInterruption() {
        if (_activeCallState.value.contains("Simulation")) return

        val currentMode = audioManager.mode
        val isTelephonyRingingOrActive = try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
                    } else false
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
                }
            } else false
        } catch (_: Exception) {
            false
        }

        val isVoipOrCallMode = (
            currentMode == AudioManager.MODE_IN_CALL ||
            currentMode == AudioManager.MODE_IN_COMMUNICATION ||
            currentMode == AudioManager.MODE_RINGTONE ||
            currentMode == AudioManager.MODE_CALL_SCREENING
        )

        if (isTelephonyRingingOrActive || isVoipOrCallMode) {
            // An incoming call or connected call is detected!
            val label = when {
                currentMode == AudioManager.MODE_RINGTONE -> "Incoming Call Ringing"
                currentMode == AudioManager.MODE_IN_COMMUNICATION -> "In-App VoIP Call Active (WhatsApp/Meet)"
                currentMode == AudioManager.MODE_IN_CALL -> "Phone Call in Progress"
                isTelephonyRingingOrActive -> "Incoming Dialer Call Ringing"
                else -> "Call Active"
            }

            _activeCallState.value = label
            _interruptionStatus.value = "Auto-stopped: $label"

            if (isPlaying()) {
                wasPlayingBeforeInterruption = true
                onAutoStop()
            }
        } else {
            // No call detected by mode or telephony
            if (_activeCallState.value != "Idle (No Call)" && !_activeCallState.value.contains("Simulation")) {
                val wasInCall = wasPlayingBeforeInterruption
                _activeCallState.value = "Idle (No Call)"
                if (wasInCall && _autoResumeOnCallEnd.value) {
                    wasPlayingBeforeInterruption = false
                    _interruptionStatus.value = "Call ended • Playback auto-resumed"
                    onResume()
                    scope.launch {
                        delay(3000L)
                        _interruptionStatus.value = null
                    }
                }
            }
        }
    }

    /**
     * Core handler for Android audio focus transitions.
     */
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Audio focus taken by another external app
                if (_dedicatedCallInterruptionOnly.value) {
                    // Check if there is an actual incoming or active call
                    val mode = audioManager.mode
                    val isActualCall = (
                        mode == AudioManager.MODE_IN_CALL ||
                        mode == AudioManager.MODE_IN_COMMUNICATION ||
                        mode == AudioManager.MODE_RINGTONE ||
                        mode == AudioManager.MODE_CALL_SCREENING ||
                        _activeCallState.value != "Idle (No Call)"
                    )

                    if (isActualCall) {
                        hasAudioFocus = false
                        wasPlayingBeforeInterruption = isPlaying()
                        _interruptionStatus.value = "Auto-stopped: Call active"
                        onAutoStop()
                    } else {
                        // Keep playing! Re-assert focus so random background apps don't prematurely stop AUDI playback.
                        hasAudioFocus = false
                        requestAudioFocus()
                    }
                } else {
                    hasAudioFocus = false
                    wasPlayingBeforeInterruption = false
                    _interruptionStatus.value = "Playback stopped (Audio focus taken by another app)"
                    onAutoStop()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Transient loss without ducking: check if this is an incoming or lifted call
                val mode = audioManager.mode
                val isCallMode = (
                    mode == AudioManager.MODE_IN_CALL ||
                    mode == AudioManager.MODE_IN_COMMUNICATION ||
                    mode == AudioManager.MODE_RINGTONE ||
                    mode == AudioManager.MODE_CALL_SCREENING ||
                    _activeCallState.value != "Idle (No Call)"
                )

                if (isCallMode || !_dedicatedCallInterruptionOnly.value) {
                    hasAudioFocus = false
                    if (isPlaying()) {
                        wasPlayingBeforeInterruption = true
                    }
                    _interruptionStatus.value = "Auto-stopped: Incoming / Active Call"
                    onAutoStop()
                } else {
                    // Non-call transient audio focus loss: treat as ducking instead of stopping!
                    _isDucked.value = true
                    _interruptionStatus.value = "Audio interruption • Sound lowered (${(_duckVolumeFactor.value * 100).toInt()}%)"
                    onDuck(_duckVolumeFactor.value)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // User requirement: "lower the sound if any notifications comes"
                _isDucked.value = true
                _interruptionStatus.value = "Notification alert • Sound lowered (Ducked to ${(_duckVolumeFactor.value * 100).toInt()}%)"
                onDuck(_duckVolumeFactor.value)
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                // Focus regained!
                hasAudioFocus = true

                // User requirement: "then raise volume automatically"
                if (_isDucked.value) {
                    _isDucked.value = false
                    _interruptionStatus.value = "Notification finished • Volume restored"
                    onRaise()
                }

                // If playback was stopped for transient call/prompt, auto-resume
                if (wasPlayingBeforeInterruption && _autoResumeOnCallEnd.value) {
                    wasPlayingBeforeInterruption = false
                    _interruptionStatus.value = "Call ended • Playback auto-resumed"
                    onResume()
                } else {
                    wasPlayingBeforeInterruption = false
                }

                scope.launch {
                    delay(3000L)
                    if (_interruptionStatus.value?.startsWith("Notification finished") == true ||
                        _interruptionStatus.value?.startsWith("Call ended") == true
                    ) {
                        _interruptionStatus.value = null
                    }
                }
            }
        }
    }

    /**
     * Telephony listener to catch dialer calls directly: incoming ringing and active call.
     */
    private fun setupTelephonyListener() {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    telephonyManager.registerTelephonyCallback(
                        context.mainExecutor,
                        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                            override fun onCallStateChanged(state: Int) {
                                handleCallStateChange(state, isDialer = true)
                            }
                        }
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(
                    object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            handleCallStateChange(state, isDialer = true)
                        }
                    },
                    PhoneStateListener.LISTEN_CALL_STATE
                )
            }
        } catch (_: Exception) {}
    }

    /**
     * Listener for in-app VoIP calls (WhatsApp, Telegram, Zoom, Meet, WebRTC).
     * Android sets AudioManager mode to MODE_IN_COMMUNICATION or MODE_RINGTONE during in-app calls.
     */
    private fun setupAudioModeListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val listener = AudioManager.OnModeChangedListener { mode ->
                    handleAudioModeChange(mode)
                }
                modeChangedListener = listener
                audioManager.addOnModeChangedListener(context.mainExecutor, listener)
            } catch (_: Exception) {}
        }
    }

    private fun registerPhoneStateBroadcastReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(phoneStateReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(phoneStateReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (_: Exception) {}
    }

    private fun handleAudioModeChange(mode: Int) {
        when (mode) {
            AudioManager.MODE_IN_COMMUNICATION -> {
                // In-App VoIP Call Active (WhatsApp, Telegram, Meet, Zoom)
                _activeCallState.value = "In-App VoIP Call Active"
                _interruptionStatus.value = "Auto-stopped: In-app VoIP call active (WhatsApp/Meet/Zoom)"
                if (isPlaying()) {
                    wasPlayingBeforeInterruption = true
                }
                onAutoStop()
            }
            AudioManager.MODE_RINGTONE -> {
                // In-App Call Ringing
                _activeCallState.value = "Incoming Call Ringing"
                _interruptionStatus.value = "Auto-stopped: Incoming call ringing"
                if (isPlaying()) {
                    wasPlayingBeforeInterruption = true
                }
                onAutoStop()
            }
            AudioManager.MODE_IN_CALL -> {
                // Telephony Call Active
                _activeCallState.value = "Dialer Phone Call Active"
                _interruptionStatus.value = "Auto-stopped: Phone call in progress"
                if (isPlaying()) {
                    wasPlayingBeforeInterruption = true
                }
                onAutoStop()
            }
            AudioManager.MODE_NORMAL -> {
                // Call ended, returned to normal mode
                val wasInCall = _activeCallState.value != "Idle (No Call)"
                _activeCallState.value = "Idle (No Call)"
                if (wasInCall && wasPlayingBeforeInterruption && _autoResumeOnCallEnd.value) {
                    wasPlayingBeforeInterruption = false
                    _interruptionStatus.value = "In-app call ended • Playback auto-resumed"
                    onResume()
                    scope.launch {
                        delay(3000L)
                        _interruptionStatus.value = null
                    }
                }
            }
        }
    }

    private fun handleCallStateChange(state: Int, isDialer: Boolean) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                _activeCallState.value = if (isDialer) "Dialer Incoming Call Ringing" else "In-App Call Ringing"
                _interruptionStatus.value = "Auto-stopped: Incoming call ringing from ${if (isDialer) "Dialer" else "In-App"}"
                if (isPlaying()) {
                    wasPlayingBeforeInterruption = true
                }
                onAutoStop()
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                _activeCallState.value = if (isDialer) "Dialer Call Active" else "In-App Call Active"
                _interruptionStatus.value = "Auto-stopped: Call active"
                if (isPlaying()) {
                    wasPlayingBeforeInterruption = true
                }
                onAutoStop()
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                val wasActive = _activeCallState.value != "Idle (No Call)"
                _activeCallState.value = "Idle (No Call)"
                if (wasActive) {
                    if (wasPlayingBeforeInterruption && _autoResumeOnCallEnd.value) {
                        wasPlayingBeforeInterruption = false
                        _interruptionStatus.value = "Call ended • Playback auto-resumed"
                        onResume()
                        scope.launch {
                            delay(3000L)
                            _interruptionStatus.value = null
                        }
                    } else {
                        wasPlayingBeforeInterruption = false
                    }
                }
            }
        }
    }

    // Settings modifiers
    fun setDedicatedCallInterruptionOnly(enabled: Boolean) {
        _dedicatedCallInterruptionOnly.value = enabled
    }

    fun setAutoResumeOnCallEnd(enabled: Boolean) {
        _autoResumeOnCallEnd.value = enabled
    }

    fun setDuckVolumeFactor(factor: Float) {
        _duckVolumeFactor.value = factor.coerceIn(0.05f, 0.50f)
    }

    fun clearStatus() {
        _interruptionStatus.value = null
    }

    // ==========================================
    // Interactive Simulation & Verification Tools
    // ==========================================

    /**
     * Simulates an incoming dialer call:
     * 1. Auto-stops the player immediately.
     * 2. Shows incoming dialer call banner.
     * 3. After duration (e.g. 4 seconds), call ends and playback resumes automatically!
     */
    fun simulateDialerCall(callDurationMs: Long = 4000L) {
        simulationJob?.cancel()
        val currentlyPlaying = isPlaying()
        wasPlayingBeforeInterruption = currentlyPlaying

        _activeCallState.value = "Dialer Incoming Call (Simulation)"
        _interruptionStatus.value = "Auto-stopped: Incoming call from Phone Dialer"
        onAutoStop()

        simulationJob = scope.launch {
            delay(1500L)
            _activeCallState.value = "Dialer Call Connected (Simulation)"
            _interruptionStatus.value = "Auto-stopped: Phone call in conversation"

            delay(callDurationMs)
            _activeCallState.value = "Idle (No Call)"

            if (wasPlayingBeforeInterruption && _autoResumeOnCallEnd.value) {
                _interruptionStatus.value = "Dialer call ended • Playback auto-resumed"
                wasPlayingBeforeInterruption = false
                onResume()
            } else {
                _interruptionStatus.value = "Dialer call ended"
                wasPlayingBeforeInterruption = false
            }

            delay(3500L)
            _interruptionStatus.value = null
        }
    }

    /**
     * Simulates an in-app VoIP call (WhatsApp, Telegram, Meet, Zoom):
     * 1. Auto-stops the player immediately.
     * 2. Shows VoIP call banner.
     * 3. Ends call after duration and auto-resumes playback!
     */
    fun simulateInAppVoipCall(callDurationMs: Long = 4000L) {
        simulationJob?.cancel()
        val currentlyPlaying = isPlaying()
        wasPlayingBeforeInterruption = currentlyPlaying

        _activeCallState.value = "In-App VoIP Call Ringing (WhatsApp/Meet)"
        _interruptionStatus.value = "Auto-stopped: In-app VoIP call ringing (WhatsApp/Telegram)"
        onAutoStop()

        simulationJob = scope.launch {
            delay(1500L)
            _activeCallState.value = "In-App VoIP Connected (WhatsApp/Meet)"
            _interruptionStatus.value = "Auto-stopped: In-app VoIP call in conversation"

            delay(callDurationMs)
            _activeCallState.value = "Idle (No Call)"

            if (wasPlayingBeforeInterruption && _autoResumeOnCallEnd.value) {
                _interruptionStatus.value = "In-app call ended • Playback auto-resumed"
                wasPlayingBeforeInterruption = false
                onResume()
            } else {
                _interruptionStatus.value = "In-app call ended"
                wasPlayingBeforeInterruption = false
            }

            delay(3500L)
            _interruptionStatus.value = null
        }
    }

    /**
     * Simulates notification audio ducking:
     * 1. Plays a real system notification sound chime.
     * 2. Automatically lowers media volume down to ducking level (20%).
     * 3. When notification chime finishes (2.5s), automatically raises volume back to 100%!
     */
    fun simulateNotificationDucking(alertDurationMs: Long = 2500L) {
        simulationJob?.cancel()

        // Play real system notification chime
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (_: Exception) {}

        // User requirement: "lower the sound if any notifications comes"
        _isDucked.value = true
        _interruptionStatus.value = "Notification alert • Sound lowered (${(_duckVolumeFactor.value * 100).toInt()}%)"
        onDuck(_duckVolumeFactor.value)

        // User requirement: "then raise volume automatically"
        simulationJob = scope.launch {
            delay(alertDurationMs)
            _isDucked.value = false
            _interruptionStatus.value = "Notification ended • Volume raised back to normal"
            onRaise()

            delay(3000L)
            _interruptionStatus.value = null
        }
    }

    fun release() {
        stopCallMonitoring()
        simulationJob?.cancel()
        abandonAudioFocus()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(phoneStateReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modeChangedListener?.let {
                try {
                    audioManager.removeOnModeChangedListener(it as AudioManager.OnModeChangedListener)
                } catch (_: Exception) {}
            }
        }
    }
}
