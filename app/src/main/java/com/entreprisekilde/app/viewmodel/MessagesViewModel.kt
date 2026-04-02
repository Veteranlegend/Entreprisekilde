package com.entreprisekilde.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.entreprisekilde.app.data.repository.messages.MessagesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val repository: MessagesRepository
) : ViewModel() {

    val messageThreads = mutableStateListOf<MessageThread>()
    val currentMessages = mutableStateListOf<ChatMessage>()
    val selectedThread = mutableStateOf<MessageThread?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    private var activeUserId: String? = null
    private var removeThreadsListener: (() -> Unit)? = null
    private var removeMessagesListener: (() -> Unit)? = null
    private var typingStopJob: Job? = null

    fun startListeningForUser(userId: String) {
        if (activeUserId == userId && removeThreadsListener != null) return

        activeUserId = userId
        stopListeningToThreads()

        removeThreadsListener = repository.startThreadsListener(
            userId = userId,
            onUpdate = { threads ->
                messageThreads.clear()
                messageThreads.addAll(threads.sortedByDescending { it.updatedAt })

                val selectedId = selectedThread.value?.id
                if (selectedId != null) {
                    val updatedSelected = threads.find { it.id == selectedId }
                    selectedThread.value = updatedSelected
                }
            },
            onError = { message ->
                errorMessage.value = message
            }
        )
    }

    fun stopListening() {
        val threadId = selectedThread.value?.id
        val userId = activeUserId

        if (threadId != null && userId != null) {
            viewModelScope.launch {
                try {
                    repository.setTypingState(threadId, userId, false)
                } catch (_: Exception) {
                }
            }
        }

        activeUserId = null
        stopListeningToThreads()
        stopListeningToMessages()
        messageThreads.clear()
        currentMessages.clear()
        selectedThread.value = null
    }

    fun selectThread(
        thread: MessageThread,
        currentUserId: String
    ) {
        selectedThread.value = thread
        startListeningToMessages(thread.id)
        markCurrentThreadAsRead(currentUserId)
    }

    fun selectThreadById(
        threadId: Int,
        currentUserId: String
    ): Boolean {
        val thread = messageThreads.find { it.id == threadId } ?: return false
        selectThread(thread, currentUserId)
        return true
    }

    fun createOrGetThread(
        currentUserId: String,
        currentUserName: String,
        recipientId: String,
        recipientName: String,
        onReady: (MessageThread) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val thread = repository.createOrGetThread(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    recipientId = recipientId,
                    recipientName = recipientName
                )

                upsertThreadLocally(thread)
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

                messageThreads.removeAll { it.id == thread.id }

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
        message: String,
        onSuccess: () -> Unit = {}
    ) {
        val thread = selectedThread.value ?: return
        val currentUserId = activeUserId ?: senderId

        viewModelScope.launch {
            try {
                repository.sendMessage(
                    threadId = thread.id,
                    senderId = senderId,
                    text = message
                )

                repository.setTypingState(thread.id, senderId, false)

                val updatedThread = repository.findThreadById(
                    threadId = thread.id,
                    currentUserId = currentUserId
                ) ?: thread.copy(
                    lastMessage = message,
                    updatedAt = System.currentTimeMillis(),
                    lastMessageSenderId = senderId
                )

                upsertThreadLocally(updatedThread)
                selectedThread.value = updatedThread

                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to send message."
            }
        }
    }

    fun markCurrentThreadAsRead(currentUserId: String) {
        val thread = selectedThread.value ?: return

        viewModelScope.launch {
            try {
                repository.markThreadAsRead(thread.id, currentUserId)
                repository.markMessagesAsRead(thread.id, currentUserId)

                val updatedThread = repository.findThreadById(thread.id, currentUserId)
                if (updatedThread != null) {
                    selectedThread.value = updatedThread
                    upsertThreadLocally(updatedThread)
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to mark chat as read."
            }
        }
    }

    fun onMessageInputChanged(currentUserId: String, text: String) {
        val thread = selectedThread.value ?: return

        viewModelScope.launch {
            try {
                repository.setTypingState(
                    threadId = thread.id,
                    userId = currentUserId,
                    isTyping = text.isNotBlank()
                )
            } catch (_: Exception) {
            }
        }

        typingStopJob?.cancel()
        if (text.isNotBlank()) {
            typingStopJob = viewModelScope.launch {
                delay(2000)
                try {
                    repository.setTypingState(
                        threadId = thread.id,
                        userId = currentUserId,
                        isTyping = false
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    fun clearTypingState(currentUserId: String) {
        val thread = selectedThread.value ?: return

        typingStopJob?.cancel()
        viewModelScope.launch {
            try {
                repository.setTypingState(thread.id, currentUserId, false)
            } catch (_: Exception) {
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

    private fun upsertThreadLocally(thread: MessageThread) {
        val index = messageThreads.indexOfFirst { it.id == thread.id }

        if (index == -1) {
            messageThreads.add(0, thread)
        } else {
            messageThreads[index] = thread
        }

        val sorted = messageThreads.sortedByDescending { it.updatedAt }
        messageThreads.clear()
        messageThreads.addAll(sorted)
    }

    private fun stopListeningToThreads() {
        removeThreadsListener?.invoke()
        removeThreadsListener = null
    }

    private fun stopListeningToMessages() {
        removeMessagesListener?.invoke()
        removeMessagesListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningToThreads()
        stopListeningToMessages()
    }
}