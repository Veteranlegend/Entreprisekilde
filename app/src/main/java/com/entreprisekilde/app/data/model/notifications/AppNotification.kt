package com.entreprisekilde.app.data.model.notifications

/**
 * Represents a notification shown to a specific user.
 *
 * Notifications are stored in Firestore and used to:
 * - display a list of notifications in the UI
 * - show unread badge counts
 * - optionally navigate the user to a related screen (e.g. chat)
 */
data class AppNotification(

    // Unique ID of the notification (Firestore document ID)
    val id: String = "",

    // Short title shown in the notification list
    val title: String = "",

    // Detailed message describing the notification
    val message: String = "",

    // Type of notification (used to determine behavior/navigation)
    val type: NotificationType = NotificationType.MESSAGE,

    // Timestamp used for sorting (most recent notifications first)
    val createdAt: Long = 0L,

    // Indicates whether the user has read this notification
    // Used for badge count and UI styling (e.g. bold vs normal)
    val isRead: Boolean = false,

    // Optional reference to a related chat thread
    // Used when clicking the notification to navigate to the correct conversation
    val relatedThreadId: Int? = null,

    // ID of the user this notification belongs to
    // Ensures notifications are scoped per user in Firestore
    val userId: String = ""
)

/**
 * Defines the types of notifications supported in the app.
 *
 * This allows the system to:
 * - trigger different behaviors depending on type
 * - extend easily in the future (e.g. add new notification types)
 */
enum class NotificationType {

    // Notification triggered when a new message is received
    MESSAGE,

    // Notification triggered when a task is assigned to a user
    TASK_ASSIGNED
}