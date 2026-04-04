package com.entreprisekilde.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Handles FCM token management for the app.
 *
 * Responsibilities:
 * - Sync device token with the logged-in user
 * - Ensure token uniqueness across users (VERY important)
 * - Remove tokens when user logs out
 *
 * This is critical for making sure push notifications are sent to the correct user.
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    // Firebase Auth instance (used to get current user)
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    // Firestore instance (used to store tokens)
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Syncs the current device FCM token to the logged-in user.
     *
     * Flow:
     * 1. Get current user ID
     * 2. Get FCM token for this device
     * 3. Remove token from any other users (prevents cross-user notifications)
     * 4. Save token to current user
     */
    suspend fun syncCurrentTokenToLoggedInUser() {
        try {
            val currentUserId = auth.currentUser?.uid

            // If no user is logged in, we should not sync any token
            if (currentUserId.isNullOrBlank()) {
                Log.d(TAG, "No logged in user. Skipping token sync.")
                return
            }

            val token = FirebaseMessaging.getInstance().token.await().trim()

            // Token should never be blank, but we guard against it anyway
            if (token.isBlank()) {
                Log.d(TAG, "FCM token is blank. Skipping token sync.")
                return
            }

            Log.d(TAG, "Syncing token for userId=$currentUserId token=$token")

            // VERY IMPORTANT:
            // Ensure this token is not assigned to another user
            removeTokenFromOtherUsers(
                token = token,
                currentUserId = currentUserId
            )

            val userRef = firestore.collection("users").document(currentUserId)

            // Store:
            // - fcmToken (primary token)
            // - fcmTokens (supports multiple devices)
            // - timestamp for tracking updates
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

    /**
     * Removes the current device token from a user.
     *
     * Typically used on logout.
     *
     * Also ensures:
     * - token is removed from array
     * - primary token is updated correctly
     */
    suspend fun removeCurrentTokenFromUser(userId: String) {
        try {
            if (userId.isBlank()) return

            val token = FirebaseMessaging.getInstance().token.await().trim()
            if (token.isBlank()) return

            val userRef = firestore.collection("users").document(userId)

            // Remove token from token list
            userRef.set(
                mapOf(
                    "fcmTokens" to FieldValue.arrayRemove(token),
                    "lastTokenUpdatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            // Fetch remaining tokens to update primary token
            val snapshot = userRef.get().await()
            val remainingTokens = (snapshot.get("fcmTokens") as? List<*>)
                ?.filterIsInstance<String>()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            // Set new primary token (or empty if none left)
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

    /**
     * Ensures that a token belongs to ONLY one user.
     *
     * This prevents a critical bug where:
     * - User A logs in on a device
     * - Then User B logs in on the same device
     * - Both users receive notifications (WRONG)
     *
     * Solution:
     * Remove this token from all other users before assigning it.
     */
    private suspend fun removeTokenFromOtherUsers(
        token: String,
        currentUserId: String
    ) {
        val snapshot = firestore.collection("users").get().await()

        for (doc in snapshot.documents) {

            // Skip current user
            if (doc.id == currentUserId) continue

            val singleToken = doc.getString("fcmToken")?.trim().orEmpty()

            val tokenList = (doc.get("fcmTokens") as? List<*>)
                ?.filterIsInstance<String>()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            // Check if this user has the same token
            val containsToken = singleToken == token || tokenList.contains(token)
            if (!containsToken) continue

            // Remove the token from this user
            val cleanedTokens = tokenList.filter { it != token }

            // Update primary token (fallback to first available or empty)
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