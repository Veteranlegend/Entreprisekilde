
package com.entreprisekilde.app.data.model.messages

data class MessageThread(
    val id: Int,
    val recipientId: String,
    val recipientName: String,
    val lastMessage: String,
    val unreadCount: Int = 0
)


