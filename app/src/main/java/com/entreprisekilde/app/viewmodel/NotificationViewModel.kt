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

    fun startListeningForUser(userId: String) {
        if (activeUserId == userId) return

        activeUserId = userId
        repository.removeNotificationListener()

        repository.observeNotifications(
            userId = userId,
            onChanged = { updatedNotifications ->
                val sortedNotifications = updatedNotifications.sortedByDescending { it.createdAt }

                notifications.clear()
                notifications.addAll(
                    if (isNotificationsScreenOpen) {
                        sortedNotifications.map { it.copy(isRead = true) }
                    } else {
                        sortedNotifications
                    }
                )

                recalculateUnreadCount()
            },
            onError = {}
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
            markAllAsRead()
        } else {
            recalculateUnreadCount()
        }
    }

    fun onNotificationsOpened() {
        val userId = activeUserId ?: return

        isNotificationsScreenOpen = true

        // 🔥 FORCE local UI immediately
        if (notifications.isNotEmpty()) {
            val updated = notifications.map { it.copy(isRead = true) }
            notifications.clear()
            notifications.addAll(updated)
        }

        unreadCount = 0

        // 🔥 FORCE backend update EVERY time
        viewModelScope.launch {
            runCatching {
                repository.markAllAsRead(userId)
            }
        }
    }

    fun markAsRead(
        notificationId: String,
        onDone: () -> Unit = {}
    ) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
        }
        recalculateUnreadCount()

        viewModelScope.launch {
            runCatching {
                repository.markAsRead(notificationId)
            }
            onDone()
        }
    }

    fun markAllAsRead(onDone: () -> Unit = {}) {
        if (notifications.isNotEmpty()) {
            val updated = notifications.map { it.copy(isRead = true) }
            notifications.clear()
            notifications.addAll(updated)
        }

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
        recalculateUnreadCount()

        viewModelScope.launch {
            runCatching {
                repository.deleteNotification(notificationId)
            }
            onDone()
        }
    }

    private fun recalculateUnreadCount() {
        unreadCount = if (isNotificationsScreenOpen) {
            0
        } else {
            notifications.count { !it.isRead }.coerceAtMost(9)
        }
    }

    override fun onCleared() {
        repository.removeNotificationListener()
        super.onCleared()
    }
}