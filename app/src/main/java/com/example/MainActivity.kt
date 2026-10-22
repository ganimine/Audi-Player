package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PlayerViewModel
import com.example.widget.AudiPlayerWidgetProvider

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    // Activity Result Launcher for Media and Call Permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isAudioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
        val isVideoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
        val isStorageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (isAudioGranted || isVideoGranted || isStorageGranted) {
            // Permission granted, automatically scan local media files
            viewModel.scanDeviceStorage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and request media & phone state permissions on startup
        requestPermissions()

        // Handle incoming audio or video intent or widget action
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Checks and requests media and phone state permissions for call auto-stop.
     */
    fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)

        val allMediaGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (allMediaGranted) {
            viewModel.scanDeviceStorage(silent = true)
        }

        val ungranted = permissionsToRequest.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (viewModel.headsetController.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            AudiPlayerWidgetProvider.ACTION_PLAY_PAUSE -> {
                viewModel.playerController.togglePlayPause()
            }
            AudiPlayerWidgetProvider.ACTION_NEXT -> {
                viewModel.playerController.playNext()
            }
            AudiPlayerWidgetProvider.ACTION_PREV -> {
                viewModel.playerController.playPrevious()
            }
            "androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK" -> {
                viewModel.setNowPlayingExpanded(true)
            }
            else -> {
                val uri = intent?.data
                if (uri != null) {
                    viewModel.importFileUri(uri)
                }
            }
        }
    }
}
