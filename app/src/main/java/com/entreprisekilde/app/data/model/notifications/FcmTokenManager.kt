package com.entreprisekilde.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    suspend fun syncCurrentTokenToLoggedInUser() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d("FCM", "No logged in user. Token not saved.")
            return
        }

        try {
            val token = FirebaseMessaging.getInstance().token.await()

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(
                    mapOf("fcmToken" to token),
                    SetOptions.merge()
                )
                .await()

            Log.d("FCM", "CURRENT TOKEN: $token")
            Log.d("FCM", "FCM token saved successfully")
        } catch (e: Exception) {
            Log.e("FCM", "Failed to sync FCM token", e)
        }
    }
}