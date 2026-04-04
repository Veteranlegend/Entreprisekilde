package com.entreprisekilde.app.viewmodel

import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.entreprisekilde.app.data.repository.notifications.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    // Test dispatcher gives us full control over coroutine timing in unit tests.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Replace Dispatchers.Main so ViewModel coroutines run on the test dispatcher.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Always restore the original Main dispatcher after each test.
        Dispatchers.resetMain()
    }

    @Test
    fun startListeningForUser_loadsNotifications_andUpdatesUnreadCount() = runTest {
        // Fake notification data returned by the repository listener.
        val fakeNotifications = listOf(
            AppNotification(
                id = "n1",
                title = "New message",
                message = "Sara sent you a message",
                type = NotificationType.MESSAGE,
                createdAt = 1000L,
                isRead = false,
                relatedThreadId = 1,
                userId = "user1"
            ),
            AppNotification(
                id = "n2",
                title = "Task assigned",
                message = "You were assigned a task",
                type = NotificationType.TASK_ASSIGNED,
                createdAt = 2000L,
                isRead = true,
                userId = "user1"
            ),
            AppNotification(
                id = "n3",
                title = "New message",
                message = "Ali sent you a message",
                type = NotificationType.MESSAGE,
                createdAt = 3000L,
                isRead = false,
                relatedThreadId = 2,
                userId = "user1"
            )
        )

        val fakeRepository = object : NotificationRepository {
            override suspend fun getNotifications(userId: String): List<AppNotification> = fakeNotifications

            override fun observeNotifications(
                userId: String,
                onChanged: (List<AppNotification>) -> Unit,
                onError: (Exception) -> Unit
            ) {
                // Simulate a repository listener immediately returning data.
                onChanged(fakeNotifications)
            }

            override fun removeNotificationListener() {}

            override suspend fun addMessageNotification(
                senderName: String,
                recipientUserId: String,
                threadId: Int
            ) {}

            override suspend fun addTaskAssignedNotification(
                taskName: String,
                assignedUserId: String,
                assignedToName: String
            ) {}

            override suspend fun markAsRead(notificationId: String) {}

            override suspend fun markAllAsRead(userId: String) {}

            override suspend fun unreadCount(userId: String): Int = 0

            override suspend fun deleteNotification(notificationId: String) {}
        }

        val viewModel = NotificationViewModel(fakeRepository)

        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Notifications should be sorted newest first by createdAt.
        assertEquals(3, viewModel.notifications.size)
        assertEquals("n3", viewModel.notifications[0].id)
        assertEquals("n2", viewModel.notifications[1].id)
        assertEquals("n1", viewModel.notifications[2].id)

        // Two of the three notifications are unread.
        assertEquals(2, viewModel.unreadCount)
    }

    @Test
    fun markAsRead_marksNotificationAsRead_andUpdatesUnreadCount() = runTest {
        var markedId: String? = null

        val fakeRepository = object : NotificationRepository {
            override suspend fun getNotifications(userId: String): List<AppNotification> = emptyList()

            override fun observeNotifications(
                userId: String,
                onChanged: (List<AppNotification>) -> Unit,
                onError: (Exception) -> Unit
            ) {}

            override fun removeNotificationListener() {}

            override suspend fun addMessageNotification(
                senderName: String,
                recipientUserId: String,
                threadId: Int
            ) {}

            override suspend fun addTaskAssignedNotification(
                taskName: String,
                assignedUserId: String,
                assignedToName: String
            ) {}

            override suspend fun markAsRead(notificationId: String) {
                // Capture which id the ViewModel asked the repository to mark as read.
                markedId = notificationId
            }

            override suspend fun markAllAsRead(userId: String) {}

            override suspend fun unreadCount(userId: String): Int = 0

            override suspend fun deleteNotification(notificationId: String) {}
        }

        val viewModel = NotificationViewModel(fakeRepository)
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Seed the ViewModel with a mix of unread and already-read notifications.
        viewModel.notifications.clear()
        viewModel.notifications.addAll(
            listOf(
                AppNotification(
                    id = "n1",
                    title = "New message",
                    message = "Unread notification",
                    type = NotificationType.MESSAGE,
                    isRead = false,
                    userId = "user1"
                ),
                AppNotification(
                    id = "n2",
                    title = "Task assigned",
                    message = "Already read",
                    type = NotificationType.TASK_ASSIGNED,
                    isRead = true,
                    userId = "user1"
                )
            )
        )

        viewModel.markAsRead("n1")
        advanceUntilIdle()

        // The local state should reflect the change immediately after the operation completes.
        assertEquals(true, viewModel.notifications.first { it.id == "n1" }.isRead)
        assertEquals(0, viewModel.unreadCount)
        assertEquals("n1", markedId)
    }

    @Test
    fun markAllAsRead_marksEverythingRead_andResetsUnreadCount() = runTest {
        var markedAllForUserId: String? = null

        val fakeRepository = object : NotificationRepository {
            override suspend fun getNotifications(userId: String): List<AppNotification> = emptyList()

            override fun observeNotifications(
                userId: String,
                onChanged: (List<AppNotification>) -> Unit,
                onError: (Exception) -> Unit
            ) {}

            override fun removeNotificationListener() {}

            override suspend fun addMessageNotification(
                senderName: String,
                recipientUserId: String,
                threadId: Int
            ) {}

            override suspend fun addTaskAssignedNotification(
                taskName: String,
                assignedUserId: String,
                assignedToName: String
            ) {}

            override suspend fun markAsRead(notificationId: String) {}

            override suspend fun markAllAsRead(userId: String) {
                // Capture the user id used for the bulk mark-as-read action.
                markedAllForUserId = userId
            }

            override suspend fun unreadCount(userId: String): Int = 0

            override suspend fun deleteNotification(notificationId: String) {}
        }

        val viewModel = NotificationViewModel(fakeRepository)
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Start with two unread notifications and one already-read notification.
        viewModel.notifications.clear()
        viewModel.notifications.addAll(
            listOf(
                AppNotification(
                    id = "n1",
                    title = "New message",
                    message = "Unread 1",
                    type = NotificationType.MESSAGE,
                    isRead = false,
                    userId = "user1"
                ),
                AppNotification(
                    id = "n2",
                    title = "Task assigned",
                    message = "Unread 2",
                    type = NotificationType.TASK_ASSIGNED,
                    isRead = false,
                    userId = "user1"
                ),
                AppNotification(
                    id = "n3",
                    title = "Old",
                    message = "Already read",
                    type = NotificationType.MESSAGE,
                    isRead = true,
                    userId = "user1"
                )
            )
        )

        viewModel.markAllAsRead()
        advanceUntilIdle()

        // Everything should now be marked read, both visually and in unread count.
        assertEquals(true, viewModel.notifications.all { it.isRead })
        assertEquals(0, viewModel.unreadCount)
        assertEquals("user1", markedAllForUserId)
    }

    @Test
    fun deleteNotification_removesItem_updatesUnreadCount_andCallsRepository() = runTest {
        var deletedId: String? = null

        val fakeRepository = object : NotificationRepository {
            override suspend fun getNotifications(userId: String): List<AppNotification> = emptyList()

            override fun observeNotifications(
                userId: String,
                onChanged: (List<AppNotification>) -> Unit,
                onError: (Exception) -> Unit
            ) {}

            override fun removeNotificationListener() {}

            override suspend fun addMessageNotification(
                senderName: String,
                recipientUserId: String,
                threadId: Int
            ) {}

            override suspend fun addTaskAssignedNotification(
                taskName: String,
                assignedUserId: String,
                assignedToName: String
            ) {}

            override suspend fun markAsRead(notificationId: String) {}

            override suspend fun markAllAsRead(userId: String) {}

            override suspend fun unreadCount(userId: String): Int = 0

            override suspend fun deleteNotification(notificationId: String) {
                // Capture the id so we can verify the correct notification was deleted.
                deletedId = notificationId
            }
        }

        val viewModel = NotificationViewModel(fakeRepository)
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // One unread and one read notification before deletion.
        viewModel.notifications.clear()
        viewModel.notifications.addAll(
            listOf(
                AppNotification(
                    id = "n1",
                    title = "Unread",
                    message = "Unread notification",
                    type = NotificationType.MESSAGE,
                    isRead = false,
                    userId = "user1"
                ),
                AppNotification(
                    id = "n2",
                    title = "Read",
                    message = "Read notification",
                    type = NotificationType.TASK_ASSIGNED,
                    isRead = true,
                    userId = "user1"
                )
            )
        )

        viewModel.deleteNotification("n1")
        advanceUntilIdle()

        // After deleting the unread item, only the read one should remain.
        assertEquals(1, viewModel.notifications.size)
        assertEquals("n2", viewModel.notifications[0].id)
        assertEquals(0, viewModel.unreadCount)
        assertEquals("n1", deletedId)
    }

    @Test
    fun onNotificationsOpened_marksAllLocalNotificationsAsRead_andResetsUnreadCount() = runTest {
        var markedAllForUserId: String? = null

        val fakeRepository = object : NotificationRepository {
            override suspend fun getNotifications(userId: String): List<AppNotification> = emptyList()

            override fun observeNotifications(
                userId: String,
                onChanged: (List<AppNotification>) -> Unit,
                onError: (Exception) -> Unit
            ) {}

            override fun removeNotificationListener() {}

            override suspend fun addMessageNotification(
                senderName: String,
                recipientUserId: String,
                threadId: Int
            ) {}

            override suspend fun addTaskAssignedNotification(
                taskName: String,
                assignedUserId: String,
                assignedToName: String
            ) {}

            override suspend fun markAsRead(notificationId: String) {}

            override suspend fun markAllAsRead(userId: String) {
                // Capture whether the ViewModel asked the repository to mark all notifications read.
                markedAllForUserId = userId
            }

            override suspend fun unreadCount(userId: String): Int = 0

            override suspend fun deleteNotification(notificationId: String) {}
        }

        val viewModel = NotificationViewModel(fakeRepository)
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Seed unread notifications to simulate opening the notifications screen.
        viewModel.notifications.clear()
        viewModel.notifications.addAll(
            listOf(
                AppNotification(
                    id = "n1",
                    title = "Unread 1",
                    message = "Message 1",
                    type = NotificationType.MESSAGE,
                    isRead = false,
                    userId = "user1"
                ),
                AppNotification(
                    id = "n2",
                    title = "Unread 2",
                    message = "Message 2",
                    type = NotificationType.TASK_ASSIGNED,
                    isRead = false,
                    userId = "user1"
                )
            )
        )

        viewModel.onNotificationsOpened()
        advanceUntilIdle()

        // Opening the screen should clear unread state locally and in the repository layer.
        assertEquals(true, viewModel.notifications.all { it.isRead })
        assertEquals(0, viewModel.unreadCount)
        assertEquals("user1", markedAllForUserId)
    }
}