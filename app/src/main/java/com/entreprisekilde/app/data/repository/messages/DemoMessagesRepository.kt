package com.entreprisekilde.app.data.repository.messages

import android.net.Uri
import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.ChatMessage.Companion.MESSAGE_TYPE_IMAGE
import com.entreprisekilde.app.data.model.messages.ChatMessage.Companion.MESSAGE_TYPE_TEXT
import com.entreprisekilde.app.data.model.messages.MessageThread
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DemoMessagesRepository : MessagesRepository {

    private val messageThreads = DemoSeedData.createMessageThreads().toMutableList()
    private val chatMessages = DemoSeedData.createChatMessages()
        .mapValues { (_, messages) -> messages.toMutableList() }
        .toMutableMap()

    override suspend fun getThreadsForUser(userId: String): List<MessageThread> {
        return messageThreads
            .filter { it.participantIds.contains(userId) && !it.deletedForUserIds.contains(userId) }
            .map { thread ->
                thread.copy(unreadCount = thread.unreadCountByUser[userId] ?: 0)
            }
            .sortedByDescending { it.updatedAt }
    }

    override fun startThreadsListener(
        userId: String,
        onUpdate: (List<MessageThread>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        onUpdate(
            messageThreads
                .filter { it.participantIds.contains(userId) && !it.deletedForUserIds.contains(userId) }
                .map { thread ->
                    thread.copy(unreadCount = thread.unreadCountByUser[userId] ?: 0)
                }
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

    override suspend fun deleteThread(
        threadId: Int,
        currentUserId: String
    ) {
        val threadIndex = messageThreads.indexOfFirst { it.id == threadId }
        if (threadIndex == -1) return

        val oldThread = messageThreads[threadIndex]
        val updatedDeletedForUserIds = oldThread.deletedForUserIds.toMutableList()

        if (!updatedDeletedForUserIds.contains(currentUserId)) {
            updatedDeletedForUserIds.add(currentUserId)
        }

        messageThreads[threadIndex] = oldThread.copy(
            deletedForUserIds = updatedDeletedForUserIds
        )
    }

    override suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    ) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        val messagesForThread = chatMessages.getOrPut(threadId) { mutableListOf() }
        val allMessages = chatMessages.values.flatten()

        val newMessageId =
            ((allMessages.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        val nowMillis = System.currentTimeMillis()

        val newMessage = ChatMessage(
            id = newMessageId,
            threadId = threadId,
            senderId = senderId,
            text = trimmedText,
            imageUrl = "",
            messageType = MESSAGE_TYPE_TEXT,
            time = currentTime(nowMillis),
            createdAt = nowMillis,
            readByUserIds = listOf(senderId)
        )

        messagesForThread.add(newMessage)

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
                lastMessage = trimmedText,
                unreadCount = updatedUnreadMap[senderId] ?: 0,
                unreadCountByUser = updatedUnreadMap,
                updatedAt = nowMillis,
                lastMessageSenderId = senderId,
                deletedForUserIds = emptyList()
            )
        }
    }

    override suspend fun sendImageMessage(
        threadId: Int,
        senderId: String,
        imageUri: Uri
    ) {
        val messagesForThread = chatMessages.getOrPut(threadId) { mutableListOf() }
        val allMessages = chatMessages.values.flatten()

        val newMessageId =
            ((allMessages.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        val nowMillis = System.currentTimeMillis()

        val newMessage = ChatMessage(
            id = newMessageId,
            threadId = threadId,
            senderId = senderId,
            text = "",
            imageUrl = imageUri.toString(),
            messageType = MESSAGE_TYPE_IMAGE,
            time = currentTime(nowMillis),
            createdAt = nowMillis,
            readByUserIds = listOf(senderId)
        )

        messagesForThread.add(newMessage)

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
                lastMessage = "📷 Image",
                unreadCount = updatedUnreadMap[senderId] ?: 0,
                unreadCountByUser = updatedUnreadMap,
                updatedAt = nowMillis,
                lastMessageSenderId = senderId,
                deletedForUserIds = emptyList()
            )
        }
    }

    override suspend fun findThreadById(
        threadId: Int,
        currentUserId: String
    ): MessageThread? {
        val thread = messageThreads.firstOrNull { it.id == threadId } ?: return null
        if (thread.deletedForUserIds.contains(currentUserId)) return null

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
            thread.participantIds.size == 2 &&
                    thread.participantIds.contains(currentUserId) &&
                    thread.participantIds.contains(recipientId)
        }

        if (existingThread != null) {
            val threadIndex = messageThreads.indexOfFirst { it.id == existingThread.id }
            if (threadIndex != -1) {
                val oldThread = messageThreads[threadIndex]
                val updatedDeletedForUserIds = oldThread.deletedForUserIds.filter { it != currentUserId }

                messageThreads[threadIndex] = oldThread.copy(
                    deletedForUserIds = updatedDeletedForUserIds
                )
            }

            val updatedThread = messageThreads.first { it.id == existingThread.id }
            return updatedThread.copy(
                unreadCount = updatedThread.unreadCountByUser[currentUserId] ?: 0
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
            lastMessageSenderId = "",
            typingUserIds = emptyList(),
            deletedForUserIds = emptyList()
        )

        messageThreads.add(newThread)
        chatMessages[newThreadId] = mutableListOf()

        return newThread
    }

    private fun currentTime(nowMillis: Long): String {
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault()
        )
        return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}