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
    private var listenerRegistration: ListenerRegistration? = null

    override suspend fun getNotifications(userId: String): List<AppNotification> {
        return notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(AppNotification::class.java)?.copy(id = document.id)
            }
            .reversed()
    }

    override fun observeNotifications(
        userId: String,
        onChanged: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        removeNotificationListener()

        listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents
                        .mapNotNull { document ->
                            document.toObject(AppNotification::class.java)?.copy(id = document.id)
                        }
                        .reversed()

                    onChanged(notifications)
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
        val snapshot = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        if (snapshot.isEmpty) return

        val batch = firestore.batch()

        snapshot.documents.forEach { document ->
            batch.update(document.reference, "isRead", true)
        }

        batch.commit().await()
    }

    override suspend fun unreadCount(userId: String): Int {
        return notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()
            .size()
    }

    override suspend fun deleteNotification(notificationId: String) {
        notificationsCollection
            .document(notificationId)
            .delete()
            .await()
    }
}