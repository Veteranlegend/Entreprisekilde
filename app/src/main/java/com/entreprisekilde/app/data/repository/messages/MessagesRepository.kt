package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread

interface MessagesRepository {

    suspend fun getThreads(): List<MessageThread>

    suspend fun getMessages(threadId: Int): List<ChatMessage>

    fun startMessagesListener(
        threadId: Int,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit = {}
    ): () -> Unit

    suspend fun deleteThread(threadId: Int)

    suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    )

    suspend fun findThreadById(threadId: Int): MessageThread?

    suspend fun createOrGetThread(
        recipientId: String,
        recipientName: String
    ): MessageThread
}