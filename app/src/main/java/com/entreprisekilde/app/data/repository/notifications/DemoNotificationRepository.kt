package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType

/**
 * Simple in-memory demo implementation of [NotificationRepository].
 *
 * This repository is useful for development, previews, local testing,
 * or any temporary setup where we do not want to connect to a real backend yet.
 *
 * Important note:
 * - Data only lives in memory while the app process is alive.
 * - Everything is lost when the app restarts.
 * - Only a single listener is supported in this implementation.
 */
class DemoNotificationRepository : NotificationRepository {

    /**
     * Incrementing ID counter used to generate unique notification IDs.
     *
     * Since this is just a demo repository, a simple integer counter is enough.
     */
    private var nextId = 1

    /**
     * Internal in-memory storage for all notifications.
     *
     * We keep notifications for every user in the same list and filter by userId
     * when returning or observing data.
     */
    private val notifications = mutableListOf<AppNotification>()

    /**
     * Single active listener for notification updates.
     *
     * This demo version keeps things simple and supports one observer at a time.
     * A real implementation would likely support multiple listeners or rely on
     * Flow / LiveData / backend snapshot listeners.
     */
    private var listener: ((List<AppNotification>) -> Unit)? = null

    /**
     * Returns all notifications for the given user.
     */
    override suspend fun getNotifications(userId: String): List<AppNotification> {
        return notifications.filter { it.userId == userId }
    }

    /**
     * Starts observing notifications for a specific user.
     *
     * We store a wrapper listener that receives the full internal list and then
     * filters it down before sending it to the caller. That way the repository
     * can keep a single source of truth internally.
     *
     * We also immediately emit the current state so the UI has data right away
     * instead of waiting for the next update.
     */
    override fun observeNotifications(
        userId: String,
        onChanged: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        listener = { allNotifications ->
            onChanged(allNotifications.filter { it.userId == userId })
        }

        // Push the current notifications immediately so the observer starts
        // with the latest known state.
        listener?.invoke(notifications.toList())
    }

    /**
     * Stops notification observation by clearing the active listener.
     */
    override fun removeNotificationListener() {
        listener = null
    }

    /**
     * Creates and stores a "new message" notification for the recipient.
     *
     * The notification is inserted at index 0 so the newest notification appears
     * first in the list.
     */
    override suspend fun addMessageNotification(
        senderName: String,
        recipientUserId: String,
        threadId: Int
    ) {
        notifications.add(
            0,
            AppNotification(
                id = (nextId++).toString(),
                userId = recipientUserId,
                title = "New message",
                message = "$senderName sent you a message",
                type = NotificationType.MESSAGE,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                relatedThreadId = threadId
            )
        )

        // Notify the active observer that the list has changed.
        notifyListener()
    }

    /**
     * Creates and stores a "task assigned" notification for the assigned user.
     *
     * Even though [assignedToName] is passed in, it is not currently used in the
     * notification text. It may be kept for consistency with other repository
     * implementations or future message customization.
     */
    override suspend fun addTaskAssignedNotification(
        taskName: String,
        assignedUserId: String,
        assignedToName: String
    ) {
        notifications.add(
            0,
            AppNotification(
                id = (nextId++).toString(),
                userId = assignedUserId,
                title = "Task assigned",
                message = "You were assigned \"$taskName\"",
                type = NotificationType.TASK_ASSIGNED,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                relatedThreadId = null
            )
        )

        // Notify the observer so the UI updates immediately.
        notifyListener()
    }

    /**
     * Marks a single notification as read.
     *
     * Because [AppNotification] is likely a data class and therefore immutable
     * by design, we replace the existing item with a copied version instead of
     * mutating fields directly.
     */
    override suspend fun markAsRead(notificationId: String) {
        val index = notifications.indexOfFirst { it.id == notificationId }

        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            notifyListener()
        }
    }

    /**
     * Marks every unread notification for the given user as read.
     *
     * We loop through the whole list because this in-memory repository stores
     * notifications for all users together.
     */
    override suspend fun markAllAsRead(userId: String) {
        notifications.indices.forEach { index ->
            val item = notifications[index]

            if (item.userId == userId && !item.isRead) {
                notifications[index] = item.copy(isRead = true)
            }
        }

        notifyListener()
    }

    /**
     * Returns the number of unread notifications for the given user.
     */
    override suspend fun unreadCount(userId: String): Int {
        return notifications.count { it.userId == userId && !it.isRead }
    }

    /**
     * Deletes a notification by ID.
     *
     * [removeAll] is used here even though IDs are expected to be unique.
     * That makes the method a little more defensive in case duplicate IDs
     * ever appear during testing.
     */
    override suspend fun deleteNotification(notificationId: String) {
        notifications.removeAll { it.id == notificationId }
        notifyListener()
    }

    /**
     * Sends the latest notification list to the active listener, if one exists.
     *
     * We pass a copied list using [toList] so external consumers cannot
     * accidentally modify the repository's internal mutable list.
     */
    private fun notifyListener() {
        listener?.invoke(notifications.toList())
    }
}