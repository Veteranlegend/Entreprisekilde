package com.entreprisekilde.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.entreprisekilde.app.data.repository.notifications.NotificationRepository
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val notifications = mutableStateListOf<AppNotification>()

    var unreadCount by mutableIntStateOf(0)
        private set

    private var activeUserId: String? = null

    fun startListeningForUser(userId: String) {
        if (activeUserId == userId) return

        activeUserId = userId
        repository.removeNotificationListener()

        repository.observeNotifications(
            userId = userId,
            onChanged = { updatedNotifications ->
                viewModelScope.launch {
                    notifications.clear()
                    notifications.addAll(updatedNotifications)
                    updateUnreadCount()
                }
            },
            onError = {
                // optional UI error later
            }
        )
    }

    fun stopListening() {
        activeUserId = null
        repository.removeNotificationListener()
        notifications.clear()
        updateUnreadCount()
    }

    fun onNotificationsOpened() {
        val userId = activeUserId ?: return

        var changed = false
        notifications.indices.forEach { index ->
            val item = notifications[index]
            if (!item.isRead) {
                notifications[index] = item.copy(isRead = true)
                changed = true
            }
        }

        if (changed) {
            updateUnreadCount()
        }

        viewModelScope.launch {
            runCatching {
                repository.markAllAsRead(userId)
            }
        }
    }

    fun addMessageNotification(
        senderName: String,
        recipientUserId: String,
        threadId: Int,
        onDone: () -> Unit = {}
    ) {
        if (recipientUserId == activeUserId) {
            notifications.add(
                0,
                AppNotification(
                    id = "local-${UUID.randomUUID()}",
                    userId = recipientUserId,
                    title = "New message",
                    message = "$senderName sent you a message",
                    type = NotificationType.MESSAGE,
                    createdAt = System.currentTimeMillis(),
                    isRead = false,
                    relatedThreadId = threadId
                )
            )
            updateUnreadCount()
        }

        viewModelScope.launch {
            runCatching {
                repository.addMessageNotification(
                    senderName = senderName,
                    recipientUserId = recipientUserId,
                    threadId = threadId
                )
            }
            onDone()
        }
    }

    fun addTaskAssignedNotification(
        taskName: String,
        assignedUserId: String,
        assignedToName: String,
        onDone: () -> Unit = {}
    ) {
        if (assignedUserId == activeUserId) {
            notifications.add(
                0,
                AppNotification(
                    id = "local-${UUID.randomUUID()}",
                    userId = assignedUserId,
                    title = "Task assigned",
                    message = "You were assigned \"$taskName\"",
                    type = NotificationType.TASK_ASSIGNED,
                    createdAt = System.currentTimeMillis(),
                    isRead = false,
                    relatedThreadId = null
                )
            )
            updateUnreadCount()
        }

        viewModelScope.launch {
            runCatching {
                repository.addTaskAssignedNotification(
                    taskName = taskName,
                    assignedUserId = assignedUserId,
                    assignedToName = assignedToName
                )
            }
            onDone()
        }
    }

    fun markAsRead(
        notificationId: String,
        onDone: () -> Unit = {}
    ) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1 && !notifications[index].isRead) {
            notifications[index] = notifications[index].copy(isRead = true)
            updateUnreadCount()
        }

        viewModelScope.launch {
            runCatching {
                if (!notificationId.startsWith("local-")) {
                    repository.markAsRead(notificationId)
                }
            }
            onDone()
        }
    }

    fun markAllAsRead(onDone: () -> Unit = {}) {
        val userId = activeUserId ?: return

        var changed = false
        notifications.indices.forEach { index ->
            val item = notifications[index]
            if (!item.isRead) {
                notifications[index] = item.copy(isRead = true)
                changed = true
            }
        }

        if (changed) {
            updateUnreadCount()
        }

        viewModelScope.launch {
            runCatching {
                repository.markAllAsRead(userId)
            }
            onDone()
        }
    }

    fun deleteNotification(
        notificationId: String,
        onDone: () -> Unit = {}
    ) {
        notifications.removeAll { it.id == notificationId }
        updateUnreadCount()

        viewModelScope.launch {
            runCatching {
                if (!notificationId.startsWith("local-")) {
                    repository.deleteNotification(notificationId)
                }
            }
            onDone()
        }
    }

    private fun updateUnreadCount() {
        unreadCount = notifications.count { !it.isRead }
    }

    override fun onCleared() {
        repository.removeNotificationListener()
        super.onCleared()
    }
}