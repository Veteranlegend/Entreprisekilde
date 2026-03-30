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
    val errorMessage = mutableStateOf<String?>(null)

    private var removeMessagesListener: (() -> Unit)? = null

    init {
        loadThreads()
    }

    fun selectThread(thread: MessageThread) {
        selectedThread.value = thread
        startListeningToMessages(thread.id)
    }

    fun selectThreadById(threadId: Int): Boolean {
        val thread = messageThreads.find { it.id == threadId } ?: return false
        selectThread(thread)
        return true
    }

    fun createOrGetThread(
        recipientId: String,
        recipientName: String,
        onReady: (MessageThread) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val thread = repository.createOrGetThread(
                    recipientId = recipientId,
                    recipientName = recipientName
                )
                refreshThreads()
                selectedThread.value = thread
                startListeningToMessages(thread.id)
                onReady(thread)
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to create chat."
            }
        }
    }

    fun deleteThread(thread: MessageThread) {
        viewModelScope.launch {
            try {
                repository.deleteThread(thread.id)
                refreshThreads()

                if (selectedThread.value?.id == thread.id) {
                    stopListeningToMessages()
                    selectedThread.value = null
                    currentMessages.clear()
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to delete chat."
            }
        }
    }

    fun getMessagesForSelectedThread() = currentMessages

    fun sendMessage(
        senderId: String,
        message: String
    ) {
        val thread = selectedThread.value ?: return

        viewModelScope.launch {
            try {
                repository.sendMessage(
                    threadId = thread.id,
                    senderId = senderId,
                    text = message
                )
                refreshThreads()

                val updatedThread = repository.findThreadById(thread.id)
                selectedThread.value = updatedThread
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to send message."
            }
        }
    }

    private fun loadThreads() {
        viewModelScope.launch {
            try {
                messageThreads.clear()
                messageThreads.addAll(repository.getThreads())
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to load chats."
            }
        }
    }

    private fun startListeningToMessages(threadId: Int) {
        stopListeningToMessages()

        removeMessagesListener = repository.startMessagesListener(
            threadId = threadId,
            onUpdate = { messages ->
                currentMessages.clear()
                currentMessages.addAll(messages)
            },
            onError = { message ->
                errorMessage.value = message
            }
        )
    }

    private fun stopListeningToMessages() {
        removeMessagesListener?.invoke()
        removeMessagesListener = null
    }

    private suspend fun refreshThreads() {
        messageThreads.clear()
        messageThreads.addAll(repository.getThreads())
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningToMessages()
    }
}