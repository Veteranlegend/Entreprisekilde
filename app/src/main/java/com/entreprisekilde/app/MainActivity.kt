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

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("FCM", "POST_NOTIFICATIONS granted: $isGranted")
            if (isGranted) {
                lifecycleScope.launch {
                    FcmTokenManager.syncCurrentTokenToLoggedInUser()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            FcmTokenManager.syncCurrentTokenToLoggedInUser()
        }

        setContent {
            EntreprisekildeTheme {
                EntreprisekildeApp()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
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