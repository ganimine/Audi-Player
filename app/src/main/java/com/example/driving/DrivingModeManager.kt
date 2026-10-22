package com.example.driving

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import kotlin.math.sqrt

/**
 * DrivingModeManager detects whether the vehicle/device is in motion.
 * When in motion, it enforces road safety policies restricting video display
 * while permitting uninterrupted audio streaming. Prohibited video interactions
 * trigger explicit warning toasts and UI messages.
 */
class DrivingModeManager(
    private val context: Context,
    var onMotionStateChanged: (isMoving: Boolean) -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager

    // Speed threshold in km/h above which vehicle is considered in motion (~5 km/h = 1.38 m/s)
    val motionSpeedThresholdKmh = 5.0f

    // Motion State Flows
    private val _isVehicleMoving = MutableStateFlow(false)
    val isVehicleMoving: StateFlow<Boolean> = _isVehicleMoving.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0.0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    private val _motionStatusDescription = MutableStateFlow("Parked / Stationary (0 km/h)")
    val motionStatusDescription: StateFlow<String> = _motionStatusDescription.asStateFlow()

    private val _isDrivingRestrictionEnabled = MutableStateFlow(true)
    val isDrivingRestrictionEnabled: StateFlow<Boolean> = _isDrivingRestrictionEnabled.asStateFlow()

    private val _isSimulatingMotion = MutableStateFlow(false)
    val isSimulatingMotion: StateFlow<Boolean> = _isSimulatingMotion.asStateFlow()

    private val _lastRestrictionMessage = MutableStateFlow<String?>(null)
    val lastRestrictionMessage: StateFlow<String?> = _lastRestrictionMessage.asStateFlow()

    private var simulationJob: Job? = null
    private var isLocationListening = false
    private var isSensorListening = false

    // Hardware GPS Location Listener
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (_isSimulatingMotion.value) return // Simulated motion overrides hardware GPS

            val speedMps = if (location.hasSpeed()) location.speed else 0.0f
            val speedKmh = speedMps * 3.6f
            updateSpeedAndMotionState(speedKmh, source = "GPS")
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    // Accelerometer / Motion Sensor Listener
    private val sensorEventListener = object : SensorEventListener {
        private var lastAcceleration = 0f
        private var currentAcceleration = SensorManager.GRAVITY_EARTH
        private var deltaAccumulator = 0f

        override fun onSensorChanged(event: SensorEvent?) {
            if (_isSimulatingMotion.value) return
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAcceleration = currentAcceleration
                currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = kotlin.math.abs(currentAcceleration - lastAcceleration)
                deltaAccumulator = deltaAccumulator * 0.9f + delta * 0.1f

                // Sustained vehicle acceleration when GPS is unavailable
                if (!_isVehicleMoving.value && deltaAccumulator > 3.5f) {
                    val estimatedSpeed = 25.0f
                    updateSpeedAndMotionState(estimatedSpeed, source = "Inertial Sensor")
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        startMotionMonitoring()
    }

    /**
     * Starts listening to GPS speed and device sensors.
     */
    fun startMotionMonitoring() {
        // 1. Check UI Car Mode
        checkCarMode()

        // 2. Start GPS Location Monitoring if permissions granted
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if ((hasFine || hasCoarse) && !isLocationListening) {
            try {
                locationManager?.let { lm ->
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, locationListener, Looper.getMainLooper())
                        isLocationListening = true
                    }
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5.0f, locationListener, Looper.getMainLooper())
                        isLocationListening = true
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Register Accelerometer for movement sensing
        if (!isSensorListening) {
            sensorManager?.let { sm ->
                val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (accel != null) {
                    sm.registerListener(sensorEventListener, accel, SensorManager.SENSOR_DELAY_NORMAL)
                    isSensorListening = true
                }
            }
        }
    }

    fun stopMotionMonitoring() {
        if (isLocationListening) {
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (_: Exception) {}
            isLocationListening = false
        }
        if (isSensorListening) {
            try {
                sensorManager?.unregisterListener(sensorEventListener)
            } catch (_: Exception) {}
            isSensorListening = false
        }
    }

    private fun checkCarMode() {
        val isCarUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR
        if (isCarUiMode && !_isVehicleMoving.value) {
            _motionStatusDescription.value = "Car Mode Connected"
        }
    }

    /**
     * Updates current speed and checks whether car has transitioned between stationary and in-motion.
     */
    fun updateSpeedAndMotionState(speedKmh: Float, source: String = "GPS") {
        val clampedSpeed = speedKmh.coerceAtLeast(0.0f)
        _currentSpeedKmh.value = clampedSpeed

        val wasMoving = _isVehicleMoving.value
        val isNowMoving = clampedSpeed >= motionSpeedThresholdKmh

        if (isNowMoving != wasMoving) {
            _isVehicleMoving.value = isNowMoving
            onMotionStateChanged(isNowMoving)

            if (isNowMoving) {
                notifyProhibitedVideoInteraction(
                    "Vehicle in motion (${clampedSpeed.toInt()} km/h): Video playback is restricted for safety. Audio will continue streaming."
                )
            }
        }

        _motionStatusDescription.value = if (isNowMoving) {
            "In Motion (${clampedSpeed.toInt()} km/h • $source)"
        } else {
            "Parked / Stationary (0 km/h)"
        }
    }

    /**
     * Simulates car speed in km/h to allow testing in AI Studio preview or stationary vehicles.
     */
    fun simulateSpeed(speedKmh: Float) {
        simulationJob?.cancel()
        _isSimulatingMotion.value = true
        updateSpeedAndMotionState(speedKmh, source = "Simulation")
    }

    /**
     * Simulates realistic acceleration, cruising, and parking cycle for testing.
     */
    fun simulateDrivingCycle(targetSpeedKmh: Float = 55.0f, durationSecs: Int = 15) {
        simulationJob?.cancel()
        _isSimulatingMotion.value = true
        simulationJob = scope.launch {
            // Accelerate
            var current = 0f
            while (current < targetSpeedKmh && isActive) {
                current += 10f
                updateSpeedAndMotionState(current, source = "Simulated Drive")
                delay(600L)
            }
            // Cruise
            delay((durationSecs * 1000L).coerceAtLeast(3000L))
            // Decelerate to stop
            while (current > 0f && isActive) {
                current = (current - 12f).coerceAtLeast(0f)
                updateSpeedAndMotionState(current, source = "Simulated Decel")
                delay(600L)
            }
            updateSpeedAndMotionState(0.0f, source = "Simulated Parked")
            _isSimulatingMotion.value = false
        }
    }

    /**
     * Stops simulation and resets to stationary.
     */
    fun stopSimulation() {
        simulationJob?.cancel()
        _isSimulatingMotion.value = false
        updateSpeedAndMotionState(0.0f, source = "Stopped")
    }

    fun setDrivingRestrictionEnabled(enabled: Boolean) {
        _isDrivingRestrictionEnabled.value = enabled
    }

    /**
     * Verifies if video display is permissible under vehicle motion constraints.
     * If vehicle is in motion, returns false, triggers a warning toast & UI alert,
     * and signals that audio streaming should be enforced.
     */
    fun checkVideoAllowedAndNotify(showToast: Boolean = true): Boolean {
        if (!_isDrivingRestrictionEnabled.value) return true

        if (_isVehicleMoving.value) {
            val speed = _currentSpeedKmh.value.toInt()
            val msg = "While driving ($speed km/h), video playback is prohibited for vehicle safety. Switched to Audio Only playback."
            _lastRestrictionMessage.value = msg

            if (showToast) {
                mainHandler.post {
                    Toast.makeText(context, "⚠️ Vehicle in Motion: Video display is prohibited. Audio streaming is active.", Toast.LENGTH_LONG).show()
                }
            }
            return false
        }
        return true
    }

    /**
     * Triggers warning toast and registers UI status message when user interacts with prohibited video elements while driving.
     */
    fun notifyProhibitedVideoInteraction(customMsg: String? = null) {
        val msg = customMsg ?: "⚠️ Vehicle in motion: Video playback restricted while driving. Audio streaming will continue."
        _lastRestrictionMessage.value = msg
        mainHandler.post {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun clearRestrictionMessage() {
        _lastRestrictionMessage.value = null
    }

    fun release() {
        simulationJob?.cancel()
        stopMotionMonitoring()
    }
}
