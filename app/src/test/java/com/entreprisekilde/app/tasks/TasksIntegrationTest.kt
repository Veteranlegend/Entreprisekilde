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

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DemoTasksRepository
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = DemoTasksRepository()
        viewModel = TasksViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addTask_shouldAppearInViewModelList() = runTest {
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

        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        val exists = viewModel.tasks.any {
            it.taskDetails == "Integration Test Task"
        }

        assertTrue(exists)
    }

    @Test
    fun deleteTask_shouldRemoveTaskFromViewModelList() = runTest {
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

        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        val addedTask = viewModel.tasks.firstOrNull {
            it.taskDetails == "Task To Delete"
        }

        assertTrue(addedTask != null)

        viewModel.deleteTask(addedTask!!.id)
        advanceUntilIdle()

        val stillExists = viewModel.tasks.any {
            it.id == addedTask.id
        }

        assertFalse(stillExists)
    }

    @Test
    fun updateStatus_shouldChangeTaskStatusInViewModelList() = runTest {
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

        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        val addedTask = viewModel.tasks.firstOrNull {
            it.taskDetails == "Task To Update Status"
        }

        assertTrue(addedTask != null)
        assertEquals(TaskStatus.PENDING, addedTask!!.status)

        viewModel.updateStatus(addedTask.id, TaskStatus.COMPLETED)
        advanceUntilIdle()

        val updatedTask = viewModel.tasks.firstOrNull {
            it.id == addedTask.id
        }

        assertTrue(updatedTask != null)
        assertEquals(TaskStatus.COMPLETED, updatedTask!!.status)
    }

    @Test
    fun updateTask_shouldChangeTaskFieldsInViewModelList() = runTest {
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

        viewModel.addTask(newTask, emptyList())
        advanceUntilIdle()

        val addedTask = viewModel.tasks.firstOrNull {
            it.taskDetails == "Original Details"
        }

        assertTrue(addedTask != null)

        val updatedTaskData = addedTask!!.copy(
            customer = "Updated Customer",
            address = "Updated Address",
            taskDetails = "Updated Details"
        )

        viewModel.updateTask(updatedTaskData)
        advanceUntilIdle()

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
        val failingRepository = FailingTasksRepository()
        val failingViewModel = TasksViewModel(failingRepository)

        advanceUntilIdle()

        assertEquals("Failed to load tasks for test", failingViewModel.errorMessage.value)
        assertTrue(failingViewModel.tasks.isEmpty())
        assertFalse(failingViewModel.isLoading.value)
    }

    private class FailingTasksRepository : TasksRepository {

        override suspend fun getTasks(): List<TaskData> {
            throw Exception("Failed to load tasks for test")
        }

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