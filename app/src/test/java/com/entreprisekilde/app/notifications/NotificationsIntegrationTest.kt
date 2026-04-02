package com.entreprisekilde.app.notifications

import com.entreprisekilde.app.data.repository.notifications.DemoNotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsIntegrationTest {

    private val repository = DemoNotificationRepository()

    @Test
    fun addMessageNotification_shouldBeReturnedForUser() = runTest {
        // Arrange
        val userId = "user1"

        // Act
        repository.addMessageNotification(
            senderName = "John",
            recipientUserId = userId,
            threadId = 1
        )

        val notifications = repository.getNotifications(userId)

        // Assert
        assertEquals(1, notifications.size)
        assertEquals("New message", notifications.first().title)
        assertTrue(notifications.first().message.contains("John"))
        assertEquals(userId, notifications.first().userId)
    }
}