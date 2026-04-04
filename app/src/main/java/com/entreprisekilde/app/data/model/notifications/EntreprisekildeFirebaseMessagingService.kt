package com.entreprisekilde.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.entreprisekilde.app.MainActivity
import com.entreprisekilde.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles Firebase Cloud Messaging (FCM) events.
 *
 * Responsibilities:
 * - Receive push notifications from backend (Cloud Functions)
 * - Handle token updates (used to identify device)
 * - Display local notifications to the user
 */
class EntreprisekildeFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        // Unique channel ID (must stay consistent once used in production)
        const val CHANNEL_ID = "entreprisekilde_urgent_v2"

        // User-visible name of the notification channel
        const val CHANNEL_NAME = "Entreprisekilde Urgent"
    }

    /**
     * Called whenever a new FCM token is generated for this device.
     *
     * This can happen:
     * - On first install
     * - When app data is cleared
     * - When Firebase refreshes the token
     *
     * We sync the token to Firestore so backend can send push notifications to this user.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        // Sync token in background (avoid blocking main thread)
        CoroutineScope(Dispatchers.IO).launch {
            FcmTokenManager.syncCurrentTokenToLoggedInUser()
        }
    }

    /**
     * Called when a push notification is received.
     *
     * This handles both:
     * - data payload (custom data from backend)
     * - notification payload (Firebase default notification)
     *
     * We extract relevant info and show a local notification.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Debug logs to help track issues with push notifications
        Log.d("FCM", "From: ${message.from}")
        Log.d("FCM", "Data payload: ${message.data}")
        Log.d("FCM", "Notification payload title: ${message.notification?.title}")
        Log.d("FCM", "Notification payload body: ${message.notification?.body}")

        // Prefer data payload (more reliable/customizable), fallback to notification payload
        val title = message.data["title"]
            ?: message.notification?.title
            ?: "Entreprisekilde"

        val body = message.data["body"]
            ?: message.notification?.body
            ?: "You have a new notification"

        // Optional debugging for sender/recipient logic (useful for your current bug)
        val senderUserId = message.data["senderUserId"] ?: ""
        val recipientUserId = message.data["recipientUserId"] ?: ""

        Log.d("FCM", "senderUserId=$senderUserId recipientUserId=$recipientUserId")

        // Display notification to user
        showNotification(title, body)
    }

    /**
     * Builds and displays the local notification.
     *
     * This is what the user actually sees on their device.
     */
    private fun showNotification(title: String, body: String) {

        // Ensure notification channel exists (required for Android 8+)
        createUrgentNotificationChannel()

        // Intent that opens the app when user taps notification
        val intent = Intent(this, MainActivity::class.java).apply {
            // Clear existing stack and open fresh activity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Wrap intent in PendingIntent (required for notifications)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Default notification sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // Expandable text
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority (heads-up)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setAutoCancel(true) // Dismiss when clicked
            .setContentIntent(pendingIntent)
            .build()

        // Android 13+ requires runtime notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w("FCM", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        // Show the notification with a unique ID
        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            notification
        )

        Log.d("FCM", "Notification shown")
    }

    /**
     * Creates the notification channel (Android 8+ requirement).
     *
     * This defines:
     * - importance (HIGH → heads-up notification)
     * - sound & vibration
     * - lockscreen visibility
     *
     * Note: Once created, channel settings cannot be changed programmatically.
     */
    private fun createUrgentNotificationChannel() {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Urgent notifications"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            // Custom sound configuration
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}