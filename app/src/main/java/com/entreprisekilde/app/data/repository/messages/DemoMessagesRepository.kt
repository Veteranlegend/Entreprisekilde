package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread

class DemoMessagesRepository : MessagesRepository {

    private val messageThreads = DemoSeedData.createMessageThreads().toMutableList()
    private val chatMessages = DemoSeedData.createChatMessages()
        .mapValues { (_, messages) -> messages.toMutableList() }
        .toMutableMap()

    override suspend fun getThreads(): List<MessageThread> {
        return messageThreads.toList()
    }

    override suspend fun getMessages(threadId: Int): List<ChatMessage> {
        return chatMessages[threadId]?.toList() ?: emptyList()
    }

    override fun startMessagesListener(
        threadId: Int,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        onUpdate(chatMessages[threadId]?.toList() ?: emptyList())
        return {}
    }

    override suspend fun deleteThread(threadId: Int) {
        messageThreads.removeAll { it.id == threadId }
        chatMessages.remove(threadId)
    }

    override suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    ) {
        val messages = chatMessages.getOrPut(threadId) { mutableListOf() }

        val newMessageId = (chatMessages.values.flatten().maxOfOrNull { it.id } ?: 0) + 1
        val newMessage = ChatMessage(
            id = newMessageId,
            threadId = threadId,
            senderId = senderId,
            text = text,
            time = currentTime()
        )

        messages.add(newMessage)

        val threadIndex = messageThreads.indexOfFirst { it.id == threadId }
        if (threadIndex != -1) {
            val oldThread = messageThreads[threadIndex]
            messageThreads[threadIndex] = oldThread.copy(
                lastMessage = text,
                unreadCount = 0
            )
        }
    }

    override suspend fun findThreadById(threadId: Int): MessageThread? {
        return messageThreads.firstOrNull { it.id == threadId }
    }

    override suspend fun createOrGetThread(
        recipientId: String,
        recipientName: String
    ): MessageThread {
        val existingThread = messageThreads.firstOrNull { it.recipientId == recipientId }
        if (existingThread != null) {
            return existingThread
        }

        val newThreadId = (messageThreads.maxOfOrNull { it.id } ?: 0) + 1

        val newThread = MessageThread(
            id = newThreadId,
            recipientId = recipientId,
            recipientName = recipientName,
            lastMessage = "",
            unreadCount = 0
        )

        messageThreads.add(newThread)
        chatMessages[newThreadId] = mutableListOf()

        return newThread
    }

    private fun currentTime(): String {
        val now = java.time.LocalTime.now()
        return now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
}