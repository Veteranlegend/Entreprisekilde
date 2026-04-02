package com.entreprisekilde.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntreprisekildeFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "entreprisekilde_urgent_v2"
        const val CHANNEL_NAME = "Entreprisekilde Urgent"
        private const val LOGIN_PREFS = "login_prefs"
        private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        CoroutineScope(Dispatchers.IO).launch {
            FcmTokenManager.syncCurrentTokenToLoggedInUser()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "From: ${message.from}")
        Log.d("FCM", "Data payload: ${message.data}")
        Log.d("FCM", "Notification payload title: ${message.notification?.title}")
        Log.d("FCM", "Notification payload body: ${message.notification?.body}")

        val currentUserId = resolveCurrentUserId()

        if (currentUserId.isBlank()) {
            Log.d("FCM", "No logged-in app user -> ignore local display")
            return
        }

        val senderUserId = message.data["senderUserId"] ?: ""
        val recipientUserId = message.data["recipientUserId"] ?: ""

        if (senderUserId == currentUserId) {
            Log.d("FCM", "Ignoring self notification")
            return
        }

        if (recipientUserId.isNotBlank() && recipientUserId != currentUserId) {
            Log.d("FCM", "Not for this user -> ignore")
            return
        }

        val title = message.data["title"]
            ?: message.notification?.title
            ?: "Entreprisekilde"

        val body = message.data["body"]
            ?: message.notification?.body
            ?: "You have a new notification"

        showNotification(title, body)
    }

    private fun resolveCurrentUserId(): String {
        val authUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (!authUserId.isNullOrBlank()) {
            return authUserId
        }

        val prefs = getSharedPreferences(LOGIN_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGGED_IN_USER_ID, "") ?: ""
    }

    private fun showNotification(title: String, body: String) {
        createUrgentNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

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

        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            notification
        )

        Log.d("FCM", "Notification shown")
    }

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
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}