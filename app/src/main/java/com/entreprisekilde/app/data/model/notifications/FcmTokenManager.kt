package com.entreprisekilde.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    suspend fun syncCurrentTokenToLoggedInUser() {
        try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId.isNullOrBlank()) {
                Log.d(TAG, "No logged in user. Skipping token sync.")
                return
            }

            val token = FirebaseMessaging.getInstance().token.await().trim()
            if (token.isBlank()) {
                Log.d(TAG, "FCM token is blank. Skipping token sync.")
                return
            }

            Log.d(TAG, "Syncing token for userId=$currentUserId")

            removeTokenFromOtherUsers(
                token = token,
                currentUserId = currentUserId
            )

            val userRef = firestore.collection("users").document(currentUserId)

            val snapshot = userRef.get().await()

            val existingTokens = (snapshot.get("fcmTokens") as? List<*>)?.filterIsInstance<String>()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val updatedTokens = (existingTokens + token).distinct()

            userRef.set(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokens" to updatedTokens
                ),
                SetOptions.merge()
            ).await()

            Log.d(TAG, "FCM token synced successfully for userId=$currentUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync FCM token", e)
        }
    }

    suspend fun removeCurrentTokenFromUser(userId: String) {
        try {
            if (userId.isBlank()) return

            val token = FirebaseMessaging.getInstance().token.await().trim()
            if (token.isBlank()) return

            val userRef = firestore.collection("users").document(userId)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) return

            val existingTokens = (snapshot.get("fcmTokens") as? List<*>)?.filterIsInstance<String>()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val updatedTokens = existingTokens.filter { it != token }

            val newSingleToken = updatedTokens.firstOrNull() ?: ""

            userRef.set(
                mapOf(
                    "fcmToken" to newSingleToken,
                    "fcmTokens" to updatedTokens
                ),
                SetOptions.merge()
            ).await()

            Log.d(TAG, "Removed token from userId=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove token from user", e)
        }
    }

    private suspend fun removeTokenFromOtherUsers(
        token: String,
        currentUserId: String
    ) {
        val snapshot = firestore.collection("users").get().await()

        for (doc in snapshot.documents) {
            if (doc.id == currentUserId) continue

            val singleToken = doc.getString("fcmToken")?.trim().orEmpty()
            val tokenList = (doc.get("fcmTokens") as? List<*>)?.filterIsInstance<String>()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val containsToken = singleToken == token || tokenList.contains(token)
            if (!containsToken) continue

            val cleanedTokens = tokenList.filter { it != token }
            val cleanedSingleToken = if (singleToken == token) {
                cleanedTokens.firstOrNull() ?: ""
            } else {
                singleToken
            }

            firestore.collection("users")
                .document(doc.id)
                .set(
                    mapOf(
                        "fcmToken" to cleanedSingleToken,
                        "fcmTokens" to cleanedTokens
                    ),
                    SetOptions.merge()
                )
                .await()

            Log.d(TAG, "Removed duplicate token from userId=${doc.id}")
        }
    }
}