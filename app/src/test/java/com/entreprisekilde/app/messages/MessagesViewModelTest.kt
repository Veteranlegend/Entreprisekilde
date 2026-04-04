package com.entreprisekilde.app.viewmodel

import android.net.Uri
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

    // Test dispatcher used to control coroutine execution deterministically.
    // This makes async ViewModel behavior predictable inside unit tests.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Replace the main dispatcher so ViewModel coroutine work runs on our test dispatcher.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Always restore the original main dispatcher after each test.
        // This avoids side effects leaking into other tests.
        Dispatchers.resetMain()
    }

    @Test
    fun startListeningForUser_loadsAndSortsThreads() = runTest {
        // Fake thread list deliberately created out of order by updatedAt
        // so the test can verify sorting behavior clearly.
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

        // Minimal fake repository that behaves just enough like the real one
        // for this specific test case.
        val fakeRepository = object : MessagesRepository {
            override suspend fun getThreadsForUser(userId: String): List<MessageThread> = fakeThreads

            override fun startThreadsListener(
                userId: String,
                onUpdate: (List<MessageThread>) -> Unit,
                onError: (String) -> Unit
            ): () -> Unit {
                // Simulate an immediate repository update callback.
                onUpdate(fakeThreads)
                return {}
            }

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
            override suspend fun deleteThread(threadId: Int, currentUserId: String) {}
            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun sendImageMessage(threadId: Int, senderId: String, imageUri: Uri) {}

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

        viewModel.startListeningForUser("user1")
        advanceUntilIdle()

        // Threads should be sorted descending by updatedAt:
        // id=2 first, then id=3, then id=1.
        assertEquals(3, viewModel.messageThreads.size)
        assertEquals(2, viewModel.messageThreads[0].id)
        assertEquals(3, viewModel.messageThreads[1].id)
        assertEquals(1, viewModel.messageThreads[2].id)
    }

    @Test
    fun selectThread_setsSelectedThread_andLoadsMessages() = runTest {
        val selected = MessageThread(
            id = 10,
            recipientId = "user2",
            recipientName = "Sara",
            lastMessage = "Hello",
            updatedAt = 1000L
        )

        // Fake messages returned for the selected thread.
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
                // Simulate message listener delivering current thread messages immediately.
                onUpdate(fakeMessages)
                return {}
            }

            override suspend fun markThreadAsRead(threadId: Int, userId: String) {}
            override suspend fun markMessagesAsRead(threadId: Int, userId: String) {}
            override suspend fun setTypingState(threadId: Int, userId: String, isTyping: Boolean) {}
            override suspend fun deleteThread(threadId: Int, currentUserId: String) {}
            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun sendImageMessage(threadId: Int, senderId: String, imageUri: Uri) {}

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

        viewModel.selectThread(selected, "user1")
        advanceUntilIdle()

        // Verifies both selected thread state and loaded messages.
        assertEquals(10, viewModel.selectedThread.value?.id)
        assertEquals(2, viewModel.currentMessages.size)
        assertEquals("Hi", viewModel.currentMessages[0].text)
        assertEquals("Hello back", viewModel.currentMessages[1].text)
    }

    @Test
    fun sendMessage_updatesThreadAndCallsOnSuccess() = runTest {
        val originalThread = MessageThread(
            id = 20,
            recipientId = "user2",
            recipientName = "Sara",
            lastMessage = "Old message",
            updatedAt = 1000L,
            lastMessageSenderId = "user2"
        )

        var successCalled = false

        // This is what the repository will return after the message is sent,
        // simulating the thread's latest server-side state.
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
            override suspend fun deleteThread(threadId: Int, currentUserId: String) {}

            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {
                // No-op: success is implied by not throwing.
            }

            override suspend fun sendImageMessage(threadId: Int, senderId: String, imageUri: Uri) {}

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

        viewModel.sendMessage(
            senderId = "user1",
            message = "New message",
            onSuccess = { successCalled = true }
        )
        advanceUntilIdle()

        // Make sure the callback fired and both selectedThread + thread list were updated.
        assertEquals(true, successCalled)
        assertEquals("New message", viewModel.selectedThread.value?.lastMessage)
        assertEquals("user1", viewModel.selectedThread.value?.lastMessageSenderId)
        assertEquals(20, viewModel.messageThreads[0].id)
        assertEquals("New message", viewModel.messageThreads[0].lastMessage)
    }

    @Test
    fun deleteThread_removesThread_andClearsSelectedState() = runTest {
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
                // Simulate the thread already having one message loaded.
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

            override suspend fun deleteThread(threadId: Int, currentUserId: String) {
                // No-op for successful deletion simulation.
            }

            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun sendImageMessage(threadId: Int, senderId: String, imageUri: Uri) {}

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

        viewModel.startListeningForUser("user1")
        viewModel.messageThreads.add(thread)
        viewModel.selectThread(thread, "user1")
        advanceUntilIdle()

        // Sanity check before deletion.
        assertEquals(1, viewModel.currentMessages.size)
        assertEquals(30, viewModel.selectedThread.value?.id)

        viewModel.deleteThread(thread)
        advanceUntilIdle()

        // After deletion, the thread should disappear everywhere relevant.
        assertTrue(viewModel.messageThreads.none { it.id == 30 })
        assertEquals(null, viewModel.selectedThread.value)
        assertTrue(viewModel.currentMessages.isEmpty())
    }

    @Test
    fun createOrGetThread_addsThread_selectsIt_andCallsOnReady() = runTest {
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
            override suspend fun deleteThread(threadId: Int, currentUserId: String) {}
            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {}
            override suspend fun sendImageMessage(threadId: Int, senderId: String, imageUri: Uri) {}
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

        viewModel.createOrGetThread(
            currentUserId = "user1",
            currentUserName = "Ahmad",
            recipientId = "user2",
            recipientName = "Sara",
            onReady = { readyThread = it }
        )
        advanceUntilIdle()

        // Newly created thread should be inserted, selected, and returned via callback.
        assertEquals(1, viewModel.messageThreads.size)
        assertEquals(40, viewModel.messageThreads[0].id)
        assertEquals(40, viewModel.selectedThread.value?.id)
        assertEquals(40, readyThread?.id)
    }

    @Test
    fun sendMessage_whenRepositoryFails_setsErrorMessage() = runTest {
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
            override suspend fun deleteThread(threadId: Int, currentUserId: String) {}

            override suspend fun sendMessage(threadId: Int, senderId: String, text: String) {
                // Force the failure path so the ViewModel's error handling can be tested.
                throw Exception("Failed to send message.")
            }

            override suspend fun sendImageMessage(threadId: Int, senderId: String, imageUri: Uri) {}

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
        viewModel.startListeningForUser("user1")
        viewModel.messageThreads.add(thread)
        viewModel.selectThread(thread, "user1")
        advanceUntilIdle()

        viewModel.sendMessage(
            senderId = "user1",
            message = "Hello"
        )
        advanceUntilIdle()

        // The ViewModel should expose a user-visible error when sending fails.
        assertEquals("Failed to send message.", viewModel.errorMessage.value)
    }
}