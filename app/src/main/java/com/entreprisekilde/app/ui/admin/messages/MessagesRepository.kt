package com.entreprisekilde.app.ui.admin.messages

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.entreprisekilde.app.data.AppDemoData

class MessagesRepository {

    private val messageThreads = AppDemoData.createMessageThreads()
    private val chatMessages = AppDemoData.createChatMessages()

    fun getThreads(): SnapshotStateList<MessageThread> {
        return messageThreads
    }

    fun getMessages(threadId: Int): SnapshotStateList<ChatMessage> {
        return chatMessages.getOrPut(threadId) {
            mutableStateListOf()
        }
    }

    fun deleteThread(thread: MessageThread) {
        messageThreads.removeAll { it.id == thread.id }
        chatMessages.remove(thread.id)
    }

    fun sendMessage(thread: MessageThread, message: String) {
        val messages = getMessages(thread.id)

        messages.add(
            ChatMessage(
                id = messages.size + 1,
                threadId = thread.id,
                text = message,
                isFromMe = true,
                time = "Now"
            )
        )

        val index = messageThreads.indexOfFirst { it.id == thread.id }
        if (index != -1) {
            messageThreads[index] = messageThreads[index].copy(
                lastMessage = message
            )
        }
    }
}