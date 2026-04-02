package com.entreprisekilde.app.ui.admin.tasks

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

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
    fun init_loadsTasksIntoState() = runTest {
        // Arrange
        val repository = DemoTasksRepository()

        // Act
        val viewModel = TasksViewModel(repository)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.tasks.isNotEmpty())
    }

    @Test
    fun addTask_addsTaskToViewModelState() = runTest {
        // Arrange
        val repository = DemoTasksRepository()
        val viewModel = TasksViewModel(repository)
        advanceUntilIdle()

        val initialSize = viewModel.tasks.size

        val newTask = TaskData(
            id = "vm-task-1",
            customer = "ViewModel Customer",
            phoneNumber = "12345678",
            address = "ViewModel Address",
            date = "02/04/2026",
            assignTo = "Ahmad",
            taskDetails = "Created from ViewModel test"
        )

        // Act
        viewModel.addTask(newTask)
        advanceUntilIdle()

        // Assert
        assertEquals(initialSize + 1, viewModel.tasks.size)
        assertTrue(viewModel.tasks.any { it.id == "vm-task-1" })
    }


    @Test
    fun deleteTask_removesTaskFromViewModelState() = runTest {
        // Arrange
        val repository = DemoTasksRepository()
        val viewModel = TasksViewModel(repository)
        advanceUntilIdle()

        val taskToDelete = TaskData(
            id = "vm-delete-task",
            customer = "Delete Customer",
            phoneNumber = "12345678",
            address = "Delete Address",
            date = "03/04/2026",
            assignTo = "Ahmad",
            taskDetails = "Task to delete"
        )

        viewModel.addTask(taskToDelete)
        advanceUntilIdle()

        val sizeBeforeDelete = viewModel.tasks.size

        // Act
        viewModel.deleteTask("vm-delete-task")
        advanceUntilIdle()

        // Assert
        assertEquals(sizeBeforeDelete - 1, viewModel.tasks.size)
        assertTrue(viewModel.tasks.none { it.id == "vm-delete-task" })
    }

    @Test
    fun updateStatus_changesTaskStatusInViewModelState() = runTest {
        // Arrange
        val repository = DemoTasksRepository()
        val viewModel = TasksViewModel(repository)
        advanceUntilIdle()

        val taskToUpdate = TaskData(
            id = "vm-status-task",
            customer = "Status Customer",
            phoneNumber = "12345678",
            address = "Status Address",
            date = "04/04/2026",
            assignTo = "Ahmad",
            taskDetails = "Task for status update"
        )

        viewModel.addTask(taskToUpdate)
        advanceUntilIdle()

        // Act
        viewModel.updateStatus("vm-status-task", com.entreprisekilde.app.data.model.task.TaskStatus.COMPLETED)
        advanceUntilIdle()

        // Assert
        val updatedTask = viewModel.tasks.first { it.id == "vm-status-task" }
        assertEquals(com.entreprisekilde.app.data.model.task.TaskStatus.COMPLETED, updatedTask.status)
    }
    @Test
    fun selectTask_andClearSelectedTask_updateSelectedTaskIndex() {
        // Arrange
        val repository = DemoTasksRepository()
        val viewModel = TasksViewModel(repository)

        // Act
        viewModel.selectTask(2)

        // Assert
        assertEquals(2, viewModel.selectedTaskIndex.value)

        // Act
        viewModel.clearSelectedTask()

        // Assert
        assertEquals(-1, viewModel.selectedTaskIndex.value)
    }

    @Test
    fun addTask_whenRepositoryFails_setsErrorMessage() = runTest {
        // Arrange
        val failingRepository = object : TasksRepository {
            override suspend fun getTasks() = emptyList<TaskData>()

            override suspend fun addTask(newTask: TaskData) {
                throw Exception("Test error")
            }

            override suspend fun deleteTask(taskId: String) {}
            override suspend fun updateTask(updatedTask: TaskData) {}
            override suspend fun updateStatus(taskId: String, newStatus: TaskStatus) {}
        }

        val viewModel = TasksViewModel(failingRepository)

        val newTask = TaskData(
            id = "error-task",
            customer = "Error Customer",
            phoneNumber = "12345678",
            address = "Error Address",
            date = "05/04/2026",
            assignTo = "Ahmad",
            taskDetails = "Error test"
        )

        // Act
        viewModel.addTask(newTask)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.errorMessage.value == "Test error")
    }
}