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

/**
 * ViewModel responsible for handling all notification-related logic.
 *
 * This includes:
 * - Listening to real-time updates from the backend
 * - Managing UI state (notifications list + unread count)
 * - Syncing read/unread status between UI and backend
 *
 * Important: This ViewModel is designed to keep the UI responsive by updating
 * local state immediately, while syncing changes asynchronously with the backend.
 */
class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    /**
     * Observable list of notifications used directly by the UI.
     * Uses Compose state so UI recomposes automatically on updates.
     */
    val notifications = mutableStateListOf<AppNotification>()

    /**
     * Number of unread notifications (capped at 9 for UI display purposes).
     */
    var unreadCount by mutableIntStateOf(0)
        private set

    /**
     * Currently active user (used to scope notifications).
     */
    private var activeUserId: String? = null

    /**
     * Tracks whether the notifications screen is currently open.
     * This affects how we calculate unread count and mark notifications.
     */
    private var isNotificationsScreenOpen = false

    /**
     * Starts listening for real-time notifications for a given user.
     *
     * - Prevents duplicate listeners if already listening to the same user
     * - Clears previous listener before attaching a new one
     * - Sorts notifications by newest first
     * - If screen is open → auto-mark all as read locally
     */
    fun startListeningForUser(userId: String) {
        if (activeUserId == userId) return

        activeUserId = userId

        // Always clean up previous listener before attaching a new one
        repository.removeNotificationListener()

        repository.observeNotifications(
            userId = userId,
            onChanged = { updatedNotifications ->

                // Sort newest → oldest
                val sortedNotifications = updatedNotifications
                    .sortedByDescending { it.createdAt }

                notifications.clear()

                notifications.addAll(
                    if (isNotificationsScreenOpen) {
                        // If user is currently viewing notifications,
                        // we treat everything as read immediately in UI
                        sortedNotifications.map { it.copy(isRead = true) }
                    } else {
                        sortedNotifications
                    }
                )

                recalculateUnreadCount()
            },
            onError = {
                // Intentionally ignored for now
                // (could log or show UI error later)
            }
        )
    }

    /**
     * Stops listening to notifications and resets all local state.
     *
     * Important when:
     * - User logs out
     * - Switching users
     * - Cleaning up ViewModel
     */
    fun stopListening() {
        activeUserId = null
        isNotificationsScreenOpen = false

        repository.removeNotificationListener()

        notifications.clear()
        unreadCount = 0
    }

    /**
     * Updates whether the notifications screen is open or not.
     *
     * Behavior:
     * - If opened → mark everything as read
     * - If closed → recalculate unread count
     */
    fun setNotificationsScreenOpen(isOpen: Boolean) {
        isNotificationsScreenOpen = isOpen

        if (isOpen) {
            markAllAsRead()
        } else {
            recalculateUnreadCount()
        }
    }

    /**
     * Called when user explicitly opens the notifications screen.
     *
     * This aggressively:
     * - Marks everything as read locally (instant UI feedback)
     * - Forces backend sync EVERY time (no assumptions)
     *
     * Note: This is intentionally redundant-safe.
     */
    fun onNotificationsOpened() {
        val userId = activeUserId ?: return

        isNotificationsScreenOpen = true

        // 🔥 Force immediate UI update (no waiting for backend)
        if (notifications.isNotEmpty()) {
            val updated = notifications.map { it.copy(isRead = true) }
            notifications.clear()
            notifications.addAll(updated)
        }

        unreadCount = 0

        // 🔥 Always sync with backend (even if already read)
        viewModelScope.launch {
            runCatching {
                repository.markAllAsRead(userId)
            }
        }
    }

    /**
     * Marks a single notification as read.
     *
     * - Updates UI instantly
     * - Then syncs with backend in background
     */
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

    /**
     * Marks all notifications as read.
     *
     * Used when:
     * - Opening notifications screen
     * - Bulk actions
     */
    fun markAllAsRead(onDone: () -> Unit = {}) {

        // Update UI immediately
        if (notifications.isNotEmpty()) {
            val updated = notifications.map { it.copy(isRead = true) }
            notifications.clear()
            notifications.addAll(updated)
        }

        unreadCount = 0

        val userId = activeUserId ?: return

        // Sync with backend
        viewModelScope.launch {
            runCatching {
                repository.markAllAsRead(userId)
            }
            onDone()
        }
    }

    /**
     * Deletes a notification.
     *
     * - Removes it from UI immediately
     * - Then deletes from backend
     */
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

    /**
     * Recalculates unread count based on current state.
     *
     * Rules:
     * - If screen is open → always 0
     * - Otherwise → count unread (max 9 for UI badge)
     */
    private fun recalculateUnreadCount() {
        unreadCount = if (isNotificationsScreenOpen) {
            0
        } else {
            notifications.count { !it.isRead }
                .coerceAtMost(9) // Prevents UI overflow (e.g. "99+")
        }
    }

    /**
     * Called when ViewModel is destroyed.
     *
     * Ensures we clean up any active listeners to avoid memory leaks.
     */
    override fun onCleared() {
        repository.removeNotificationListener()
        super.onCleared()
    }
}