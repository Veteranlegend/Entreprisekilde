package com.entreprisekilde.app.ui.admin.messages

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.entreprisekilde.app.data.repository.messages.MessagesRepository
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val repository: MessagesRepository
) : ViewModel() {

    val messageThreads = mutableStateListOf<MessageThread>()
    val currentMessages = mutableStateListOf<ChatMessage>()
    val selectedThread = mutableStateOf<MessageThread?>(null)

    init {
        loadThreads()
    }

    fun selectThread(thread: MessageThread) {
        selectedThread.value = thread
        loadMessages(thread.id)
    }

    fun selectThreadById(threadId: Int): Boolean {
        val thread = messageThreads.find { it.id == threadId } ?: return false
        selectThread(thread)
        return true
    }

    fun deleteThread(thread: MessageThread) {
        viewModelScope.launch {
            repository.deleteThread(thread.id)
            refreshThreads()

            if (selectedThread.value?.id == thread.id) {
                selectedThread.value = null
                currentMessages.clear()
            }
        }
    }

    fun getMessagesForSelectedThread() = currentMessages

    fun sendMessage(message: String) {
        val thread = selectedThread.value ?: return

        viewModelScope.launch {
            repository.sendMessage(thread.id, message)
            refreshThreads()

            val updatedThread = repository.findThreadById(thread.id)
            selectedThread.value = updatedThread

            loadMessages(thread.id)
        }
    }

    private fun loadThreads() {
        viewModelScope.launch {
            messageThreads.clear()
            messageThreads.addAll(repository.getThreads())
        }
    }

    private fun loadMessages(threadId: Int) {
        viewModelScope.launch {
            currentMessages.clear()
            currentMessages.addAll(repository.getMessages(threadId))
        }
    }

    private suspend fun refreshThreads() {
        messageThreads.clear()
        messageThreads.addAll(repository.getThreads())
    }
}