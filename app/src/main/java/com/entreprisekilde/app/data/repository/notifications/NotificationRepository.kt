package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType

object NotificationRepository {

    private var nextId = 1

    private val notifications = mutableListOf<AppNotification>()

    suspend fun getNotifications(): List<AppNotification> {
        return notifications.toList()
    }

    suspend fun addMessageNotification(senderName: String, threadId: Int) {
        notifications.add(
            0,
            AppNotification(
                id = nextId++,
                title = "New message",
                message = "$senderName sent you a message",
                type = NotificationType.MESSAGE,
                createdAt = "Now",
                isRead = false,
                relatedThreadId = threadId
            )
        )
    }

    suspend fun addTaskAssignedNotification(taskName: String, assignedTo: String) {
        notifications.add(
            0,
            AppNotification(
                id = nextId++,
                title = "Task assigned",
                message = "You assigned \"$taskName\" to $assignedTo",
                type = NotificationType.TASK_ASSIGNED,
                createdAt = "Now",
                isRead = false,
                relatedThreadId = null
            )
        )
    }

    suspend fun markAsRead(notificationId: Int) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
        }
    }

    suspend fun markAllAsRead() {
        notifications.indices.forEach { index ->
            val item = notifications[index]
            if (!item.isRead) {
                notifications[index] = item.copy(isRead = true)
            }
        }
    }

    suspend fun unreadCount(): Int {
        return notifications.count { !it.isRead }
    }

    suspend fun deleteNotification(notificationId: Int) {
        notifications.removeAll { it.id == notificationId }
    }
}