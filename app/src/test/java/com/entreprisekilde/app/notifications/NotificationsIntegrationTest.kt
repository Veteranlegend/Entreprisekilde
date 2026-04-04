package com.entreprisekilde.app.notifications

import com.entreprisekilde.app.data.repository.notifications.DemoNotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Basic integration test for the notification system.
 *
 * This test verifies that when a message notification is created,
 * it is correctly stored and returned for the intended user.
 *
 * We are testing the repository as a whole (not mocking it),
 * which gives us confidence that the full flow works end-to-end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsIntegrationTest {

    // Using the demo repository implementation to simulate real behavior.
    private val repository = DemoNotificationRepository()

    @Test
    fun addMessageNotification_shouldBeReturnedForUser() = runTest {
        // Arrange
        // Define a user that will receive the notification.
        val userId = "user1"

        // Act
        // Add a new message notification for the user.
        repository.addMessageNotification(
            senderName = "John",
            recipientUserId = userId,
            threadId = 1
        )

        // Fetch all notifications for that user.
        val notifications = repository.getNotifications(userId)

        // Assert

        // We expect exactly one notification to exist.
        assertEquals(1, notifications.size)

        // The title should match what the repository creates for message notifications.
        assertEquals("New message", notifications.first().title)

        // The message body should include the sender's name.
        assertTrue(notifications.first().message.contains("John"))

        // The notification should belong to the correct user.
        assertEquals(userId, notifications.first().userId)
    }
}