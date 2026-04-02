package com.entreprisekilde.app.messages

import com.entreprisekilde.app.data.repository.messages.DemoMessagesRepository
import com.entreprisekilde.app.viewmodel.MessagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DemoMessagesRepository
    private lateinit var viewModel: MessagesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = DemoMessagesRepository()
        viewModel = MessagesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startListeningForUser_shouldLoadMessageThreadsIntoViewModel() = runTest {
        viewModel.startListeningForUser("me")
        advanceUntilIdle()

        assertFalse(viewModel.messageThreads.isEmpty())
        assertEquals(4, viewModel.messageThreads.size)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun selectThread_shouldLoadMessagesIntoCurrentMessages() = runTest {
        viewModel.startListeningForUser("me")
        advanceUntilIdle()

        val thread = viewModel.messageThreads.firstOrNull { it.id == 1 }
        assertTrue(thread != null)

        viewModel.selectThread(thread!!, "me")
        advanceUntilIdle()

        assertEquals(thread.id, viewModel.selectedThread.value?.id)
        assertFalse(viewModel.currentMessages.isEmpty())
        assertEquals(3, viewModel.currentMessages.size)
        assertEquals(1, viewModel.currentMessages.first().threadId)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun sendMessage_shouldUpdateSelectedThreadAndMessages() = runTest {
        // Arrange
        viewModel.startListeningForUser("me")
        advanceUntilIdle()

        val thread = viewModel.messageThreads.firstOrNull { it.id == 1 }
        assertTrue(thread != null)

        viewModel.selectThread(thread!!, "me")
        advanceUntilIdle()

        val initialMessageCount = viewModel.currentMessages.size

        // Act
        viewModel.sendMessage(
            senderId = "me",
            message = "Integration test message"
        )
        advanceUntilIdle()

        // Refresh messages from repository through the listener again
        viewModel.selectThreadById(1, "me")
        advanceUntilIdle()

        // Assert
        assertEquals("Integration test message", viewModel.selectedThread.value?.lastMessage)
        assertEquals(initialMessageCount + 1, viewModel.currentMessages.size)

        val lastMessage = viewModel.currentMessages.last()
        assertEquals("Integration test message", lastMessage.text)
        assertEquals("me", lastMessage.senderId)

        assertNull(viewModel.errorMessage.value)
    }
}