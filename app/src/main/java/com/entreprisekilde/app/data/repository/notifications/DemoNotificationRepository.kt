package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType

class DemoNotificationRepository : NotificationRepository {

    private var nextId = 1
    private val notifications = mutableListOf<AppNotification>()
    private var listener: ((List<AppNotification>) -> Unit)? = null

    override suspend fun getNotifications(userId: String): List<AppNotification> {
        return notifications.filter { it.userId == userId }
    }

    override fun observeNotifications(
        userId: String,
        onChanged: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        listener = { allNotifications ->
            onChanged(allNotifications.filter { it.userId == userId })
        }
        listener?.invoke(notifications.toList())
    }

    override fun removeNotificationListener() {
        listener = null
    }

    override suspend fun addMessageNotification(
        senderName: String,
        recipientUserId: String,
        threadId: Int
    ) {
        notifications.add(
            0,
            AppNotification(
                id = (nextId++).toString(),
                userId = recipientUserId,
                title = "New message",
                message = "$senderName sent you a message",
                type = NotificationType.MESSAGE,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                relatedThreadId = threadId
            )
        )
        notifyListener()
    }

    override suspend fun addTaskAssignedNotification(
        taskName: String,
        assignedUserId: String,
        assignedToName: String
    ) {
        notifications.add(
            0,
            AppNotification(
                id = (nextId++).toString(),
                userId = assignedUserId,
                title = "Task assigned",
                message = "You were assigned \"$taskName\"",
                type = NotificationType.TASK_ASSIGNED,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                relatedThreadId = null
            )
        )
        notifyListener()
    }

    override suspend fun markAsRead(notificationId: String) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            notifyListener()
        }
    }

    override suspend fun markAllAsRead(userId: String) {
        notifications.indices.forEach { index ->
            val item = notifications[index]
            if (item.userId == userId && !item.isRead) {
                notifications[index] = item.copy(isRead = true)
            }
        }
        notifyListener()
    }

    override suspend fun unreadCount(userId: String): Int {
        return notifications.count { it.userId == userId && !it.isRead }
    }

    override suspend fun deleteNotification(notificationId: String) {
        notifications.removeAll { it.id == notificationId }
        notifyListener()
    }

    private fun notifyListener() {
        listener?.invoke(notifications.toList())
    }
}