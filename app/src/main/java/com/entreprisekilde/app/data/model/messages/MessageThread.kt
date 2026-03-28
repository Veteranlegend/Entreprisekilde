package com.entreprisekilde.app.data.model.messages

data class MessageThread(
    val id: Int,
    val name: String,
    val lastMessage: String,
    val unreadCount: Int = 0
)