package com.entreprisekilde.app.ui.notifications

import androidx.compose.runtime.mutableStateListOf

object NotificationRepository {

    private var nextId = 1

    val notifications = mutableStateListOf<AppNotification>()

    fun addMessageNotification(senderName: String) {
        notifications.add(
            0,
            AppNotification(
                id = nextId++,
                title = "New message",
                message = "$senderName sent you a message",
                type = NotificationType.MESSAGE,
                createdAt = "Now",
                isRead = false
            )
        )
    }

    fun addTaskAssignedNotification(taskName: String, assignedTo: String) {
        notifications.add(
            0,
            AppNotification(
                id = nextId++,
                title = "Task assigned",
                message = "You assigned \"$taskName\" to $assignedTo",
                type = NotificationType.TASK_ASSIGNED,
                createdAt = "Now",
                isRead = false
            )
        )
    }

    fun markAsRead(notificationId: Int) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
        }
    }

    fun markAllAsRead() {
        notifications.indices.forEach { index ->
            val item = notifications[index]
            if (!item.isRead) {
                notifications[index] = item.copy(isRead = true)
            }
        }
    }

    fun unreadCount(): Int {
        return notifications.count { !it.isRead }
    }

    fun deleteNotification(notificationId: Int) {
        notifications.removeAll { it.id == notificationId }
    }
}