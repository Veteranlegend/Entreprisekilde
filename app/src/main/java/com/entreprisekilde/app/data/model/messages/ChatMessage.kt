package com.entreprisekilde.app.data.model.messages

/**
 * Represents a single message inside a chat thread.
 *
 * This model is used both for Firestore storage and UI rendering.
 * It supports both text and image messages.
 */
data class ChatMessage(

    // Unique ID of the message (Firestore document ID)
    val id: String = "",

    // ID of the thread this message belongs to
    val threadId: Int = 0,

    // User ID of the sender (Firebase UID)
    val senderId: String = "",

    // Text content of the message (used for text messages)
    val text: String = "",

    // URL of the image (used for image messages stored in cloud storage)
    val imageUrl: String = "",

    // Defines the type of message (text or image)
    val messageType: String = MESSAGE_TYPE_TEXT,

    // Human-readable time (used for UI display, e.g. "14:32")
    val time: String = "",

    // Timestamp used for sorting messages (epoch time)
    val createdAt: Long = 0L,

    // List of user IDs who have read this message
    // Used to track read receipts in the chat
    val readByUserIds: List<String> = emptyList()
) {

    /**
     * Defines supported message types.
     *
     * Using constants avoids hardcoding strings across the codebase
     * and reduces risk of bugs from typos.
     */
    companion object {
        const val MESSAGE_TYPE_TEXT = "text"
        const val MESSAGE_TYPE_IMAGE = "image"
    }

    /**
     * Helper property to check if this message is a valid image message.
     *
     * We ensure both:
     * - type is "image"
     * - imageUrl is not empty
     *
     * This prevents broken UI states if data is incomplete.
     */
    val isImageMessage: Boolean
        get() = messageType == MESSAGE_TYPE_IMAGE && imageUrl.isNotBlank()
}