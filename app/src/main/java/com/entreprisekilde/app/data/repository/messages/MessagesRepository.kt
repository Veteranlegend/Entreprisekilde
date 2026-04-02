package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread

interface MessagesRepository {

    suspend fun getThreadsForUser(userId: String): List<MessageThread>

    fun startThreadsListener(
        userId: String,
        onUpdate: (List<MessageThread>) -> Unit,
        onError: (String) -> Unit = {}
    ): () -> Unit

    suspend fun getMessages(threadId: Int): List<ChatMessage>

    fun startMessagesListener(
        threadId: Int,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit = {}
    ): () -> Unit

    suspend fun markThreadAsRead(
        threadId: Int,
        userId: String
    )

    suspend fun markMessagesAsRead(
        threadId: Int,
        userId: String
    )

    suspend fun setTypingState(
        threadId: Int,
        userId: String,
        isTyping: Boolean
    )

    suspend fun deleteThread(
        threadId: Int,
        currentUserId: String
    )

    suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    )

    suspend fun findThreadById(
        threadId: Int,
        currentUserId: String
    ): MessageThread?

    suspend fun createOrGetThread(
        currentUserId: String,
        currentUserName: String,
        recipientId: String,
        recipientName: String
    ): MessageThread
}