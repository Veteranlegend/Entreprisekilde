package com.entreprisekilde.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.repository.notifications.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val notifications = mutableStateListOf<AppNotification>()

    var unreadCount by mutableIntStateOf(0)
        private set

    private var activeUserId: String? = null
    private var isNotificationsScreenOpen = false
    private var lastSeenTimestamp = 0L

    fun startListeningForUser(userId: String) {
        if (activeUserId == userId) return

        activeUserId = userId
        repository.removeNotificationListener()

        repository.observeNotifications(
            userId = userId,
            onChanged = { updatedNotifications ->
                val sortedNotifications = updatedNotifications
                    .sortedByDescending { it.createdAt }

                notifications.clear()
                notifications.addAll(sortedNotifications)

                unreadCount = if (isNotificationsScreenOpen) {
                    0
                } else {
                    sortedNotifications.count { it.createdAt > lastSeenTimestamp }
                        .coerceAtMost(9)
                }
            },
            onError = {
                // optional later
            }
        )
    }

    fun stopListening() {
        activeUserId = null
        isNotificationsScreenOpen = false
        repository.removeNotificationListener()
        notifications.clear()
        unreadCount = 0
    }

    fun setNotificationsScreenOpen(isOpen: Boolean) {
        isNotificationsScreenOpen = isOpen

        if (isOpen) {
            lastSeenTimestamp = System.currentTimeMillis()
            unreadCount = 0

            val userId = activeUserId ?: return
            viewModelScope.launch {
                runCatching {
                    repository.markAllAsRead(userId)
                }
            }
        } else {
            unreadCount = notifications.count { it.createdAt > lastSeenTimestamp }
                .coerceAtMost(9)
        }
    }

    fun onNotificationsOpened() {
        setNotificationsScreenOpen(true)
    }

    fun addMessageNotification(
        senderName: String,
        recipientUserId: String,
        threadId: Int,
        onDone: () -> Unit = {}
    ) {
        if (recipientUserId.isBlank()) {
            onDone()
            return
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
        if (assignedUserId.isBlank()) {
            onDone()
            return
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
        viewModelScope.launch {
            runCatching {
                repository.markAsRead(notificationId)
            }
            onDone()
        }
    }

    fun markAllAsRead(onDone: () -> Unit = {}) {
        lastSeenTimestamp = System.currentTimeMillis()
        unreadCount = 0

        val userId = activeUserId ?: return

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

        unreadCount = if (isNotificationsScreenOpen) {
            0
        } else {
            notifications.count { it.createdAt > lastSeenTimestamp }
                .coerceAtMost(9)
        }

        viewModelScope.launch {
            runCatching {
                repository.deleteNotification(notificationId)
            }
            onDone()
        }
    }

    override fun onCleared() {
        repository.removeNotificationListener()
        super.onCleared()
    }
}