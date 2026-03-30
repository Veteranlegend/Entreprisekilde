package com.entreprisekilde.app.data.model.notifications

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.MESSAGE,
    val createdAt: Long = 0L,
    val isRead: Boolean = false,
    val relatedThreadId: Int? = null,
    val userId: String = ""
)

enum class NotificationType {
    MESSAGE,
    TASK_ASSIGNED
}