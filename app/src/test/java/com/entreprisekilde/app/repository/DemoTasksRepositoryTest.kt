package com.entreprisekilde.app.repository

import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.repository.tasks.DemoTasksRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTasksRepositoryTest {

    @Test
    fun getTasks_returnsListOfTasks() {
        runBlocking {
            // Arrange
            val repository = DemoTasksRepository()

            // Act
            val tasks = repository.getTasks()

            // Assert
            assertTrue(tasks.isNotEmpty())
        }
    }

    @Test
    fun addTask_addsNewTaskToRepository() {
        runBlocking {
            // Arrange
            val repository = DemoTasksRepository()
            val initialTasks = repository.getTasks()

            val newTask = TaskData(
                id = "test-task-1",
                customer = "Test Customer",
                phoneNumber = "12345678",
                address = "Test Address 1",
                date = "01/04/2026",
                assignTo = "Ahmad",
                taskDetails = "Test details"
            )

            // Act
            repository.addTask(newTask)
            val updatedTasks = repository.getTasks()

            // Assert
            assertEquals(initialTasks.size + 1, updatedTasks.size)
            assertTrue(updatedTasks.any { it.id == "test-task-1" })
        }
    }

    @Test
    fun getTasks_whenAllDeleted_returnsEmptyList() {
        runBlocking {
            // Arrange
            val repository = DemoTasksRepository()

            val tasks = repository.getTasks()
            tasks.forEach { repository.deleteTask(it.id) }

            // Act
            val emptyTasks = repository.getTasks()

            // Assert
            assertTrue(emptyTasks.isEmpty())
        }
    }

    @Test
    fun updateTask_updatesExistingTask() {
        runBlocking {
            // Arrange
            val repository = DemoTasksRepository()

            val originalTask = TaskData(
                id = "update-task",
                customer = "Original Customer",
                phoneNumber = "11111111",
                address = "Original Address",
                date = "01/04/2026",
                assignTo = "Ahmad",
                taskDetails = "Original details",
                status = TaskStatus.PENDING
            )

            repository.addTask(originalTask)

            val updatedTask = originalTask.copy(
                customer = "Updated Customer",
                address = "Updated Address",
                taskDetails = "Updated details",
                status = TaskStatus.COMPLETED
            )

            // Act
            repository.updateTask(updatedTask)
            val resultTask = repository.getTasks().first { it.id == "update-task" }

            // Assert
            assertEquals("Updated Customer", resultTask.customer)
            assertEquals("Updated Address", resultTask.address)
            assertEquals("Updated details", resultTask.taskDetails)
            assertEquals(TaskStatus.COMPLETED, resultTask.status)
        }
    }
}