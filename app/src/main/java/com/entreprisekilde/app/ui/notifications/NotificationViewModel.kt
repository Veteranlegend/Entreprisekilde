package com.entreprisekilde.app.ui.notifications

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.repository.notifications.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    val notifications = mutableStateListOf<AppNotification>()

    init {
        loadNotifications()
    }

    fun unreadCount(): Int {
        return notifications.count { !it.isRead }
    }

    fun addMessageNotification(senderName: String, threadId: Int) {
        viewModelScope.launch {
            NotificationRepository.addMessageNotification(
                senderName = senderName,
                threadId = threadId
            )
            refreshNotifications()
        }
    }

    fun addTaskAssignedNotification(taskName: String, assignedTo: String) {
        viewModelScope.launch {
            NotificationRepository.addTaskAssignedNotification(
                taskName = taskName,
                assignedTo = assignedTo
            )
            refreshNotifications()
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            NotificationRepository.markAsRead(notificationId)
            refreshNotifications()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            NotificationRepository.markAllAsRead()
            refreshNotifications()
        }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            NotificationRepository.deleteNotification(notificationId)
            refreshNotifications()
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notifications.clear()
            notifications.addAll(NotificationRepository.getNotifications())
        }
    }

    private suspend fun refreshNotifications() {
        notifications.clear()
        notifications.addAll(NotificationRepository.getNotifications())
    }
}