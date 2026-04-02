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

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startListeningForUser_loadsNotifications_andUpdatesUnreadCount() = runTest {
        // Arrange
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

        // Act
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Assert
        assertEquals(3, viewModel.notifications.size)
        assertEquals(2, viewModel.unreadCount)
    }


    @Test
    fun markAsRead_marksNotificationAsRead_andUpdatesUnreadCount() = runTest {
        // Arrange
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
                markedId = notificationId
            }

            override suspend fun markAllAsRead(userId: String) {}

            override suspend fun unreadCount(userId: String): Int = 0

            override suspend fun deleteNotification(notificationId: String) {}
        }

        val viewModel = NotificationViewModel(fakeRepository)

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

        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Reset state after listener to make test explicit
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
        // force recount through a user action path
        viewModel.markAsRead("n2")
        advanceUntilIdle()

        // Restore n1 unread for actual test scenario
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
        // recalculate by marking unread one
        // Act
        viewModel.markAsRead("n1")
        advanceUntilIdle()

        // Assert
        assertEquals(true, viewModel.notifications.first { it.id == "n1" }.isRead)
        assertEquals(0, viewModel.unreadCount)
        assertEquals("n1", markedId)
    }

    @Test
    fun markAllAsRead_marksEverythingRead_andResetsUnreadCount() = runTest {
        // Arrange
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
                markedAllForUserId = userId
            }

            override suspend fun unreadCount(userId: String): Int = 0

            override suspend fun deleteNotification(notificationId: String) {}
        }

        val viewModel = NotificationViewModel(fakeRepository)
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

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

        // Act
        viewModel.markAllAsRead()
        advanceUntilIdle()

        // Assert
        assertEquals(true, viewModel.notifications.all { it.isRead })
        assertEquals(0, viewModel.unreadCount)
        assertEquals("user1", markedAllForUserId)
    }


    @Test
    fun deleteNotification_removesItem_updatesUnreadCount_andCallsRepository() = runTest {
        // Arrange
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
                deletedId = notificationId
            }
        }

        val viewModel = NotificationViewModel(fakeRepository)
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

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

        // Act
        viewModel.deleteNotification("n1")
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.notifications.size)
        assertEquals("n2", viewModel.notifications[0].id)
        assertEquals(0, viewModel.unreadCount)
        assertEquals("n1", deletedId)
    }

    @Test
    fun addMessageNotification_forActiveUser_addsLocalNotification_andUpdatesUnreadCount() = runTest {
        // Arrange
        var onDoneCalled = false

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

            override suspend fun deleteNotification(notificationId: String) {}
        }

        val viewModel = NotificationViewModel(fakeRepository)
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Act
        viewModel.addMessageNotification(
            senderName = "Sara",
            recipientUserId = "user1",
            threadId = 99,
            onDone = { onDoneCalled = true }
        )
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.notifications.size)
        assertEquals("New message", viewModel.notifications[0].title)
        assertEquals("Sara sent you a message", viewModel.notifications[0].message)
        assertEquals(NotificationType.MESSAGE, viewModel.notifications[0].type)
        assertEquals(99, viewModel.notifications[0].relatedThreadId)
        assertEquals(false, viewModel.notifications[0].isRead)
        assertEquals(1, viewModel.unreadCount)
        assertEquals(true, onDoneCalled)
    }
}