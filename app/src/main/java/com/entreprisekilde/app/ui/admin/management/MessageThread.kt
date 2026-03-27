package com.entreprisekilde.app.ui.admin.management

data class MessageThread(
    val id: Int,
    val name: String,
    val lastMessage: String,
    val unreadCount: Int = 0
)