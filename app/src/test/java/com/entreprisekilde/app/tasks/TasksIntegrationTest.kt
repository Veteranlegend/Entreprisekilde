package com.entreprisekilde.app.tasks

import android.net.Uri
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.repository.tasks.DemoTasksRepository
import com.entreprisekilde.app.data.repository.tasks.TasksRepository
import com.entreprisekilde.app.viewmodel.TasksViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksIntegrationTest {

    // Test dispatcher lets us fully control coroutine timing in tests.
    // That makes async ViewModel code predictable and easy to verify.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DemoTasksRepository
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setup() {
        // Redirect Main dispatcher usage to our test dispatcher.
        Dispatchers.setMain(testDispatcher)

        // Use the demo repository so the test works with predictable fake/demo data
        // instead of depending on real backend infrastructure.
        repository = DemoTasksRepository()
        viewModel = TasksViewModel(repository)
    }

    @After
    fun tearDown() {
        // Restore Main dispatcher after each test to keep test isolation clean.
        Dispatchers.resetMain()
    }

    @Test
    fun addTask_shouldAppearInViewModelList() = runTest {
        // Let init/startTasksListener finish before starting assertions.
        advanceUntilIdle()

        val newTask = TaskData(
            customer = "Test Customer",
            phoneNumber = "12345678",
            address = "Test Address",
            date = "01-01-2026",
            assignTo = "John",
            taskDetails = "Integration Test Task",
            status = TaskStatus.PENDING
        )

        // Act: add a new task through the ViewModel.
        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        // Assert: the task should now exist in the observable ViewModel list.
        val exists = viewModel.tasks.any {
            it.taskDetails == "Integration Test Task"
        }

        assertTrue(exists)
    }

    @Test
    fun deleteTask_shouldRemoveTaskFromViewModelList() = runTest {
        // Allow initial loading/listening to complete first.
        advanceUntilIdle()

        val newTask = TaskData(
            customer = "Delete Customer",
            phoneNumber = "87654321",
            address = "Delete Address",
            date = "02-01-2026",
            assignTo = "Ali",
            taskDetails = "Task To Delete",
            status = TaskStatus.PENDING
        )

        // Arrange: create a task we can delete.
        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        val addedTask = viewModel.tasks.firstOrNull {
            it.taskDetails == "Task To Delete"
        }

        assertTrue(addedTask != null)

        // Act: delete the task by id.
        viewModel.deleteTask(addedTask!!.id)
        advanceUntilIdle()

        // Assert: it should no longer be present in the ViewModel state.
        val stillExists = viewModel.tasks.any {
            it.id == addedTask.id
        }

        assertFalse(stillExists)
    }

    @Test
    fun updateStatus_shouldChangeTaskStatusInViewModelList() = runTest {
        // Allow initial repository listener/setup to settle.
        advanceUntilIdle()

        val newTask = TaskData(
            customer = "Status Customer",
            phoneNumber = "11112222",
            address = "Status Address",
            date = "03-01-2026",
            assignTo = "Omar",
            taskDetails = "Task To Update Status",
            status = TaskStatus.PENDING
        )

        // Arrange: add a task with a known starting status.
        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        val addedTask = viewModel.tasks.firstOrNull {
            it.taskDetails == "Task To Update Status"
        }

        assertTrue(addedTask != null)
        assertEquals(TaskStatus.PENDING, addedTask!!.status)

        // Act: change the task status.
        viewModel.updateStatus(addedTask.id, TaskStatus.COMPLETED)
        advanceUntilIdle()

        // Assert: the same task should now reflect the new status.
        val updatedTask = viewModel.tasks.firstOrNull {
            it.id == addedTask.id
        }

        assertTrue(updatedTask != null)
        assertEquals(TaskStatus.COMPLETED, updatedTask!!.status)
    }

    @Test
    fun updateTask_shouldChangeTaskFieldsInViewModelList() = runTest {
        // Let the initial task listener finish first.
        advanceUntilIdle()

        val newTask = TaskData(
            customer = "Original Customer",
            phoneNumber = "55556666",
            address = "Original Address",
            date = "04-01-2026",
            assignTo = "Hasan",
            taskDetails = "Original Details",
            status = TaskStatus.PENDING
        )

        // Arrange: add the original version of the task.
        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        val addedTask = viewModel.tasks.firstOrNull {
            it.taskDetails == "Original Details"
        }

        assertTrue(addedTask != null)

        // Build an updated copy with a few changed fields.
        val updatedTaskData = addedTask!!.copy(
            customer = "Updated Customer",
            address = "Updated Address",
            taskDetails = "Updated Details"
        )

        // Act: save the updated task.
        viewModel.updateTask(updatedTaskData)
        advanceUntilIdle()

        // Assert: verify the list now reflects the updated values.
        val updatedTask = viewModel.tasks.firstOrNull {
            it.id == addedTask.id
        }

        assertTrue(updatedTask != null)
        assertEquals("Updated Customer", updatedTask!!.customer)
        assertEquals("Updated Address", updatedTask.address)
        assertEquals("Updated Details", updatedTask.taskDetails)
    }

    @Test
    fun init_whenRepositoryFails_shouldSetErrorMessage() = runTest {
        // Use a purposely failing repository to verify the ViewModel's error handling
        // during startup/listener registration.
        val failingRepository = FailingTasksRepository()
        val failingViewModel = TasksViewModel(failingRepository)

        advanceUntilIdle()

        // The ViewModel should expose the listener error, keep the list empty,
        // and stop showing a loading state.
        assertEquals("Failed to listen to tasks for test", failingViewModel.errorMessage.value)
        assertTrue(failingViewModel.tasks.isEmpty())
        assertFalse(failingViewModel.isLoading.value)
    }

    // Minimal fake repository that fails on purpose.
    // This is useful for testing the unhappy path without needing mocking libraries.
    private class FailingTasksRepository : TasksRepository {

        override suspend fun getTasks(): List<TaskData> {
            throw Exception("Failed to load tasks for test")
        }

        override fun startTasksListener(
            onTasksChanged: (List<TaskData>) -> Unit,
            onError: (String) -> Unit
        ) {
            onError("Failed to listen to tasks for test")
        }

        override fun stopTasksListener() {}

        override suspend fun addTask(
            newTask: TaskData,
            imageUris: List<Uri>
        ): TaskData {
            throw Exception("Not needed in this test")
        }

        override suspend fun deleteTask(taskId: String) {
            throw Exception("Not needed in this test")
        }

        override suspend fun updateTask(updatedTask: TaskData) {
            throw Exception("Not needed in this test")
        }

        override suspend fun updateStatus(taskId: String, newStatus: TaskStatus) {
            throw Exception("Not needed in this test")
        }

        override suspend fun addImagesToTask(
            task: TaskData,
            imageUris: List<Uri>,
            uploadedByUserId: String,
            uploadedByName: String
        ): TaskData {
            throw Exception("Not needed in this test")
        }
    }
}