package com.entreprisekilde.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.entreprisekilde.app.notifications.FcmTokenManager
import com.entreprisekilde.app.ui.navigation.EntreprisekildeApp
import com.entreprisekilde.app.ui.theme.EntreprisekildeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Runtime permission launcher for push notifications on newer Android versions.
    // Once the user responds, we log the result and sync the FCM token if permission was granted.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("FCM", "POST_NOTIFICATIONS granted: $isGranted")

            if (isGranted) {
                lifecycleScope.launch {
                    // Now that notifications are allowed, make sure the current device token
                    // is stored for the logged-in user so this device can receive push messages.
                    FcmTokenManager.syncCurrentTokenToLoggedInUser()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for notification permission when needed before the user gets too far into the app.
        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            // Try to sync the current FCM token on app start.
            // This helps keep the backend/device mapping up to date even if permission was already granted.
            FcmTokenManager.syncCurrentTokenToLoggedInUser()
        }

        setContent {
            EntreprisekildeTheme {
                EntreprisekildeApp()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        // Android 13+ requires POST_NOTIFICATIONS as a runtime permission.
        // On older versions, this permission does not need to be requested this way.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!alreadyGranted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}