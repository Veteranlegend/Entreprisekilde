package com.entreprisekilde.app.data.model.messages

data class ChatMessage(
    val id: String = "",
    val threadId: Int = 0,
    val senderId: String = "",
    val text: String = "",
    val time: String = "",
    val createdAt: Long = 0L,
    val readByUserIds: List<String> = emptyList()
)