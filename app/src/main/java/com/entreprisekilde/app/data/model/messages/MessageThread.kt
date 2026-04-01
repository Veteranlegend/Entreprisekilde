package com.entreprisekilde.app.data.model.messages

data class MessageThread(
    val id: Int = 0,
    val recipientId: String = "",
    val recipientName: String = "",
    val lastMessage: String = "",
    val unreadCount: Int = 0,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val unreadCountByUser: Map<String, Int> = emptyMap(),
    val updatedAt: Long = 0L,
    val lastMessageSenderId: String = "",
    val typingUserIds: List<String> = emptyList()
)