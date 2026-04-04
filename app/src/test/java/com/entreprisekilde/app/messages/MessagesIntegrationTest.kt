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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesIntegrationTest {

    // Test dispatcher used to control coroutine execution deterministically.
    // This makes async ViewModel/repository behavior much easier to test.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DemoMessagesRepository
    private lateinit var viewModel: MessagesViewModel

    @Before
    fun setup() {
        // Replace the Main dispatcher so ViewModel coroutines run on the test dispatcher
        // instead of the real Android main thread.
        Dispatchers.setMain(testDispatcher)

        // Use the demo repository here so the test runs against predictable in-memory
        // or fake data instead of real backend infrastructure.
        repository = DemoMessagesRepository()
        viewModel = MessagesViewModel(repository)
    }

    @After
    fun tearDown() {
        // Always restore the original Main dispatcher after each test to avoid
        // leaking test configuration into other test classes.
        Dispatchers.resetMain()
    }

    @Test
    fun startListeningForUser_shouldLoadMessageThreadsIntoViewModel() = runTest {
        // Act:
        // Start listening as if user "me" has opened the messages area.
        viewModel.startListeningForUser("me")
        advanceUntilIdle()

        // Assert:
        // We expect threads to be loaded, all belonging to conversations that
        // include the current user, and no error should be present.
        assertFalse(viewModel.messageThreads.isEmpty())
        assertTrue(viewModel.messageThreads.all { it.participantIds.contains("me") })
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun selectThread_shouldLoadMessagesIntoCurrentMessages() = runTest {
        // Arrange:
        // First load the available threads for the current user.
        viewModel.startListeningForUser("me")
        advanceUntilIdle()

        val thread = viewModel.messageThreads.firstOrNull()
        assertNotNull(thread)

        // Act:
        // Select the first available thread and load its messages into the
        // currently opened conversation state.
        viewModel.selectThread(thread!!, "me")
        advanceUntilIdle()

        // Assert:
        // The thread should now be marked as selected, messages should be loaded,
        // all loaded messages should belong to that thread, and there should be
        // no error state.
        assertEquals(thread.id, viewModel.selectedThread.value?.id)
        assertFalse(viewModel.currentMessages.isEmpty())
        assertTrue(viewModel.currentMessages.all { it.threadId == thread.id })
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun sendMessage_shouldUpdateSelectedThreadAndMessages() = runTest {
        // Arrange:
        // Load threads and select one conversation to send a message into.
        viewModel.startListeningForUser("me")
        advanceUntilIdle()

        val thread = viewModel.messageThreads.firstOrNull()
        assertNotNull(thread)

        viewModel.selectThread(thread!!, "me")
        advanceUntilIdle()

        val initialMessageCount = viewModel.currentMessages.size
        val messageText = "Integration test message"

        // Act:
        // Send a new message as the current user.
        viewModel.sendMessage(
            senderId = "me",
            message = messageText
        )
        advanceUntilIdle()

        // Re-select the thread to simulate refreshing from the repository layer
        // and confirm the saved state is actually reflected back into the ViewModel.
        val wasSelected = viewModel.selectThreadById(thread.id, "me")
        advanceUntilIdle()

        // Assert:
        // The thread should still be selectable, the last message preview should
        // match the newly sent text, and the message list should have grown by one.
        assertTrue(wasSelected)
        assertEquals(messageText, viewModel.selectedThread.value?.lastMessage)
        assertEquals(initialMessageCount + 1, viewModel.currentMessages.size)

        val lastMessage = viewModel.currentMessages.last()

        // Verify the actual saved message data is correct.
        assertEquals(messageText, lastMessage.text)
        assertEquals("me", lastMessage.senderId)
        assertEquals(thread.id, lastMessage.threadId)

        assertNull(viewModel.errorMessage.value)
    }
}