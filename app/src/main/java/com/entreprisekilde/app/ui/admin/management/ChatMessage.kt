package com.entreprisekilde.app.ui.admin.management

data class ChatMessage(
    val id: Int,
    val threadId: Int,
    val text: String,
    val isFromMe: Boolean,
    val time: String = ""
)