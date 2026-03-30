package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseMessagesRepository : MessagesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val threadsCollection = firestore.collection("messageThreads")

    override suspend fun getThreads(): List<MessageThread> {
        return try {
            val snapshot = threadsCollection
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    MessageThread(
                        id = doc.id.toIntOrNull() ?: return@mapNotNull null,
                        recipientId = doc.getString("recipientId") ?: "",
                        recipientName = doc.getString("recipientName") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
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
                try {
                    ChatMessage(
                        id = doc.id.hashCode(),
                        threadId = threadId,
                        senderId = doc.getString("senderId") ?: "",
                        text = doc.getString("text") ?: "",
                        time = doc.getString("time") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
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
                    try {
                        ChatMessage(
                            id = doc.id.hashCode(),
                            threadId = threadId,
                            senderId = doc.getString("senderId") ?: "",
                            text = doc.getString("text") ?: "",
                            time = doc.getString("time") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                onUpdate(messages)
            }

        return {
            registration.remove()
        }
    }

    override suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    ) {
        val nowMillis = System.currentTimeMillis()

        val messageData = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "time" to currentTime(),
            "createdAt" to nowMillis
        )

        val threadRef = threadsCollection.document(threadId.toString())

        threadRef.collection("messages").add(messageData).await()

        threadRef.update(
            mapOf(
                "lastMessage" to text,
                "updatedAt" to nowMillis,
                "unreadCount" to 0
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

    override suspend fun findThreadById(threadId: Int): MessageThread? {
        return try {
            val doc = threadsCollection.document(threadId.toString()).get().await()

            if (!doc.exists()) return null

            MessageThread(
                id = doc.id.toIntOrNull() ?: return null,
                recipientId = doc.getString("recipientId") ?: "",
                recipientName = doc.getString("recipientName") ?: "",
                lastMessage = doc.getString("lastMessage") ?: "",
                unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createOrGetThread(
        recipientId: String,
        recipientName: String
    ): MessageThread {
        val snapshot = threadsCollection.get().await()

        val existing = snapshot.documents.firstOrNull {
            it.getString("recipientId") == recipientId
        }

        if (existing != null) {
            return MessageThread(
                id = existing.id.toInt(),
                recipientId = existing.getString("recipientId") ?: "",
                recipientName = existing.getString("recipientName") ?: "",
                lastMessage = existing.getString("lastMessage") ?: "",
                unreadCount = existing.getLong("unreadCount")?.toInt() ?: 0
            )
        }

        val nextId = (snapshot.documents.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1
        val nowMillis = System.currentTimeMillis()

        val threadData = hashMapOf(
            "recipientId" to recipientId,
            "recipientName" to recipientName,
            "lastMessage" to "",
            "unreadCount" to 0,
            "updatedAt" to nowMillis
        )

        threadsCollection.document(nextId.toString()).set(threadData).await()

        return MessageThread(
            id = nextId,
            recipientId = recipientId,
            recipientName = recipientName,
            lastMessage = "",
            unreadCount = 0
        )
    }

    private fun currentTime(): String {
        val now = java.time.LocalTime.now()
        return now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
}