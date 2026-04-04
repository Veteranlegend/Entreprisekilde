package com.entreprisekilde.app.repository

import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.repository.tasks.DemoTasksRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DemoTasksRepository.
 *
 * These tests verify basic CRUD behavior:
 * - Fetching tasks
 * - Adding tasks
 * - Deleting tasks
 * - Updating tasks
 *
 * Note: Uses runBlocking since repository functions are suspend functions.
 */
class DemoTasksRepositoryTest {

    /**
     * Ensures that the repository returns a non-empty list of tasks.
     *
     * This test assumes that DemoTasksRepository comes with pre-seeded data.
     * If this ever fails, it likely means:
     * - The demo data was removed
     * - Or repository initialization changed
     */
    @Test
    fun getTasks_returnsListOfTasks() {
        runBlocking {
            val repository = DemoTasksRepository()

            val tasks = repository.getTasks()

            assertTrue(tasks.isNotEmpty())
        }
    }

    /**
     * Verifies that adding a task:
     * - Increases total task count
     * - Actually stores the new task correctly
     */
    @Test
    fun addTask_addsNewTaskToRepository() {
        runBlocking {
            val repository = DemoTasksRepository()

            // Capture initial state
            val initialTasks = repository.getTasks()

            // Create a new test task
            val newTask = TaskData(
                id = "test-task-1",
                customer = "Test Customer",
                phoneNumber = "12345678",
                address = "Test Address 1",
                date = "01/04/2026",
                assignTo = "Ahmad",
                taskDetails = "Test details"
            )

            // Add task
            repository.addTask(newTask, emptyList())

            val updatedTasks = repository.getTasks()

            // Verify size increased
            assertEquals(initialTasks.size + 1, updatedTasks.size)

            // Verify task exists in repository
            assertTrue(updatedTasks.any { it.id == "test-task-1" })
        }
    }

    /**
     * Ensures that after deleting all tasks,
     * the repository returns an empty list.
     *
     * This protects against:
     * - Failed deletions
     * - State not updating correctly
     */
    @Test
    fun getTasks_whenAllDeleted_returnsEmptyList() {
        runBlocking {
            val repository = DemoTasksRepository()

            val tasks = repository.getTasks()

            // Delete every task
            tasks.forEach { repository.deleteTask(it.id) }

            val emptyTasks = repository.getTasks()

            assertTrue(emptyTasks.isEmpty())
        }
    }

    /**
     * Verifies that updating a task:
     * - Replaces the existing task (not duplicates it)
     * - Correctly updates all modified fields
     *
     * This is important for ensuring edit flows in the app work properly.
     */
    @Test
    fun updateTask_updatesExistingTask() {
        runBlocking {
            val repository = DemoTasksRepository()

            // Create original task
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

            repository.addTask(originalTask, emptyList())

            // Create updated version of the same task
            val updatedTask = originalTask.copy(
                customer = "Updated Customer",
                address = "Updated Address",
                taskDetails = "Updated details",
                status = TaskStatus.COMPLETED
            )

            // Perform update
            repository.updateTask(updatedTask)

            // Fetch updated task from repository
            val resultTask = repository.getTasks()
                .first { it.id == "update-task" }

            // Verify all fields were updated correctly
            assertEquals("Updated Customer", resultTask.customer)
            assertEquals("Updated Address", resultTask.address)
            assertEquals("Updated details", resultTask.taskDetails)
            assertEquals(TaskStatus.COMPLETED, resultTask.status)
        }
    }
}