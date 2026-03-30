package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification

interface NotificationRepository {

    suspend fun getNotifications(userId: String): List<AppNotification>

    fun observeNotifications(
        userId: String,
        onChanged: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit = {}
    )

    fun removeNotificationListener()

    suspend fun addMessageNotification(
        senderName: String,
        recipientUserId: String,
        threadId: Int
    )

    suspend fun addTaskAssignedNotification(
        taskName: String,
        assignedUserId: String,
        assignedToName: String
    )

    suspend fun markAsRead(notificationId: String)

    suspend fun markAllAsRead(userId: String)

    suspend fun unreadCount(userId: String): Int

    suspend fun deleteNotification(notificationId: String)
}