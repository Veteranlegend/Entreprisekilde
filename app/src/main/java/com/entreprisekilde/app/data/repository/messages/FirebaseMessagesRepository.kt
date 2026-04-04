package com.entreprisekilde.app.data.repository.messages

import android.net.Uri
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.ChatMessage.Companion.MESSAGE_TYPE_IMAGE
import com.entreprisekilde.app.data.model.messages.ChatMessage.Companion.MESSAGE_TYPE_TEXT
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Firebase implementation of [MessagesRepository].
 *
 * This repository is responsible for:
 * - loading threads for a specific user
 * - loading messages inside a thread
 * - listening for real-time updates from Firestore
 * - sending text and image messages
 * - managing unread counts, typing state, and soft delete behavior
 *
 * Firestore structure used here:
 * - messageThreads/{threadId}
 * - messageThreads/{threadId}/messages/{messageId}
 */
class FirebaseMessagesRepository : MessagesRepository {

    // Main Firestore database instance
    private val firestore = FirebaseFirestore.getInstance()

    // Firebase Storage instance used for uploading chat images
    private val storage = FirebaseStorage.getInstance()

    // Top-level collection containing all chat threads
    private val threadsCollection = firestore.collection("messageThreads")

    /**
     * Loads all threads that the given user participates in.
     *
     * We only return threads where:
     * - the user is included in participantIds
     * - the thread is not soft-deleted for that user
     *
     * The result is sorted so the newest active thread appears first.
     */
    override suspend fun getThreadsForUser(userId: String): List<MessageThread> {
        return try {
            val snapshot = threadsCollection
                .whereArrayContains("participantIds", userId)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { doc ->
                    mapThreadDocument(doc.id, doc.data, userId)
                }
                .filter { !it.deletedForUserIds.contains(userId) }
                .sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Starts a real-time Firestore listener for the user's chat threads.
     *
     * This is used so the UI updates automatically when:
     * - a new message arrives
     * - unread counts change
     * - thread order changes
     * - typing/deletion state changes
     *
     * Returns a function that removes the listener when no longer needed.
     */
    override fun startThreadsListener(
        userId: String,
        onUpdate: (List<MessageThread>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val registration: ListenerRegistration = threadsCollection
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to listen for threads.")
                    return@addSnapshotListener
                }

                val threads = snapshot?.documents
                    ?.mapNotNull { doc ->
                        mapThreadDocument(doc.id, doc.data, userId)
                    }
                    ?.filter { !it.deletedForUserIds.contains(userId) }
                    ?.sortedByDescending { it.updatedAt }
                    ?: emptyList()

                onUpdate(threads)
            }

        return {
            registration.remove()
        }
    }

    /**
     * Loads all messages inside a specific thread.
     *
     * Messages are sorted oldest -> newest so the UI can display them
     * in the correct conversation order.
     */
    override suspend fun getMessages(threadId: Int): List<ChatMessage> {
        return try {
            val snapshot = threadsCollection
                .document(threadId.toString())
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                mapChatMessageDocument(doc.id, threadId, doc.data)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Starts a real-time listener for messages inside one thread.
     *
     * This allows the chat screen to update immediately when:
     * - a new message is sent
     * - read status changes
     * - image messages are uploaded
     */
    override fun startMessagesListener(
        threadId: Int,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val registration: ListenerRegistration = threadsCollection
            .document(threadId.toString())
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to listen for messages.")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    mapChatMessageDocument(doc.id, threadId, doc.data)
                } ?: emptyList()

                onUpdate(messages)
            }

        return {
            registration.remove()
        }
    }

    /**
     * Marks the thread as read for one specific user.
     *
     * Important:
     * This only resets the thread-level unread count in unreadCountByUser.
     * Individual message read receipts are handled separately in markMessagesAsRead().
     */
    override suspend fun markThreadAsRead(
        threadId: Int,
        userId: String
    ) {
        val threadRef = threadsCollection.document(threadId.toString())
        val snapshot = threadRef.get().await()
        val data = snapshot.data ?: return

        val unreadMapAny = data["unreadCountByUser"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val unreadMap = unreadMapAny.entries.associate {
            it.key.toString() to ((it.value as? Number)?.toInt() ?: 0)
        }.toMutableMap()

        // No need to write to Firestore if unread is already zero
        if ((unreadMap[userId] ?: 0) == 0) return

        unreadMap[userId] = 0
        threadRef.update("unreadCountByUser", unreadMap).await()
    }

    /**
     * Marks all unread incoming messages in the thread as read for this user.
     *
     * Rules:
     * - we never mark the user's own messages as read
     * - we only update messages where the user is not already in readByUserIds
     *
     * A batch write is used so multiple message updates happen efficiently.
     */
    override suspend fun markMessagesAsRead(
        threadId: Int,
        userId: String
    ) {
        val messagesRef = threadsCollection
            .document(threadId.toString())
            .collection("messages")

        val snapshot = messagesRef.get().await()
        if (snapshot.isEmpty) return

        val batch = firestore.batch()
        var changed = false

        snapshot.documents.forEach { document ->
            val senderId = document.getString("senderId") ?: ""
            val readBy = (document.get("readByUserIds") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()

            if (senderId != userId && !readBy.contains(userId)) {
                batch.update(document.reference, "readByUserIds", FieldValue.arrayUnion(userId))
                changed = true
            }
        }

        if (changed) {
            batch.commit().await()
        }
    }

    /**
     * Updates the typing state for a user in a thread.
     *
     * The user's ID is either:
     * - added to typingUserIds when typing starts
     * - removed when typing stops
     *
     * This powers real-time "typing..." indicators in the UI.
     */
    override suspend fun setTypingState(
        threadId: Int,
        userId: String,
        isTyping: Boolean
    ) {
        val threadRef = threadsCollection.document(threadId.toString())

        if (isTyping) {
            threadRef.update("typingUserIds", FieldValue.arrayUnion(userId)).await()
        } else {
            threadRef.update("typingUserIds", FieldValue.arrayRemove(userId)).await()
        }
    }

    /**
     * Soft-deletes the thread for the current user.
     *
     * The thread is not removed from Firestore.
     * Instead, the current user's ID is added to deletedForUserIds so the thread
     * becomes hidden only for that user.
     */
    override suspend fun deleteThread(
        threadId: Int,
        currentUserId: String
    ) {
        val threadRef = threadsCollection.document(threadId.toString())
        threadRef.update(
            "deletedForUserIds",
            FieldValue.arrayUnion(currentUserId)
        ).await()
    }

    /**
     * Sends a text message in a thread.
     *
     * Flow:
     * 1. Validate and trim the text
     * 2. Load thread metadata
     * 3. Recalculate unread counts
     * 4. Create message document
     * 5. Update thread preview fields
     *
     * We also remove the sender from typingUserIds after sending.
     */
    override suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    ) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        val threadRef = threadsCollection.document(threadId.toString())
        val threadSnapshot = threadRef.get().await()
        val threadData = threadSnapshot.data ?: return

        val participantIds = (threadData["participantIds"] as? List<*>)?.filterIsInstance<String>()
            ?: emptyList()

        val participantNamesAny = threadData["participantNames"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val participantNames = participantNamesAny.entries.associate {
            it.key.toString() to it.value.toString()
        }

        // In 1-to-1 chat, recipient is the participant who is not the sender
        val recipientId = participantIds.firstOrNull { it != senderId }.orEmpty()
        val senderName = participantNames[senderId].orEmpty()

        val unreadMapAny = threadData["unreadCountByUser"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val unreadMap = unreadMapAny.entries.associate {
            it.key.toString() to ((it.value as? Number)?.toInt() ?: 0)
        }.toMutableMap()

        // Sender should have 0 unread in this thread.
        // Every other participant gets +1 unread.
        participantIds.forEach { participantId ->
            unreadMap[participantId] = if (participantId == senderId) 0 else (unreadMap[participantId] ?: 0) + 1
        }

        val nowMillis = System.currentTimeMillis()

        val messageData = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "recipientUserId" to recipientId,
            "text" to trimmedText,
            "imageUrl" to "",
            "messageType" to MESSAGE_TYPE_TEXT,
            "time" to currentTime(nowMillis),
            "createdAt" to nowMillis,
            "readByUserIds" to listOf(senderId)
        )

        // Store the message as a new document under the thread
        threadRef.collection("messages").add(messageData).await()

        // Update thread preview + metadata after the message is sent
        threadRef.update(
            mapOf(
                "lastMessage" to trimmedText,
                "lastMessageSenderId" to senderId,
                "updatedAt" to nowMillis,
                "unreadCountByUser" to unreadMap,
                "typingUserIds" to FieldValue.arrayRemove(senderId),
                "deletedForUserIds" to emptyList<String>()
            )
        ).await()
    }

    /**
     * Sends an image message in a thread.
     *
     * Flow:
     * 1. Load thread metadata
     * 2. Upload image to Firebase Storage
     * 3. Get public download URL
     * 4. Save image message in Firestore
     * 5. Update thread preview and unread counts
     */
    override suspend fun sendImageMessage(
        threadId: Int,
        senderId: String,
        imageUri: Uri
    ) {
        val threadRef = threadsCollection.document(threadId.toString())
        val threadSnapshot = threadRef.get().await()
        val threadData = threadSnapshot.data ?: return

        val participantIds = (threadData["participantIds"] as? List<*>)?.filterIsInstance<String>()
            ?: emptyList()

        val participantNamesAny = threadData["participantNames"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val participantNames = participantNamesAny.entries.associate {
            it.key.toString() to it.value.toString()
        }

        val recipientId = participantIds.firstOrNull { it != senderId }.orEmpty()
        val senderName = participantNames[senderId].orEmpty()

        val unreadMapAny = threadData["unreadCountByUser"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val unreadMap = unreadMapAny.entries.associate {
            it.key.toString() to ((it.value as? Number)?.toInt() ?: 0)
        }.toMutableMap()

        participantIds.forEach { participantId ->
            unreadMap[participantId] = if (participantId == senderId) 0 else (unreadMap[participantId] ?: 0) + 1
        }

        val nowMillis = System.currentTimeMillis()

        // Store image under a per-thread folder to keep chat uploads organized
        val imageRef = storage.reference
            .child("chat_images")
            .child(threadId.toString())
            .child("${UUID.randomUUID()}.jpg")

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        imageRef.putFile(imageUri, metadata).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()

        if (downloadUrl.isBlank()) {
            throw IllegalStateException("Image upload failed: empty download URL.")
        }

        val messageData = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "recipientUserId" to recipientId,
            "text" to "",
            "imageUrl" to downloadUrl,
            "messageType" to MESSAGE_TYPE_IMAGE,
            "time" to currentTime(nowMillis),
            "createdAt" to nowMillis,
            "readByUserIds" to listOf(senderId)
        )

        threadRef.collection("messages").add(messageData).await()

        // For image messages, thread preview shows a placeholder instead of the URL
        threadRef.update(
            mapOf(
                "lastMessage" to "📷 Image",
                "lastMessageSenderId" to senderId,
                "updatedAt" to nowMillis,
                "unreadCountByUser" to unreadMap,
                "typingUserIds" to FieldValue.arrayRemove(senderId),
                "deletedForUserIds" to emptyList<String>()
            )
        ).await()
    }

    /**
     * Finds one thread by ID for a specific user.
     *
     * Returns null if:
     * - the thread does not exist
     * - the thread cannot be mapped
     * - the thread is soft-deleted for this user
     */
    override suspend fun findThreadById(
        threadId: Int,
        currentUserId: String
    ): MessageThread? {
        return try {
            val doc = threadsCollection.document(threadId.toString()).get().await()
            if (!doc.exists()) return null

            val mapped = mapThreadDocument(doc.id, doc.data, currentUserId)
            if (mapped?.deletedForUserIds?.contains(currentUserId) == true) null else mapped
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns an existing 1-to-1 thread between the two users if it already exists.
     * Otherwise, creates a brand new thread.
     *
     * If an existing thread was previously soft-deleted by the current user,
     * it is restored by removing that user from deletedForUserIds.
     */
    override suspend fun createOrGetThread(
        currentUserId: String,
        currentUserName: String,
        recipientId: String,
        recipientName: String
    ): MessageThread {
        val snapshot = threadsCollection
            .whereArrayContains("participantIds", currentUserId)
            .get()
            .await()

        val existing = snapshot.documents.firstOrNull { document ->
            val participants = (document.get("participantIds") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()

            participants.size == 2 &&
                    participants.contains(currentUserId) &&
                    participants.contains(recipientId)
        }

        if (existing != null) {
            threadsCollection.document(existing.id)
                .update("deletedForUserIds", FieldValue.arrayRemove(currentUserId))
                .await()

            return mapThreadDocument(
                docId = existing.id,
                data = existing.data?.toMutableMap()?.apply {
                    val oldDeleted = (this["deletedForUserIds"] as? List<*>)?.filterIsInstance<String>()
                        ?: emptyList()
                    this["deletedForUserIds"] = oldDeleted.filter { it != currentUserId }
                },
                currentUserId = currentUserId
            ) ?: throw IllegalStateException("Failed to map existing thread.")
        }

        // Thread IDs are stored as numeric strings, so we generate the next available one
        val allThreadsSnapshot = threadsCollection.get().await()
        val nextId = (allThreadsSnapshot.documents.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1
        val nowMillis = System.currentTimeMillis()

        val threadData = hashMapOf(
            "participantIds" to listOf(currentUserId, recipientId),
            "participantNames" to mapOf(
                currentUserId to currentUserName,
                recipientId to recipientName
            ),
            "lastMessage" to "",
            "lastMessageSenderId" to "",
            "updatedAt" to nowMillis,
            "unreadCountByUser" to mapOf(
                currentUserId to 0,
                recipientId to 0
            ),
            "typingUserIds" to emptyList<String>(),
            "deletedForUserIds" to emptyList<String>()
        )

        threadsCollection.document(nextId.toString()).set(threadData).await()

        return MessageThread(
            id = nextId,
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
    }

    /**
     * Maps a Firestore message document into the app's ChatMessage model.
     *
     * This keeps Firestore parsing logic in one place and makes the rest
     * of the repository cleaner.
     */
    private fun mapChatMessageDocument(
        docId: String,
        threadId: Int,
        data: Map<String, Any>?
    ): ChatMessage? {
        if (data == null) return null

        return ChatMessage(
            id = docId,
            threadId = threadId,
            senderId = data["senderId"] as? String ?: "",
            text = data["text"] as? String ?: "",
            imageUrl = data["imageUrl"] as? String ?: "",
            messageType = data["messageType"] as? String ?: MESSAGE_TYPE_TEXT,
            time = data["time"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            readByUserIds = (data["readByUserIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    /**
     * Maps a Firestore thread document into the app's MessageThread model.
     *
     * This method also calculates the correct recipient for the current user
     * and extracts the user-specific unread count.
     */
    private fun mapThreadDocument(
        docId: String,
        data: Map<String, Any>?,
        currentUserId: String
    ): MessageThread? {
        if (data == null) return null

        val participantIds = (data["participantIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        val participantNamesAny = data["participantNames"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val participantNames = participantNamesAny.entries.associate {
            it.key.toString() to it.value.toString()
        }

        val unreadMapAny = data["unreadCountByUser"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val unreadMap = unreadMapAny.entries.associate {
            it.key.toString() to ((it.value as? Number)?.toInt() ?: 0)
        }

        val typingUserIds = (data["typingUserIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val deletedForUserIds = (data["deletedForUserIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        val recipientId = participantIds.firstOrNull { it != currentUserId } ?: ""
        val recipientName = participantNames[recipientId] ?: "Unknown user"

        return MessageThread(
            id = docId.toIntOrNull() ?: return null,
            recipientId = recipientId,
            recipientName = recipientName,
            lastMessage = data["lastMessage"] as? String ?: "",
            unreadCount = unreadMap[currentUserId] ?: 0,
            participantIds = participantIds,
            participantNames = participantNames,
            unreadCountByUser = unreadMap,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
            lastMessageSenderId = data["lastMessageSenderId"] as? String ?: "",
            typingUserIds = typingUserIds,
            deletedForUserIds = deletedForUserIds
        )
    }

    /**
     * Formats a timestamp into a simple UI-friendly HH:mm value.
     *
     * Example:
     * 09:41
     */
    private fun currentTime(nowMillis: Long): String {
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault()
        )
        return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}