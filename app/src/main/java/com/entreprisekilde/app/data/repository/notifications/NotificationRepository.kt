package com.entreprisekilde.app.data.repository.notifications

import com.entreprisekilde.app.data.model.notifications.AppNotification

/**
 * Contract for all notification-related operations in the app.
 *
 * This interface defines how the rest of the application interacts with
 * notifications, without caring about where they come from (e.g. in-memory,
 * Firebase, REST API, local database, etc.).
 *
 * Any implementation (like DemoNotificationRepository or a real backend version)
 * must follow this contract.
 */
interface NotificationRepository {

    /**
     * Fetches all notifications for a specific user.
     *
     * Typically used when initially loading a notifications screen.
     *
     * @param userId The ID of the user whose notifications should be returned.
     * @return A list of notifications belonging to that user.
     */
    suspend fun getNotifications(userId: String): List<AppNotification>

    /**
     * Starts observing notifications for a specific user.
     *
     * This is used when the UI should react in real-time to changes
     * (e.g. new notifications, read status updates, deletions).
     *
     * Implementations are expected to:
     * - Continuously push updates via [onChanged]
     * - Handle errors via [onError]
     *
     * @param userId The user whose notifications should be observed.
     * @param onChanged Called whenever the notification list changes.
     * @param onError Called if something goes wrong during observation.
     */
    fun observeNotifications(
        userId: String,
        onChanged: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit = {}
    )

    /**
     * Stops any active notification listener.
     *
     * Important to call when a screen/view model is cleared to avoid
     * memory leaks or unnecessary updates.
     */
    fun removeNotificationListener()

    /**
     * Creates a notification when a user receives a new message.
     *
     * @param senderName Name of the user who sent the message.
     * @param recipientUserId ID of the user receiving the notification.
     * @param threadId The thread related to the message (used for navigation).
     */
    suspend fun addMessageNotification(
        senderName: String,
        recipientUserId: String,
        threadId: Int
    )

    /**
     * Creates a notification when a task is assigned to a user.
     *
     * @param taskName Name of the assigned task.
     * @param assignedUserId ID of the user receiving the task.
     * @param assignedToName Name of the person assigning the task.
     */
    suspend fun addTaskAssignedNotification(
        taskName: String,
        assignedUserId: String,
        assignedToName: String
    )

    /**
     * Marks a specific notification as read.
     *
     * @param notificationId The ID of the notification to update.
     */
    suspend fun markAsRead(notificationId: String)

    /**
     * Marks all notifications as read for a given user.
     *
     * Typically used when opening the notifications screen.
     *
     * @param userId The user whose notifications should be marked as read.
     */
    suspend fun markAllAsRead(userId: String)

    /**
     * Returns the number of unread notifications for a user.
     *
     * Useful for badges, indicators, or notification counters in the UI.
     *
     * @param userId The user to check.
     * @return Number of unread notifications.
     */
    suspend fun unreadCount(userId: String): Int

    /**
     * Deletes a notification by its ID.
     *
     * @param notificationId The notification to remove.
     */
    suspend fun deleteNotification(notificationId: String)
}