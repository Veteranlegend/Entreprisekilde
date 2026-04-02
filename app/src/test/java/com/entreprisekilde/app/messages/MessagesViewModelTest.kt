package com.entreprisekilde.app.viewmodel

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.entreprisekilde.app.data.repository.messages.MessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {

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
    fun startListeningForUser_loadsAndSortsThreads() = runTest {
        // Arrange
        val fakeThreads = listOf(
            MessageThread(
                id = 1,
                recipientId = "user2",
                recipientName = "Sara",
                lastMessage = "Old message",
                updatedAt = 1000L
            ),
            MessageThread(
                id = 2,
                recipientId = "user3",
                recipientName = "Ali",
                lastMessage = "New message",
                updatedAt = 3000L
            ),
            MessageThread(
                id = 3,
                recipientId = "user4",
                recipientName = "Mona",
                lastMessage = "Middle message",
                updatedAt = 2000L
            )
        )

        val fakeRepository = object : MessagesRepository {
            override suspend fun getThreadsForUser(userId: String): List<MessageThread> = fakeThreads

            override fun startThreadsListener(
                userId: String,
                onUpdate: (List<MessageThread>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit {
                onUpdate(fakeThreads)
                return {}
            }

            override suspend fun getMessages(threadId: Int): List<ChatMessage> = emptyList()

            override fun startMessagesListener(
                threadId: Int,
                onUpdate: (List<ChatMessage>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun markThreadAsRead(threadId: Int, userId: String) {}
            override suspend fun markMessagesAsRead(threadId: Int, userId: String) {}
            override suspend fun setTypingState(threadId: Int, userId: String, isTyping: Boolean) {}
            override suspend fun deleteThread(threadId: Int) {}
            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun findThreadById(threadId: Int, currentUserId: String): MessageThread? = null

            override suspend fun createOrGetThread(
                currentUserId: String,
                currentUserName: String,
                recipientId: String,
                recipientName: String
            ): MessageThread {
                return MessageThread()
            }
        }

        val viewModel = MessagesViewModel(fakeRepository)

        // Act
        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Assert
        assertEquals(3, viewModel.messageThreads.size)
        assertEquals(2, viewModel.messageThreads[0].id)
        assertEquals(3, viewModel.messageThreads[1].id)
        assertEquals(1, viewModel.messageThreads[2].id)
    }

    @Test
    fun selectThread_setsSelectedThread_andLoadsMessages() = runTest {
        // Arrange
        val selected = MessageThread(
            id = 10,
            recipientId = "user2",
            recipientName = "Sara",
            lastMessage = "Hello",
            updatedAt = 1000L
        )

        val fakeMessages = listOf(
            ChatMessage(
                id = "m1",
                threadId = 10,
                senderId = "user1",
                text = "Hi",
                time = "10:00"
            ),
            ChatMessage(
                id = "m2",
                threadId = 10,
                senderId = "user2",
                text = "Hello back",
                time = "10:01"
            )
        )

        val fakeRepository = object : MessagesRepository {
            override suspend fun getThreadsForUser(userId: String): List<MessageThread> = emptyList()

            override fun startThreadsListener(
                userId: String,
                onUpdate: (List<MessageThread>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun getMessages(threadId: Int): List<ChatMessage> = fakeMessages

            override fun startMessagesListener(
                threadId: Int,
                onUpdate: (List<ChatMessage>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit {
                onUpdate(fakeMessages)
                return {}
            }

            override suspend fun markThreadAsRead(threadId: Int, userId: String) {}
            override suspend fun markMessagesAsRead(threadId: Int, userId: String) {}
            override suspend fun setTypingState(threadId: Int, userId: String, isTyping: Boolean) {}
            override suspend fun deleteThread(threadId: Int) {}
            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun findThreadById(threadId: Int, currentUserId: String): MessageThread? = null

            override suspend fun createOrGetThread(
                currentUserId: String,
                currentUserName: String,
                recipientId: String,
                recipientName: String
            ): MessageThread {
                return MessageThread()
            }
        }

        val viewModel = MessagesViewModel(fakeRepository)

        // Act
        viewModel.selectThread(selected, "user1")
        advanceUntilIdle()

        // Assert
        assertEquals(10, viewModel.selectedThread.value?.id)
        assertEquals(2, viewModel.currentMessages.size)
        assertEquals("Hi", viewModel.currentMessages[0].text)
        assertEquals("Hello back", viewModel.currentMessages[1].text)
    }

    @Test
    fun sendMessage_updatesThreadAndCallsOnSuccess() = runTest {
        // Arrange
        val originalThread = MessageThread(
            id = 20,
            recipientId = "user2",
            recipientName = "Sara",
            lastMessage = "Old message",
            updatedAt = 1000L,
            lastMessageSenderId = "user2"
        )

        var successCalled = false

        val updatedThread = originalThread.copy(
            lastMessage = "New message",
            updatedAt = 5000L,
            lastMessageSenderId = "user1"
        )

        val fakeRepository = object : MessagesRepository {
            override suspend fun getThreadsForUser(userId: String): List<MessageThread> = emptyList()

            override fun startThreadsListener(
                userId: String,
                onUpdate: (List<MessageThread>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun getMessages(threadId: Int): List<ChatMessage> = emptyList()

            override fun startMessagesListener(
                threadId: Int,
                onUpdate: (List<ChatMessage>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun markThreadAsRead(threadId: Int, userId: String) {}
            override suspend fun markMessagesAsRead(threadId: Int, userId: String) {}
            override suspend fun setTypingState(threadId: Int, userId: String, isTyping: Boolean) {}
            override suspend fun deleteThread(threadId: Int) {}

            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}

            override suspend fun findThreadById(threadId: Int, currentUserId: String): MessageThread? {
                return updatedThread
            }

            override suspend fun createOrGetThread(
                currentUserId: String,
                currentUserName: String,
                recipientId: String,
                recipientName: String
            ): MessageThread {
                return MessageThread()
            }
        }

        val viewModel = MessagesViewModel(fakeRepository)

        viewModel.startListeningForUser("user1")
        viewModel.messageThreads.add(originalThread)
        viewModel.selectThread(originalThread, "user1")
        advanceUntilIdle()

        // Act
        viewModel.sendMessage(
            senderId = "user1",
            message = "New message",
            onSuccess = { successCalled = true }
        )
        advanceUntilIdle()

        // Assert
        assertEquals(true, successCalled)
        assertEquals("New message", viewModel.selectedThread.value?.lastMessage)
        assertEquals("user1", viewModel.selectedThread.value?.lastMessageSenderId)
        assertEquals(20, viewModel.messageThreads[0].id)
        assertEquals("New message", viewModel.messageThreads[0].lastMessage)
    }

    @Test
    fun deleteThread_removesThread_andClearsSelectedState() = runTest {
        // Arrange
        val thread = MessageThread(
            id = 30,
            recipientId = "user2",
            recipientName = "Sara",
            lastMessage = "Thread to delete",
            updatedAt = 1000L
        )

        val fakeRepository = object : MessagesRepository {
            override suspend fun getThreadsForUser(userId: String): List<MessageThread> = emptyList()

            override fun startThreadsListener(
                userId: String,
                onUpdate: (List<MessageThread>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun getMessages(threadId: Int): List<ChatMessage> = emptyList()

            override fun startMessagesListener(
                threadId: Int,
                onUpdate: (List<ChatMessage>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit {
                onUpdate(
                    listOf(
                        ChatMessage(
                            id = "m1",
                            threadId = 30,
                            senderId = "user1",
                            text = "Hello",
                            time = "10:00"
                        )
                    )
                )
                return {}
            }

            override suspend fun markThreadAsRead(threadId: Int, userId: String) {}
            override suspend fun markMessagesAsRead(threadId: Int, userId: String) {}
            override suspend fun setTypingState(threadId: Int, userId: String, isTyping: Boolean) {}
            override suspend fun deleteThread(threadId: Int) {}
            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun findThreadById(threadId: Int, currentUserId: String): MessageThread? = null

            override suspend fun createOrGetThread(
                currentUserId: String,
                currentUserName: String,
                recipientId: String,
                recipientName: String
            ): MessageThread {
                return MessageThread()
            }
        }

        val viewModel = MessagesViewModel(fakeRepository)
        viewModel.messageThreads.add(thread)
        viewModel.selectThread(thread, "user1")
        advanceUntilIdle()

        // Sanity check before delete
        assertEquals(1, viewModel.currentMessages.size)
        assertEquals(30, viewModel.selectedThread.value?.id)

        // Act
        viewModel.deleteThread(thread)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.messageThreads.none { it.id == 30 })
        assertEquals(null, viewModel.selectedThread.value)
        assertTrue(viewModel.currentMessages.isEmpty())
    }
    @Test
    fun createOrGetThread_addsThread_selectsIt_andCallsOnReady() = runTest {
        // Arrange
        val createdThread = MessageThread(
            id = 40,
            recipientId = "user2",
            recipientName = "Sara",
            lastMessage = "",
            updatedAt = 4000L
        )

        var readyThread: MessageThread? = null

        val fakeRepository = object : MessagesRepository {
            override suspend fun getThreadsForUser(userId: String): List<MessageThread> = emptyList()

            override fun startThreadsListener(
                userId: String,
                onUpdate: (List<MessageThread>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun getMessages(threadId: Int): List<ChatMessage> = emptyList()

            override fun startMessagesListener(
                threadId: Int,
                onUpdate: (List<ChatMessage>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit {
                onUpdate(emptyList())
                return {}
            }

            override suspend fun markThreadAsRead(threadId: Int, userId: String) {}
            override suspend fun markMessagesAsRead(threadId: Int, userId: String) {}
            override suspend fun setTypingState(threadId: Int, userId: String, isTyping: Boolean) {}
            override suspend fun deleteThread(threadId: Int) {}
            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun findThreadById(threadId: Int, currentUserId: String): MessageThread? = null

            override suspend fun createOrGetThread(
                currentUserId: String,
                currentUserName: String,
                recipientId: String,
                recipientName: String
            ): MessageThread {
                return createdThread
            }
        }

        val viewModel = MessagesViewModel(fakeRepository)

        // Act
        viewModel.createOrGetThread(
            currentUserId = "user1",
            currentUserName = "Ahmad",
            recipientId = "user2",
            recipientName = "Sara",
            onReady = { readyThread = it }
        )
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.messageThreads.size)
        assertEquals(40, viewModel.messageThreads[0].id)
        assertEquals(40, viewModel.selectedThread.value?.id)
        assertEquals(40, readyThread?.id)
    }

    @Test
    fun sendMessage_whenRepositoryFails_setsErrorMessage() = runTest {
        // Arrange
        val thread = MessageThread(
            id = 50,
            recipientId = "user2",
            recipientName = "Sara",
            lastMessage = "Old message",
            updatedAt = 1000L
        )

        val fakeRepository = object : MessagesRepository {
            override suspend fun getThreadsForUser(userId: String): List<MessageThread> = emptyList()

            override fun startThreadsListener(
                userId: String,
                onUpdate: (List<MessageThread>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun getMessages(threadId: Int): List<ChatMessage> = emptyList()

            override fun startMessagesListener(
                threadId: Int,
                onUpdate: (List<ChatMessage>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit = {}

            override suspend fun markThreadAsRead(threadId: Int, userId: String) {}
            override suspend fun markMessagesAsRead(threadId: Int, userId: String) {}
            override suspend fun setTypingState(threadId: Int, userId: String, isTyping: Boolean) {}
            override suspend fun deleteThread(threadId: Int) {}

            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {
                throw Exception("Failed to send message.")
            }

            override suspend fun findThreadById(threadId: Int, currentUserId: String): MessageThread? = null

            override suspend fun createOrGetThread(
                currentUserId: String,
                currentUserName: String,
                recipientId: String,
                recipientName: String
            ): MessageThread {
                return MessageThread()
            }
        }

        val viewModel = MessagesViewModel(fakeRepository)
        viewModel.messageThreads.add(thread)
        viewModel.selectThread(thread, "user1")
        advanceUntilIdle()

        // Act
        viewModel.sendMessage(
            senderId = "user1",
            message = "Hello"
        )
        advanceUntilIdle()

        // Assert
        assertEquals("Failed to send message.", viewModel.errorMessage.value)
    }

}