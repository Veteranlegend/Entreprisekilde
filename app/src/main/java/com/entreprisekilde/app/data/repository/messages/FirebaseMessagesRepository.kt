package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FirebaseMessagesRepository : MessagesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val threadsCollection = firestore.collection("messageThreads")

    override suspend fun getThreadsForUser(userId: String): List<MessageThread> {
        return try {
            val snapshot = threadsCollection
                .whereArrayContains("participantIds", userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                mapThreadDocument(doc.id, doc.data, userId)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun startThreadsListener(
        userId: String,
        onUpdate: (List<MessageThread>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val registration: ListenerRegistration = threadsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to listen for threads.")
                    return@addSnapshotListener
                }

                val threads = snapshot?.documents?.mapNotNull { doc ->
                    mapThreadDocument(doc.id, doc.data, userId)
                } ?: emptyList()

                onUpdate(threads)
            }

        return {
            registration.remove()
        }
    }

    override suspend fun getMessages(threadId: Int): List<ChatMessage> {
        return try {
            val snapshot = threadsCollection
                .document(threadId.toString())
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                ChatMessage(
                    id = doc.id,
                    threadId = threadId,
                    senderId = data["senderId"] as? String ?: "",
                    text = data["text"] as? String ?: "",
                    time = data["time"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                    readByUserIds = (data["readByUserIds"] as? List<*>)?.filterIsInstance<String>()
                        ?: emptyList()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

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
                    val data = doc.data ?: return@mapNotNull null

                    ChatMessage(
                        id = doc.id,
                        threadId = threadId,
                        senderId = data["senderId"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        time = data["time"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                        readByUserIds = (data["readByUserIds"] as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList()
                    )
                } ?: emptyList()

                onUpdate(messages)
            }

        return {
            registration.remove()
        }
    }

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

        if ((unreadMap[userId] ?: 0) == 0) return

        unreadMap[userId] = 0
        threadRef.update("unreadCountByUser", unreadMap).await()
    }

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
            val readBy = document.get("readByUserIds") as? List<*> ?: emptyList<Any>()

            if (senderId != userId && !readBy.contains(userId)) {
                batch.update(document.reference, "readByUserIds", FieldValue.arrayUnion(userId))
                changed = true
            }
        }

        if (changed) {
            batch.commit().await()
        }
    }

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

    override suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    ) {
        val threadRef = threadsCollection.document(threadId.toString())
        val threadSnapshot = threadRef.get().await()
        val threadData = threadSnapshot.data ?: return

        val participantIds = (threadData["participantIds"] as? List<*>)?.filterIsInstance<String>()
            ?: emptyList()

        val unreadMapAny = threadData["unreadCountByUser"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val unreadMap = unreadMapAny.entries.associate {
            it.key.toString() to ((it.value as? Number)?.toInt() ?: 0)
        }.toMutableMap()

        participantIds.forEach { participantId ->
            unreadMap[participantId] = if (participantId == senderId) {
                0
            } else {
                (unreadMap[participantId] ?: 0) + 1
            }
        }

        val nowMillis = System.currentTimeMillis()

        val messageData = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "time" to currentTime(nowMillis),
            "createdAt" to nowMillis,
            "readByUserIds" to listOf(senderId)
        )

        threadRef.collection("messages").add(messageData).await()

        threadRef.update(
            mapOf(
                "lastMessage" to text,
                "lastMessageSenderId" to senderId,
                "updatedAt" to nowMillis,
                "unreadCountByUser" to unreadMap,
                "typingUserIds" to FieldValue.arrayRemove(senderId)
            )
        ).await()
    }

    override suspend fun deleteThread(threadId: Int) {
        val threadRef = threadsCollection.document(threadId.toString())
        val messages = threadRef.collection("messages").get().await()

        for (doc in messages.documents) {
            doc.reference.delete().await()
        }

        threadRef.delete().await()
    }

    override suspend fun findThreadById(
        threadId: Int,
        currentUserId: String
    ): MessageThread? {
        return try {
            val doc = threadsCollection.document(threadId.toString()).get().await()
            if (!doc.exists()) return null

            mapThreadDocument(doc.id, doc.data, currentUserId)
        } catch (e: Exception) {
            null
        }
    }

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
            participants.contains(recipientId)
        }

        if (existing != null) {
            return mapThreadDocument(
                docId = existing.id,
                data = existing.data,
                currentUserId = currentUserId
            ) ?: throw IllegalStateException("Failed to map existing thread.")
        }

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
            "typingUserIds" to emptyList<String>()
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
            typingUserIds = emptyList()
        )
    }

    private fun mapThreadDocument(
        docId: String,
        data: Map<String, Any>?,
        currentUserId: String
    ): MessageThread? {
        if (data == null) return null

        val participantIds = (data["participantIds"] as? List<*>)?.filterIsInstance<String>()
            ?: emptyList()

        val participantNamesAny = data["participantNames"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val participantNames = participantNamesAny.entries.associate {
            it.key.toString() to it.value.toString()
        }

        val unreadMapAny = data["unreadCountByUser"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val unreadMap = unreadMapAny.entries.associate {
            it.key.toString() to ((it.value as? Number)?.toInt() ?: 0)
        }

        val typingUserIds = (data["typingUserIds"] as? List<*>)?.filterIsInstance<String>()
            ?: emptyList()

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
            typingUserIds = typingUserIds
        )
    }

    private fun currentTime(nowMillis: Long): String {
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault()
        )
        return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}