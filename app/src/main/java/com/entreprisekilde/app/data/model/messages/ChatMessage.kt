package com.entreprisekilde.app.data.model.messages

data class ChatMessage(
    val id: String = "",
    val threadId: Int = 0,
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val messageType: String = MESSAGE_TYPE_TEXT,
    val time: String = "",
    val createdAt: Long = 0L,
    val readByUserIds: List<String> = emptyList()
) {
    companion object {
        const val MESSAGE_TYPE_TEXT = "text"
        const val MESSAGE_TYPE_IMAGE = "image"
    }

    val isImageMessage: Boolean
        get() = messageType == MESSAGE_TYPE_IMAGE && imageUrl.isNotBlank()
}