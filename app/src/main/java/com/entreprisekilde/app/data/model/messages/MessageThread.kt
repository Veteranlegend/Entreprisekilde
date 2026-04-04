package com.entreprisekilde.app.data.model.messages

/**
 * Represents a chat thread (conversation) between users.
 *
 * This model is stored in Firestore and used to display the list of conversations
 * (e.g. inbox view). It contains both UI-friendly data (like names and last message)
 * and system-level data (like unread counts and participants).
 */
data class MessageThread(

    // Unique ID of the thread (Firestore document ID, stored as Int in this app)
    val id: Int = 0,

    // ID of the main recipient (used for UI display in 1-to-1 chats)
    val recipientId: String = "",

    // Display name of the recipient (avoids extra lookups in UI)
    val recipientName: String = "",

    // Last message text shown in the conversation preview
    val lastMessage: String = "",

    // Total unread messages for the CURRENT user (used in UI badges)
    val unreadCount: Int = 0,

    // All users participating in this thread
    val participantIds: List<String> = emptyList(),

    // Mapping of userId -> display name
    // Helps avoid additional database calls when rendering UI
    val participantNames: Map<String, String> = emptyMap(),

    // Per-user unread message count
    // Key = userId, Value = number of unread messages for that user
    val unreadCountByUser: Map<String, Int> = emptyMap(),

    // Timestamp used for sorting threads (most recent first)
    val updatedAt: Long = 0L,

    // ID of the user who sent the last message
    val lastMessageSenderId: String = "",

    // List of users currently typing in this thread
    // Used for real-time "typing..." indicators
    val typingUserIds: List<String> = emptyList(),

    // List of users who have "deleted" this thread (soft delete)
    // Thread still exists in database but is hidden for these users
    val deletedForUserIds: List<String> = emptyList()
)