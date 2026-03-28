package com.entreprisekilde.app.ui.admin.messages

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MessagesViewModel(
    private val repository: MessagesRepository
) : ViewModel() {

    val messageThreads = repository.getThreads()
    val selectedThread = mutableStateOf<MessageThread?>(null)

    fun selectThread(thread: MessageThread) {
        selectedThread.value = thread

        val index = messageThreads.indexOfFirst { it.id == thread.id }
        if (index != -1) {
            messageThreads[index] = messageThreads[index].copy(unreadCount = 0)
        }
    }

    fun selectThreadById(threadId: Int): Boolean {
        val thread = messageThreads.find { it.id == threadId } ?: return false
        selectThread(thread)
        return true
    }

    fun deleteThread(thread: MessageThread) {
        repository.deleteThread(thread)

        if (selectedThread.value?.id == thread.id) {
            selectedThread.value = null
        }
    }

    fun getMessagesForSelectedThread() =
        selectedThread.value?.let { repository.getMessages(it.id) }

    fun sendMessage(message: String) {
        val thread = selectedThread.value ?: return
        repository.sendMessage(thread, message)
    }
}