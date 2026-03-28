package com.entreprisekilde.app.ui.notifications

import androidx.lifecycle.ViewModel

class NotificationViewModel : ViewModel() {

    val notifications = NotificationRepository.notifications

    fun unreadCount(): Int {
        return NotificationRepository.unreadCount()
    }

    fun addMessageNotification(senderName: String, threadId: Int) {
        NotificationRepository.addMessageNotification(
            senderName = senderName,
            threadId = threadId
        )
    }

    fun addTaskAssignedNotification(taskName: String, assignedTo: String) {
        NotificationRepository.addTaskAssignedNotification(
            taskName = taskName,
            assignedTo = assignedTo
        )
    }

    fun markAsRead(notificationId: Int) {
        NotificationRepository.markAsRead(notificationId)
    }

    fun markAllAsRead() {
        NotificationRepository.markAllAsRead()
    }

    fun deleteNotification(notificationId: Int) {
        NotificationRepository.deleteNotification(notificationId)
    }
}