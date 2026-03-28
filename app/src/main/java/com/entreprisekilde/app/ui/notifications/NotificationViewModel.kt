package com.entreprisekilde.app.ui.notifications

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.repository.notifications.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val notifications = mutableStateListOf<AppNotification>()

    init {
        loadNotifications()
    }

    fun unreadCount(): Int {
        return notifications.count { !it.isRead }
    }

    fun addMessageNotification(senderName: String, threadId: Int) {
        viewModelScope.launch {
            repository.addMessageNotification(
                senderName = senderName,
                threadId = threadId
            )
            refreshNotifications()
        }
    }

    fun addTaskAssignedNotification(taskName: String, assignedTo: String) {
        viewModelScope.launch {
            repository.addTaskAssignedNotification(
                taskName = taskName,
                assignedTo = assignedTo
            )
            refreshNotifications()
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
            refreshNotifications()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
            refreshNotifications()
        }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
            refreshNotifications()
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notifications.clear()
            notifications.addAll(repository.getNotifications())
        }
    }

    private suspend fun refreshNotifications() {
        notifications.clear()
        notifications.addAll(repository.getNotifications())
    }
}