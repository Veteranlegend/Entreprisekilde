package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread

class DemoMessagesRepository : MessagesRepository {

    private val messageThreads = DemoSeedData.createMessageThreads().toMutableList()
    private val chatMessages = DemoSeedData.createChatMessages()
        .mapValues { (_, messages) -> messages.toMutableList() }
        .toMutableMap()

    override suspend fun getThreadsForUser(userId: String): List<MessageThread> {
        return messageThreads
            .filter { it.participantIds.contains(userId) }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun setTypingState(
        threadId: Int,
        userId: String,
        isTyping: Boolean
    ) {
        val threadIndex = messageThreads.indexOfFirst { it.id == threadId }
        if (threadIndex == -1) return

        val oldThread = messageThreads[threadIndex]
        val updatedTypingUsers = oldThread.typingUserIds.toMutableList()

        if (isTyping) {
            if (!updatedTypingUsers.contains(userId)) {
                updatedTypingUsers.add(userId)
            }
        } else {
            updatedTypingUsers.remove(userId)
        }

        messageThreads[threadIndex] = oldThread.copy(
            typingUserIds = updatedTypingUsers
        )
    }
    override fun startThreadsListener(
        userId: String,
        onUpdate: (List<MessageThread>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        onUpdate(
            messageThreads
                .filter { it.participantIds.contains(userId) }
                .sortedByDescending { it.updatedAt }
        )
        return {}
    }

    override suspend fun getMessages(threadId: Int): List<ChatMessage> {
        return chatMessages[threadId]
            ?.sortedBy { it.createdAt }
            ?.toList()
            ?: emptyList()
    }

    override fun startMessagesListener(
        threadId: Int,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        onUpdate(
            chatMessages[threadId]
                ?.sortedBy { it.createdAt }
                ?.toList()
                ?: emptyList()
        )
        return {}
    }

    override suspend fun markThreadAsRead(
        threadId: Int,
        userId: String
    ) {
        val threadIndex = messageThreads.indexOfFirst { it.id == threadId }
        if (threadIndex == -1) return

        val oldThread = messageThreads[threadIndex]
        val updatedUnreadMap = oldThread.unreadCountByUser.toMutableMap()
        updatedUnreadMap[userId] = 0

        messageThreads[threadIndex] = oldThread.copy(
            unreadCount = 0,
            unreadCountByUser = updatedUnreadMap
        )
    }

    override suspend fun markMessagesAsRead(
        threadId: Int,
        userId: String
    ) {
        val messages = chatMessages[threadId] ?: return

        chatMessages[threadId] = messages.map { message ->
            if (message.senderId != userId && !message.readByUserIds.contains(userId)) {
                message.copy(readByUserIds = message.readByUserIds + userId)
            } else {
                message
            }
        }.toMutableList()
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
        val allMessages = chatMessages.values.flatten()

        val newMessageId = ((allMessages.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        val nowMillis = System.currentTimeMillis()

        val newMessage = ChatMessage(
            id = newMessageId,
            threadId = threadId,
            senderId = senderId,
            text = text,
            time = currentTime(),
            createdAt = nowMillis,
            readByUserIds = listOf(senderId)
        )

        messages.add(newMessage)

        val threadIndex = messageThreads.indexOfFirst { it.id == threadId }
        if (threadIndex != -1) {
            val oldThread = messageThreads[threadIndex]
            val updatedUnreadMap = oldThread.unreadCountByUser.toMutableMap()

            oldThread.participantIds.forEach { participantId ->
                updatedUnreadMap[participantId] = if (participantId == senderId) {
                    0
                } else {
                    (updatedUnreadMap[participantId] ?: 0) + 1
                }
            }

            messageThreads[threadIndex] = oldThread.copy(
                lastMessage = text,
                unreadCount = updatedUnreadMap[senderId] ?: 0,
                unreadCountByUser = updatedUnreadMap,
                updatedAt = nowMillis,
                lastMessageSenderId = senderId
            )
        }
    }

    override suspend fun findThreadById(
        threadId: Int,
        currentUserId: String
    ): MessageThread? {
        val thread = messageThreads.firstOrNull { it.id == threadId } ?: return null
        return thread.copy(
            unreadCount = thread.unreadCountByUser[currentUserId] ?: 0
        )
    }

    override suspend fun createOrGetThread(
        currentUserId: String,
        currentUserName: String,
        recipientId: String,
        recipientName: String
    ): MessageThread {
        val existingThread = messageThreads.firstOrNull { thread ->
            thread.participantIds.contains(currentUserId) && thread.participantIds.contains(recipientId)
        }

        if (existingThread != null) {
            return existingThread.copy(
                unreadCount = existingThread.unreadCountByUser[currentUserId] ?: 0
            )
        }

        val newThreadId = (messageThreads.maxOfOrNull { it.id } ?: 0) + 1
        val nowMillis = System.currentTimeMillis()

        val newThread = MessageThread(
            id = newThreadId,
            recipientId = recipientId,
            recipientName = recipientName,
            lastMessage = "",
            unreadCount = 0,
            participantIds = listOf(currentUserId, recipientId),
            participantNames = mapOf(
                currentUserId to currentUserName,
                recipientId to recipientName
            ),
            unreadCountByUser = mapOf(
                currentUserId to 0,
                recipientId to 0
            ),
            updatedAt = nowMillis,
            lastMessageSenderId = ""
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