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

    // Activity Result Launcher for Media and Storage Permissions
    private val storagePermissionLauncher = registerForActivityResult(
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

        // Check and request media permissions on startup
        requestMediaPermissions()

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
     * Checks and requests READ_MEDIA_AUDIO & READ_MEDIA_VIDEO for Android 13+ (API 33+),
     * or READ_EXTERNAL_STORAGE for Android 12 and below.
     */
    fun requestMediaPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val allGranted = permissionsToRequest.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.scanDeviceStorage()
        } else {
            storagePermissionLauncher.launch(permissionsToRequest)
        }
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
            else -> {
                val uri = intent?.data
                if (uri != null) {
                    viewModel.importFileUri(uri)
                }
            }
        }
    }
}
