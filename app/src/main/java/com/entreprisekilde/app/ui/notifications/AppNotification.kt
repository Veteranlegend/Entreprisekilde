package com.entreprisekilde.app.ui.notifications

data class AppNotification(
    val id: Int,
    val title: String,
    val message: String,
    val type: NotificationType,
    val createdAt: String,
    val isRead: Boolean = false,
    val relatedThreadId: Int? = null
)

enum class NotificationType {
    MESSAGE,
    TASK_ASSIGNED
}