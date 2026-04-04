package com.entreprisekilde.app.viewmodel

import android.net.Uri
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

    // List of chat threads shown in the messages overview screen.
    // Using Compose state here means the UI updates automatically when this list changes.
    val messageThreads = mutableStateListOf<MessageThread>()

    // Messages for the currently selected thread.
    val currentMessages = mutableStateListOf<ChatMessage>()

    // Holds the active thread the user is currently viewing.
    val selectedThread = mutableStateOf<MessageThread?>(null)

    // Simple UI-facing error state for displaying repository failures.
    val errorMessage = mutableStateOf<String?>(null)

    // Tracks which user we are currently listening on behalf of.
    // This helps prevent duplicate listeners and keeps thread/message actions scoped correctly.
    private var activeUserId: String? = null

    // Repository returns cleanup callbacks for listeners.
    // We keep them so we can stop listening when switching users or screens.
    private var removeThreadsListener: (() -> Unit)? = null
    private var removeMessagesListener: (() -> Unit)? = null

    // Used to debounce typing state, so we do not constantly spam "stopped typing" updates.
    private var typingStopJob: Job? = null

    fun startListeningForUser(userId: String) {
        // If we are already listening for this exact user and the listener is active,
        // there is no reason to recreate it.
        if (activeUserId == userId && removeThreadsListener != null) return

        activeUserId = userId

        // Always stop any existing thread listener first so we do not end up with duplicates.
        stopListeningToThreads()

        removeThreadsListener = repository.startThreadsListener(
            userId = userId,
            onUpdate = { threads ->
                // Replace the current thread list with the latest sorted version from the repository.
                messageThreads.clear()
                messageThreads.addAll(threads.sortedByDescending { it.updatedAt })

                // If a thread is already selected, keep that selected thread in sync
                // with the newest data coming from the listener.
                val selectedId = selectedThread.value?.id
                if (selectedId != null) {
                    val updatedSelected = threads.find { it.id == selectedId }
                    if (updatedSelected != null) {
                        selectedThread.value = updatedSelected
                    } else if (selectedThread.value?.id == selectedId) {
                        // If the selected thread no longer exists, clear everything tied to it
                        // so the UI does not keep showing stale chat data.
                        selectedThread.value = null
                        currentMessages.clear()
                        stopListeningToMessages()
                    }
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

        // Before shutting down listeners, try to clear the typing state for the active user.
        // This avoids leaving the other user seeing a stuck "typing..." status.
        if (threadId != null && userId != null) {
            viewModelScope.launch {
                try {
                    repository.setTypingState(threadId, userId, false)
                } catch (_: Exception) {
                    // Safe to ignore here. Cleanup should continue even if this fails.
                }
            }
        }

        typingStopJob?.cancel()
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
        // Set the selected thread first so the UI can switch immediately,
        // then start the per-thread message listener.
        selectedThread.value = thread
        startListeningToMessages(thread.id, currentUserId)

        // Opening a thread should immediately mark it as read.
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
                // Reuse an existing thread if one already exists, otherwise create a new one.
                val thread = repository.createOrGetThread(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    recipientId = recipientId,
                    recipientName = recipientName
                )

                // Make sure the local list reflects the latest thread state immediately.
                upsertThreadLocally(thread)

                selectedThread.value = thread
                startListeningToMessages(thread.id, currentUserId)

                onReady(thread)
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to create chat."
            }
        }
    }

    fun deleteThread(thread: MessageThread) {
        val currentUserId = activeUserId ?: return

        viewModelScope.launch {
            try {
                repository.deleteThread(thread.id, currentUserId)

                // Remove the thread from the local list right away
                // so the UI stays in sync with the delete action.
                messageThreads.removeAll { it.id == thread.id }

                // If the user deleted the thread they were currently viewing,
                // clear the selected state and stop its message listener too.
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

    // Exposes the currently loaded messages for the selected thread.
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

                // Once a message is sent, the user is no longer considered typing.
                repository.setTypingState(thread.id, senderId, false)

                // Try to fetch the freshest thread state from the repository.
                // If that fails, build a reasonable local fallback so the UI still updates.
                val updatedThread = repository.findThreadById(
                    threadId = thread.id,
                    currentUserId = currentUserId
                ) ?: thread.copy(
                    lastMessage = message.trim(),
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

    fun sendImageMessage(
        senderId: String,
        imageUri: Uri,
        onSuccess: () -> Unit = {}
    ) {
        val thread = selectedThread.value ?: return
        val currentUserId = activeUserId ?: senderId

        viewModelScope.launch {
            try {
                repository.sendImageMessage(
                    threadId = thread.id,
                    senderId = senderId,
                    imageUri = imageUri
                )

                // Sending an image also ends the typing state.
                repository.setTypingState(thread.id, senderId, false)

                // Same pattern as text messages:
                // prefer the real updated thread, but fall back to a local approximation if needed.
                val updatedThread = repository.findThreadById(
                    threadId = thread.id,
                    currentUserId = currentUserId
                ) ?: thread.copy(
                    lastMessage = "📷 Image",
                    updatedAt = System.currentTimeMillis(),
                    lastMessageSenderId = senderId
                )

                upsertThreadLocally(updatedThread)
                selectedThread.value = updatedThread

                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to send image."
            }
        }
    }

    fun markCurrentThreadAsRead(currentUserId: String) {
        val thread = selectedThread.value ?: return

        viewModelScope.launch {
            try {
                // Mark both the individual messages and the thread summary as read.
                // This keeps the chat screen and thread list in sync.
                repository.markMessagesAsRead(thread.id, currentUserId)
                repository.markThreadAsRead(thread.id, currentUserId)

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
                // Send typing status immediately whenever the input changes.
                repository.setTypingState(
                    threadId = thread.id,
                    userId = currentUserId,
                    isTyping = text.isNotBlank()
                )
            } catch (_: Exception) {
                // Typing state is helpful but not critical enough to surface as an error.
            }
        }

        // Reset any previous delayed "stop typing" job,
        // because the user is still actively interacting with the input.
        typingStopJob?.cancel()

        if (text.isNotBlank()) {
            typingStopJob = viewModelScope.launch {
                // Small delay so typing status does not flicker off between keystrokes.
                delay(2000)
                try {
                    repository.setTypingState(
                        threadId = thread.id,
                        userId = currentUserId,
                        isTyping = false
                    )
                } catch (_: Exception) {
                    // Safe to ignore for the same reason as above.
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
                // Typing cleanup failure should not interrupt the rest of the flow.
            }
        }
    }

    private fun startListeningToMessages(threadId: Int, currentUserId: String) {
        // Only one active message listener should exist at a time.
        stopListeningToMessages()

        removeMessagesListener = repository.startMessagesListener(
            threadId = threadId,
            onUpdate = { messages ->
                currentMessages.clear()
                currentMessages.addAll(messages)

                // Whenever new messages arrive, mark them as read for the current user.
                // This is especially useful when the user is actively sitting inside the chat screen.
                viewModelScope.launch {
                    try {
                        repository.markMessagesAsRead(threadId, currentUserId)
                        repository.markThreadAsRead(threadId, currentUserId)

                        val updatedThread = repository.findThreadById(threadId, currentUserId)
                        if (updatedThread != null && selectedThread.value?.id == threadId) {
                            selectedThread.value = updatedThread
                            upsertThreadLocally(updatedThread)
                        }
                    } catch (_: Exception) {
                        // Read sync failing here should not break the live message feed.
                    }
                }
            },
            onError = { message ->
                errorMessage.value = message
            }
        )
    }

    private fun upsertThreadLocally(thread: MessageThread) {
        val index = messageThreads.indexOfFirst { it.id == thread.id }

        if (index == -1) {
            // New thread: place it near the top immediately.
            messageThreads.add(0, thread)
        } else {
            // Existing thread: replace the old version with the updated one.
            messageThreads[index] = thread
        }

        // Re-sort after every insert/update so the newest activity always stays first.
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

        // Final safety cleanup when the ViewModel is destroyed.
        stopListeningToThreads()
        stopListeningToMessages()
    }
}