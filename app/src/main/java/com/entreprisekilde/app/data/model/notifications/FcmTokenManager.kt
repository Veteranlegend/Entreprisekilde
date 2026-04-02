package com.entreprisekilde.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

            Log.d(TAG, "Syncing token for userId=$currentUserId token=$token")

            removeTokenFromOtherUsers(
                token = token,
                currentUserId = currentUserId
            )

            val userRef = firestore.collection("users").document(currentUserId)

            userRef.set(
                mapOf(
                    "fcmToken" to token,
                    "fcmTokens" to FieldValue.arrayUnion(token),
                    "lastTokenUpdatedAt" to System.currentTimeMillis()
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

            userRef.set(
                mapOf(
                    "fcmTokens" to FieldValue.arrayRemove(token),
                    "lastTokenUpdatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            val snapshot = userRef.get().await()
            val remainingTokens = (snapshot.get("fcmTokens") as? List<*>)?.filterIsInstance<String>()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            userRef.set(
                mapOf(
                    "fcmToken" to (remainingTokens.firstOrNull() ?: "")
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
            val cleanedSingleToken = cleanedTokens.firstOrNull() ?: ""

            firestore.collection("users")
                .document(doc.id)
                .set(
                    mapOf(
                        "fcmToken" to cleanedSingleToken,
                        "fcmTokens" to cleanedTokens,
                        "lastTokenUpdatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()

            Log.d(TAG, "Removed duplicate token from userId=${doc.id}")
        }
    }
}