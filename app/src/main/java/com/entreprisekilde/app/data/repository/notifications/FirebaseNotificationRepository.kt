package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed implementation of [NotificationRepository].
 *
 * This repository is responsible for:
 * - fetching notifications for a specific user
 * - observing notification changes in real time
 * - creating new notifications
 * - marking notifications as read
 * - deleting notifications
 *
 * One important detail in this implementation:
 * we do not rely only on each notification's `isRead` field.
 * We also store `lastNotificationsOpenedAt` on the user document,
 * which lets us treat older notifications as read once the user
 * has opened the notifications screen.
 */
class FirebaseNotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

    // Main Firestore collection that stores all app notifications.
    private val notificationsCollection = firestore.collection("notifications")

    // User collection is used here mainly to read/write `lastNotificationsOpenedAt`.
    private val usersCollection = firestore.collection("users")

    // Holds the active real-time listener so we can remove it before attaching a new one.
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Fetches all notifications for a given user.
     *
     * We also read the user's `lastNotificationsOpenedAt` timestamp and use it
     * to "soft-mark" older notifications as read in memory, even if their stored
     * `isRead` value is still false.
     *
     * This gives the UI a more accurate read/unread state without requiring every
     * old notification document to always be individually updated first.
     */
    override suspend fun getNotifications(userId: String): List<AppNotification> {
        return try {
            val lastOpenedAt = getLastNotificationsOpenedAt(userId)

            notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    // Convert each Firestore document into our app model,
                    // then inject the Firestore document ID into the object.
                    document.toObject(AppNotification::class.java)
                        ?.copy(id = document.id)
                        // Apply derived read state based on when notifications
                        // were last opened by the user.
                        ?.markReadUsingLastOpenedAt(lastOpenedAt)
                }
                // Show newest notifications first.
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            // Fail safely for the UI instead of crashing the app.
            emptyList()
        }
    }

    /**
     * Starts observing notifications for a given user in real time.
     *
     * Flow:
     * 1. Remove any old listener to avoid duplicate callbacks / leaks.
     * 2. Listen for notification collection changes for this user.
     * 3. On every change, also fetch the user's `lastNotificationsOpenedAt`.
     * 4. Build the updated notification list and return it via `onChanged`.
     *
     * Note:
     * This uses a nested Firestore read inside the snapshot listener because
     * the effective read state depends on both notification data and user data.
     */
    override fun observeNotifications(
        userId: String,
        onChanged: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Always clean up any previous listener before attaching a new one.
        removeNotificationListener()

        listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                // We need the user's last-opened timestamp to correctly determine
                // which notifications should appear as read in the UI.
                firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val lastOpenedAt = (userDoc.getLong("lastNotificationsOpenedAt") ?: 0L)

                        val updatedNotifications = snapshot?.documents
                            ?.mapNotNull { document ->
                                document.toObject(AppNotification::class.java)
                                    ?.copy(id = document.id)
                                    ?.markReadUsingLastOpenedAt(lastOpenedAt)
                            }
                            ?.sortedByDescending { it.createdAt }
                            ?: emptyList()

                        onChanged(updatedNotifications)
                    }
                    .addOnFailureListener { userError ->
                        onError(userError)
                    }
            }
    }

    /**
     * Removes the currently active real-time listener, if one exists.
     *
     * This is important to prevent:
     * - duplicate listeners
     * - memory leaks
     * - callbacks continuing after the screen is gone
     */
    override fun removeNotificationListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Creates a "new message" notification for the recipient.
     *
     * We skip the operation entirely if the recipient ID is blank,
     * since there is nowhere valid to store the notification.
     */
    override suspend fun addMessageNotification(
        senderName: String,
        recipientUserId: String,
        threadId: Int
    ) {
        if (recipientUserId.isBlank()) return

        val notification = AppNotification(
            userId = recipientUserId,
            title = "New message",
            message = "$senderName sent you a message",
            type = NotificationType.MESSAGE,
            createdAt = System.currentTimeMillis(),
            isRead = false,
            relatedThreadId = threadId
        )

        notificationsCollection.add(notification).await()
    }

    /**
     * Creates a task assignment notification for the assigned user.
     *
     * `assignedToName` is currently not used in the message body, but it may
     * still be part of the method signature for consistency with callers or
     * future personalization needs.
     */
    override suspend fun addTaskAssignedNotification(
        taskName: String,
        assignedUserId: String,
        assignedToName: String
    ) {
        if (assignedUserId.isBlank()) return

        val notification = AppNotification(
            userId = assignedUserId,
            title = "Task assigned",
            message = "You were assigned \"$taskName\"",
            type = NotificationType.TASK_ASSIGNED,
            createdAt = System.currentTimeMillis(),
            isRead = false,
            relatedThreadId = null
        )

        notificationsCollection.add(notification).await()
    }

    /**
     * Marks a single notification as read by updating its stored `isRead` field.
     */
    override suspend fun markAsRead(notificationId: String) {
        notificationsCollection
            .document(notificationId)
            .update("isRead", true)
            .await()
    }

    /**
     * Marks all notifications for the user as read.
     *
     * This does two things in one batch:
     * 1. Updates any unread notification documents to `isRead = true`
     * 2. Stores `lastNotificationsOpenedAt = now` on the user document
     *
     * Updating the timestamp is useful because it gives us a reliable fallback
     * for read state, even if some notification docs were missed previously or
     * new UI logic depends on "opened after creation time".
     */
    override suspend fun markAllAsRead(userId: String) {
        val now = System.currentTimeMillis()

        val snapshot = notificationsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()

        val batch = firestore.batch()

        snapshot.documents.forEach { document ->
            val isRead = document.getBoolean("isRead") ?: false

            // Only update documents that are still unread to avoid unnecessary writes.
            if (!isRead) {
                batch.update(document.reference, "isRead", true)
            }
        }

        // Save the timestamp of when the user last opened / cleared notifications.
        // `merge()` makes sure we do not overwrite the rest of the user document.
        batch.set(
            usersCollection.document(userId),
            mapOf("lastNotificationsOpenedAt" to now),
            com.google.firebase.firestore.SetOptions.merge()
        )

        batch.commit().await()
    }

    /**
     * Returns the number of unread notifications for the user.
     *
     * A notification counts as unread only if:
     * - its stored `isRead` field is false
     * - and it was created after `lastNotificationsOpenedAt`
     *
     * That second condition avoids counting older notifications that should
     * effectively be treated as already seen.
     */
    override suspend fun unreadCount(userId: String): Int {
        return try {
            val lastOpenedAt = getLastNotificationsOpenedAt(userId)

            notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .count { document ->
                    val isRead = document.getBoolean("isRead") ?: false
                    val createdAt = document.getLong("createdAt") ?: 0L
                    !isRead && createdAt > lastOpenedAt
                }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Deletes a notification document permanently.
     */
    override suspend fun deleteNotification(notificationId: String) {
        notificationsCollection
            .document(notificationId)
            .delete()
            .await()
    }

    /**
     * Reads the user's `lastNotificationsOpenedAt` timestamp.
     *
     * Returns 0 if:
     * - the field does not exist
     * - the user document cannot be read
     * - any Firestore error occurs
     *
     * Using 0 as the fallback means "the notifications have never been opened".
     */
    private suspend fun getLastNotificationsOpenedAt(userId: String): Long {
        return try {
            usersCollection
                .document(userId)
                .get()
                .await()
                .getLong("lastNotificationsOpenedAt") ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Returns a copy of the notification with an adjusted read state.
     *
     * If the notification is still stored as unread, but it was created before
     * or at the time the user last opened notifications, we treat it as read
     * when presenting it to the app.
     *
     * This helper is intentionally pure and does not write anything to Firestore.
     * It only adjusts the object returned to the caller.
     */
    private fun AppNotification.markReadUsingLastOpenedAt(lastOpenedAt: Long): AppNotification {
        return if (!isRead && createdAt <= lastOpenedAt) {
            copy(isRead = true)
        } else {
            this
        }
    }
}