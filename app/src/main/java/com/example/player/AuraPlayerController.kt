package com.example.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.AspectRatioMode
import com.example.model.DEFAULT_EQUALIZER_PRESETS
import com.example.model.EqualizerBand
import com.example.model.EqualizerPreset
import com.example.model.MediaItemData
import com.example.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(UnstableApi::class)
class AuraPlayerController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val renderersFactory = DefaultRenderersFactory(context).apply {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory).build()

    // Player State Flows
    private val _currentMedia = MutableStateFlow<MediaItemData?>(null)
    val currentMedia: StateFlow<MediaItemData?> = _currentMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _isAudioOnlyForVideo = MutableStateFlow(false)
    val isAudioOnlyForVideo: StateFlow<Boolean> = _isAudioOnlyForVideo.asStateFlow()

    // A-B Loop
    private val _aLoopMs = MutableStateFlow<Long?>(null)
    val aLoopMs: StateFlow<Long?> = _aLoopMs.asStateFlow()

    private val _bLoopMs = MutableStateFlow<Long?>(null)
    val bLoopMs: StateFlow<Long?> = _bLoopMs.asStateFlow()

    private val _isAbLoopActive = MutableStateFlow(false)
    val isAbLoopActive: StateFlow<Boolean> = _isAbLoopActive.asStateFlow()

    // Sleep Timer
    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Audio Equalizer
    private var androidEqualizer: Equalizer? = null
    private var androidBassBoost: BassBoost? = null
    private var androidVirtualizer: Virtualizer? = null

    private val _equalizerBands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val equalizerBands: StateFlow<List<EqualizerBand>> = _equalizerBands.asStateFlow()

    private val _currentPreset = MutableStateFlow<String>("Flat")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0) // 0 - 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0) // 0 - 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    // Visualizer simulation (dancing wave bars)
    private val _visualizerAmplitudes = MutableStateFlow(List(32) { 0.2f })
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    // Queue
    private val _queue = MutableStateFlow<List<MediaItemData>>(emptyList())
    val queue: StateFlow<List<MediaItemData>> = _queue.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var visualizerJob: Job? = null

    init {
        setupPlayerListener()
        startTrackingProgress()
        startVisualizerEngine()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    initAudioEffects()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                        initAudioEffects()
                    }
                    Player.STATE_ENDED -> {
                        handlePlaybackEnded()
                    }
                    else -> {}
                }
            }
        })
    }

    private fun initAudioEffects() {
        val audioSessionId = exoPlayer.audioSessionId
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && androidEqualizer == null) {
            try {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = true
                androidEqualizer = eq

                val numBands = eq.numberOfBands.toInt()
                val bands = mutableListOf<EqualizerBand>()
                val (minLevel, maxLevel) = eq.bandLevelRange
                val minDb = minLevel / 100f
                val maxDb = maxLevel / 100f

                for (i in 0 until numBands) {
                    val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000
                    bands.add(
                        EqualizerBand(
                            bandIndex = i,
                            centerFreqHz = centerFreqHz,
                            levelDb = 0f,
                            minDb = minDb,
                            maxDb = maxDb
                        )
                    )
                }
                _equalizerBands.value = bands

                // BassBoost
                val bb = BassBoost(0, audioSessionId)
                bb.enabled = true
                androidBassBoost = bb

                // Virtualizer
                val virt = Virtualizer(0, audioSessionId)
                virt.enabled = true
                androidVirtualizer = virt
            } catch (_: Exception) {
                // Fallback default 5-band representation if hardware audiofx is restricted
                setupFallbackEqualizer()
            }
        } else if (_equalizerBands.value.isEmpty()) {
            setupFallbackEqualizer()
        }
    }

    private fun setupFallbackEqualizer() {
        val fallbackFrequencies = listOf(60, 230, 910, 3600, 14000)
        _equalizerBands.value = fallbackFrequencies.mapIndexed { idx, freq ->
            EqualizerBand(
                bandIndex = idx,
                centerFreqHz = freq,
                levelDb = 0f,
                minDb = -10f,
                maxDb = 10f
            )
        }
    }

    fun playMedia(media: MediaItemData, newQueue: List<MediaItemData>? = null) {
        if (newQueue != null) {
            _queue.value = newQueue
        }
        _currentMedia.value = media
        val mediaItem = MediaItem.fromUri(Uri.parse(media.uri))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        seekTo(newPos)
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentMedia.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }

        val nextIndex = if (_isShuffle.value) {
            (q.indices - currentIndex.coerceAtLeast(0)).randomOrNull() ?: 0
        } else {
            (currentIndex + 1) % q.size
        }
        playMedia(q[nextIndex])
    }

    fun playPrevious() {
        if (exoPlayer.currentPosition > 3000L) {
            seekTo(0L)
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentMedia.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else q.size - 1
        playMedia(q[prevIndex])
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun toggleAudioOnlyForVideo() {
        _isAudioOnlyForVideo.value = !_isAudioOnlyForVideo.value
    }

    // A-B Loop Controls
    fun setPointA() {
        _aLoopMs.value = exoPlayer.currentPosition
        if (_bLoopMs.value != null && _bLoopMs.value!! > _aLoopMs.value!!) {
            _isAbLoopActive.value = true
        }
    }

    fun setPointB() {
        val current = exoPlayer.currentPosition
        val a = _aLoopMs.value ?: 0L
        if (current > a) {
            _bLoopMs.value = current
            _isAbLoopActive.value = true
        }
    }

    fun clearAbLoop() {
        _aLoopMs.value = null
        _bLoopMs.value = null
        _isAbLoopActive.value = false
    }

    // Sleep Timer
    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }
        val totalSecs = minutes * 60
        _sleepTimerSeconds.value = totalSecs

        sleepTimerJob = scope.launch {
            var remaining = totalSecs
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            if (remaining <= 0) {
                exoPlayer.pause()
                _sleepTimerSeconds.value = null
            }
        }
    }

    // Equalizer Controls
    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        val currentBands = _equalizerBands.value.toMutableList()
        if (bandIndex in currentBands.indices) {
            val band = currentBands[bandIndex]
            currentBands[bandIndex] = band.copy(levelDb = levelDb)
            _equalizerBands.value = currentBands

            androidEqualizer?.let { eq ->
                try {
                    val milliBels = (levelDb * 100).toInt().toShort()
                    eq.setBandLevel(bandIndex.toShort(), milliBels)
                } catch (_: Exception) {}
            }
            _currentPreset.value = "Custom"
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        _currentPreset.value = preset.name
        val currentBands = _equalizerBands.value.toMutableList()
        preset.bandLevels.forEachIndexed { index, level ->
            if (index in currentBands.indices) {
                currentBands[index] = currentBands[index].copy(levelDb = level)
                androidEqualizer?.let { eq ->
                    try {
                        val milliBels = (level * 100).toInt().toShort()
                        eq.setBandLevel(index.toShort(), milliBels)
                    } catch (_: Exception) {}
                }
            }
        }
        _equalizerBands.value = currentBands
    }

    fun setBassBoost(strength: Int) {
        _bassBoostStrength.value = strength
        androidBassBoost?.let { bb ->
            try {
                bb.setStrength(strength.toShort())
            } catch (_: Exception) {}
        }
    }

    fun setVirtualizer(strength: Int) {
        _virtualizerStrength.value = strength
        androidVirtualizer?.let { virt ->
            try {
                virt.setStrength(strength.toShort())
            } catch (_: Exception) {}
        }
    }

    private fun handlePlaybackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val q = _queue.value
                val current = _currentMedia.value
                val currentIndex = q.indexOfFirst { it.id == current?.id }
                if (currentIndex in 0 until q.size - 1) {
                    playNext()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun startTrackingProgress() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition
                    _positionMs.value = pos
                    _bufferedPositionMs.value = exoPlayer.bufferedPosition

                    // Check A-B loop boundary
                    if (_isAbLoopActive.value) {
                        val b = _bLoopMs.value
                        val a = _aLoopMs.value ?: 0L
                        if (b != null && pos >= b) {
                            exoPlayer.seekTo(a)
                        }
                    }
                }
                delay(200L)
            }
        }
    }

    private fun startVisualizerEngine() {
        visualizerJob?.cancel()
        visualizerJob = scope.launch {
            var step = 0.0
            while (isActive) {
                if (_isPlaying.value) {
                    step += 0.2
                    val bars = List(32) { i ->
                        val base = (sin(step + i * 0.4) * 0.4 + 0.5).toFloat()
                        val pulse = (sin(step * 1.5 + i * 0.8) * 0.25).toFloat()
                        (base + pulse).coerceIn(0.08f, 0.98f)
                    }
                    _visualizerAmplitudes.value = bars
                } else {
                    _visualizerAmplitudes.value = List(32) { 0.08f }
                }
                delay(60L)
            }
        }
    }

    fun release() {
        progressTrackingJob?.cancel()
        visualizerJob?.cancel()
        sleepTimerJob?.cancel()
        androidEqualizer?.release()
        androidBassBoost?.release()
        androidVirtualizer?.release()
        exoPlayer.release()
    }
}
