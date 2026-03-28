package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread

class MessagesRepository {

    private val messageThreads = DemoSeedData.createMessageThreads().toMutableList()
    private val chatMessages = DemoSeedData.createChatMessages()
        .mapValues { (_, messages) -> messages.toMutableList() }
        .toMutableMap()

    suspend fun getThreads(): List<MessageThread> {
        return messageThreads.toList()
    }

    suspend fun getMessages(threadId: Int): List<ChatMessage> {
        return chatMessages[threadId]?.toList() ?: emptyList()
    }

    suspend fun deleteThread(threadId: Int) {
        messageThreads.removeAll { it.id == threadId }
        chatMessages.remove(threadId)
    }

    suspend fun sendMessage(threadId: Int, text: String) {
        val messages = chatMessages.getOrPut(threadId) { mutableListOf() }

        val newMessageId = (chatMessages.values.flatten().maxOfOrNull { it.id } ?: 0) + 1
        val newMessage = ChatMessage(
            id = newMessageId,
            threadId = threadId,
            text = text,
            isFromMe = true,
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

    suspend fun findThreadById(threadId: Int): MessageThread? {
        return messageThreads.firstOrNull { it.id == threadId }
    }

    private fun currentTime(): String {
        val now = java.time.LocalTime.now()
        return now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
}