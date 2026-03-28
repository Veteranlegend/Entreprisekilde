package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification

interface NotificationRepository {

    suspend fun getNotifications(): List<AppNotification>

    suspend fun addMessageNotification(senderName: String, threadId: Int)

    suspend fun addTaskAssignedNotification(taskName: String, assignedTo: String)

    suspend fun markAsRead(notificationId: Int)

    suspend fun markAllAsRead()

    suspend fun unreadCount(): Int

    suspend fun deleteNotification(notificationId: Int)
}