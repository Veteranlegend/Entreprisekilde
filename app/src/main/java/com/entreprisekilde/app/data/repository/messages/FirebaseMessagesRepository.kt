package com.entreprisekilde.app.data.repository.messages

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseMessagesRepository : MessagesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val threadsCollection = firestore.collection("messageThreads")

    override suspend fun getThreads(): List<MessageThread> {
        return try {
            val snapshot = threadsCollection.get().await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    MessageThread(
                        id = doc.id.toIntOrNull() ?: return@mapNotNull null,
                        name = doc.getString("name") ?: "",
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
                .orderBy("createdAt")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    ChatMessage(
                        id = doc.id.hashCode(),
                        threadId = threadId,
                        text = doc.getString("text") ?: "",
                        isFromMe = doc.getBoolean("isFromMe") ?: false,
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

    override suspend fun sendMessage(threadId: Int, text: String) {
        val messageData = hashMapOf(
            "text" to text,
            "isFromMe" to true,
            "time" to currentTime(),
            "createdAt" to System.currentTimeMillis()
        )

        val threadRef = threadsCollection.document(threadId.toString())

        threadRef.collection("messages").add(messageData).await()

        threadRef.update(
            mapOf(
                "lastMessage" to text,
                "updatedAt" to System.currentTimeMillis(),
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
                name = doc.getString("name") ?: "",
                lastMessage = doc.getString("lastMessage") ?: "",
                unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun currentTime(): String {
        val now = java.time.LocalTime.now()
        return now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
}