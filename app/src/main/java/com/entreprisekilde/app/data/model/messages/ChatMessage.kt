package com.entreprisekilde.app.data.model.messages

data class ChatMessage(
    val id: Int,
    val threadId: Int,
    val senderId: String,
    val text: String,
    val time: String = ""
)