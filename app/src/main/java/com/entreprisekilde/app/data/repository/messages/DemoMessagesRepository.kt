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

/**
 * Demo/in-memory implementation of [MessagesRepository].
 *
 * This repository is useful during development and testing because it allows
 * the messaging feature to work without Firebase/Firestore.
 *
 * Important:
 * - Data only exists in memory while the app is running
 * - Nothing is persisted permanently
 * - This is mainly used to simulate real repository behavior
 */
class DemoMessagesRepository : MessagesRepository {

    /**
     * In-memory list of chat threads.
     *
     * We start with seeded demo data so the app already has conversations to show.
     */
    private val messageThreads = DemoSeedData.createMessageThreads().toMutableList()

    /**
     * In-memory map of threadId -> messages in that thread.
     *
     * Each thread gets its own mutable message list so we can simulate sending,
     * reading, and updating chat state locally.
     */
    private val chatMessages = DemoSeedData.createChatMessages()
        .mapValues { (_, messages) -> messages.toMutableList() }
        .toMutableMap()

    /**
     * Returns all visible threads for a specific user.
     *
     * We only include threads where:
     * - the user is a participant
     * - the thread has not been soft-deleted for that user
     *
     * unreadCount is also recalculated from unreadCountByUser so the UI gets
     * the correct user-specific unread value.
     */
    override suspend fun getThreadsForUser(userId: String): List<MessageThread> {
        return messageThreads
            .filter { it.participantIds.contains(userId) && !it.deletedForUserIds.contains(userId) }
            .map { thread ->
                thread.copy(unreadCount = thread.unreadCountByUser[userId] ?: 0)
            }
            .sortedByDescending { it.updatedAt }
    }

    /**
     * Demo version of a real-time thread listener.
     *
     * Since this is not backed by Firestore, there is no actual live listener here.
     * Instead, we immediately return the current data once and return an empty
     * unsubscribe function.
     */
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

    /**
     * Returns all messages for a thread sorted from oldest to newest.
     */
    override suspend fun getMessages(threadId: Int): List<ChatMessage> {
        return chatMessages[threadId]
            ?.sortedBy { it.createdAt }
            ?.toList()
            ?: emptyList()
    }

    /**
     * Demo version of a real-time message listener.
     *
     * Same idea as startThreadsListener():
     * we return the current messages once and provide a no-op unsubscribe function.
     */
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

    /**
     * Marks the whole thread as read for the given user by resetting that user's
     * unread count to 0.
     *
     * This only updates thread-level unread state.
     * Individual message read tracking is handled separately in markMessagesAsRead().
     */
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

    /**
     * Marks each message in the thread as read for the given user.
     *
     * Rules:
     * - We do not mark the user's own messages as read
     * - We only add the user if not already present in readByUserIds
     */
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

    /**
     * Updates typing state for a user inside a thread.
     *
     * This simulates real-time "user is typing" behavior by adding/removing
     * the user ID from typingUserIds.
     */
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

    /**
     * Soft-deletes a thread for the current user.
     *
     * The thread is not removed from the repository entirely.
     * Instead, the current user's ID is added to deletedForUserIds so it becomes
     * hidden only for that user.
     */
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

    /**
     * Sends a normal text message in a thread.
     *
     * What happens here:
     * - trim the text
     * - ignore empty messages
     * - create a new message with a generated ID and timestamp
     * - update the thread preview and unread counts
     * - restore the thread for all users if it was previously soft-deleted
     */
    override suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    ) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        val messagesForThread = chatMessages.getOrPut(threadId) { mutableListOf() }
        val allMessages = chatMessages.values.flatten()

        // Generate next numeric message ID based on existing demo messages
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

            // Sender should have 0 unread in this thread.
            // Everyone else gets +1 unread.
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

    /**
     * Sends an image message in a thread.
     *
     * This follows the same flow as sendMessage(), but stores the image URI
     * as imageUrl and sets the message type to IMAGE.
     *
     * For the thread preview, we show a placeholder text instead of the actual URI.
     */
    override suspend fun sendImageMessage(
        threadId: Int,
        senderId: String,
        imageUri: Uri
    ) {
        val messagesForThread = chatMessages.getOrPut(threadId) { mutableListOf() }
        val allMessages = chatMessages.values.flatten()

        // Generate next numeric message ID based on existing demo messages
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

            // Sender should have 0 unread in this thread.
            // Everyone else gets +1 unread.
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

    /**
     * Finds a single thread by ID for the current user.
     *
     * Returns null if:
     * - the thread does not exist
     * - the thread was soft-deleted for this user
     */
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

    /**
     * Returns an existing 1-to-1 thread if one already exists.
     * Otherwise, creates a new thread between the two users.
     *
     * Important behavior:
     * - If the thread already exists but was deleted for the current user,
     *   it becomes visible again
     * - A newly created thread starts with empty messages and zero unread counts
     */
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

                // If the current user had previously deleted the thread,
                // bring it back by removing them from deletedForUserIds
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

    /**
     * Formats a timestamp into UI-friendly HH:mm time.
     *
     * Example:
     * 14:05
     */
    private fun currentTime(nowMillis: Long): String {
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault()
        )
        return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}