package com.entreprisekilde.app.data.repository.messages

import android.net.Uri
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread

/**
 * Contract for all message-related data operations in the app.
 *
 * This repository abstracts how threads and messages are fetched, observed,
 * created, updated, and deleted so the rest of the app does not need to care
 * whether the data comes from Firestore, REST, local cache, or something else.
 */
interface MessagesRepository {

    /**
     * Fetches all message threads that belong to a specific user.
     *
     * Typically used for the inbox / conversations overview screen.
     *
     * @param userId The ID of the user whose threads should be returned.
     * @return A list of message threads for the given user.
     */
    suspend fun getThreadsForUser(userId: String): List<MessageThread>

    /**
     * Starts a real-time listener for a user's message threads.
     *
     * This is useful when the UI should update automatically as new threads
     * appear or existing ones change (for example: unread count, last message,
     * typing state, etc.).
     *
     * The function returns another function that should be called to stop
     * listening when the screen/view model is cleared.
     *
     * @param userId The ID of the user whose threads should be observed.
     * @param onUpdate Called whenever the thread list changes.
     * @param onError Called if the listener fails for any reason.
     * @return A function that unsubscribes / removes the listener.
     */
    fun startThreadsListener(
        userId: String,
        onUpdate: (List<MessageThread>) -> Unit,
        onError: (String) -> Unit = {}
    ): () -> Unit

    /**
     * Fetches all messages for a specific thread.
     *
     * Usually used when opening a conversation screen and loading its history.
     *
     * @param threadId The ID of the thread to load messages from.
     * @return A list of chat messages in that thread.
     */
    suspend fun getMessages(threadId: Int): List<ChatMessage>

    /**
     * Starts a real-time listener for messages inside a specific thread.
     *
     * This allows the chat UI to update instantly when new messages arrive,
     * messages change, or read states are updated.
     *
     * As with the threads listener, the returned function should be called
     * when listening is no longer needed.
     *
     * @param threadId The ID of the thread to observe.
     * @param onUpdate Called whenever the message list changes.
     * @param onError Called if the listener fails.
     * @return A function that unsubscribes / removes the listener.
     */
    fun startMessagesListener(
        threadId: Int,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit = {}
    ): () -> Unit

    /**
     * Marks an entire thread as read for a specific user.
     *
     * This is often used when the user opens a conversation so the thread-level
     * unread indicator disappears from the inbox screen.
     *
     * @param threadId The thread to mark as read.
     * @param userId The user for whom the thread should be marked as read.
     */
    suspend fun markThreadAsRead(
        threadId: Int,
        userId: String
    )

    /**
     * Marks the messages inside a thread as read for a specific user.
     *
     * Depending on implementation, this may update per-message read state
     * instead of only a thread-level status.
     *
     * @param threadId The thread whose messages should be marked as read.
     * @param userId The user who has read the messages.
     */
    suspend fun markMessagesAsRead(
        threadId: Int,
        userId: String
    )

    /**
     * Updates the typing state for a user in a thread.
     *
     * Used to show "user is typing..." style indicators in a conversation.
     *
     * @param threadId The thread where typing state should be updated.
     * @param userId The user whose typing state is being changed.
     * @param isTyping True if the user is currently typing, false otherwise.
     */
    suspend fun setTypingState(
        threadId: Int,
        userId: String,
        isTyping: Boolean
    )

    /**
     * Deletes a thread for the current user.
     *
     * Depending on business logic, this may mean:
     * - fully deleting the thread for everyone, or
     * - only removing/hiding it for the current user.
     *
     * @param threadId The thread to delete.
     * @param currentUserId The user performing the delete action.
     */
    suspend fun deleteThread(
        threadId: Int,
        currentUserId: String
    )

    /**
     * Sends a plain text message in an existing thread.
     *
     * @param threadId The thread where the message should be sent.
     * @param senderId The ID of the user sending the message.
     * @param text The message content.
     */
    suspend fun sendMessage(
        threadId: Int,
        senderId: String,
        text: String
    )

    /**
     * Sends an image message in an existing thread.
     *
     * The implementation is expected to handle uploading the image (if needed)
     * and then creating the corresponding chat message entry.
     *
     * @param threadId The thread where the image should be sent.
     * @param senderId The ID of the user sending the image.
     * @param imageUri A URI pointing to the selected image.
     */
    suspend fun sendImageMessage(
        threadId: Int,
        senderId: String,
        imageUri: Uri
    )

    /**
     * Finds a specific thread by its ID, but only if it is accessible
     * to the current user.
     *
     * Useful when opening a thread from a deep link, notification,
     * or external navigation path.
     *
     * @param threadId The thread to find.
     * @param currentUserId The user requesting access to the thread.
     * @return The matching thread, or null if not found / not accessible.
     */
    suspend fun findThreadById(
        threadId: Int,
        currentUserId: String
    ): MessageThread?

    /**
     * Returns an existing thread between two users, or creates one if it
     * does not already exist.
     *
     * This is typically used when starting a new conversation from a profile,
     * contact list, or similar entry point.
     *
     * @param currentUserId The ID of the current user.
     * @param currentUserName The display name of the current user.
     * @param recipientId The ID of the other participant.
     * @param recipientName The display name of the other participant.
     * @return The existing or newly created message thread.
     */
    suspend fun createOrGetThread(
        currentUserId: String,
        currentUserName: String,
        recipientId: String,
        recipientName: String
    ): MessageThread
}