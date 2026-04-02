package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

    private val notificationsCollection = firestore.collection("notifications")
    private val usersCollection = firestore.collection("users")
    private var listenerRegistration: ListenerRegistration? = null

    override suspend fun getNotifications(userId: String): List<AppNotification> {
        return try {
            val lastOpenedAt = getLastNotificationsOpenedAt(userId)

            notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(AppNotification::class.java)
                        ?.copy(id = document.id)
                        ?.markReadUsingLastOpenedAt(lastOpenedAt)
                }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun observeNotifications(
        userId: String,
        onChanged: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        removeNotificationListener()

        listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

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

    override fun removeNotificationListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

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

    override suspend fun markAsRead(notificationId: String) {
        notificationsCollection
            .document(notificationId)
            .update("isRead", true)
            .await()
    }

    override suspend fun markAllAsRead(userId: String) {
        val now = System.currentTimeMillis()

        val snapshot = notificationsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()

        val batch = firestore.batch()

        snapshot.documents.forEach { document ->
            val isRead = document.getBoolean("isRead") ?: false
            if (!isRead) {
                batch.update(document.reference, "isRead", true)
            }
        }

        batch.set(
            usersCollection.document(userId),
            mapOf("lastNotificationsOpenedAt" to now),
            com.google.firebase.firestore.SetOptions.merge()
        )

        batch.commit().await()
    }

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

    override suspend fun deleteNotification(notificationId: String) {
        notificationsCollection
            .document(notificationId)
            .delete()
            .await()
    }

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

    private fun AppNotification.markReadUsingLastOpenedAt(lastOpenedAt: Long): AppNotification {
        return if (!isRead && createdAt <= lastOpenedAt) {
            copy(isRead = true)
        } else {
            this
        }
    }
}