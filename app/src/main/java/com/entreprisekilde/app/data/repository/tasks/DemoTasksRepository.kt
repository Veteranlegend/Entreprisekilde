package com.entreprisekilde.app.data.repository.tasks

import android.net.Uri
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import java.util.UUID

/**
 * Simple in-memory demo implementation of [TasksRepository].
 *
 * This repository is useful for:
 * - local development
 * - UI previews / testing
 * - demo builds where Firebase or backend data is not needed
 *
 * Important to remember:
 * this data only lives in memory while the app process is alive.
 * Once the app is restarted, everything goes back to the hardcoded demo state.
 */
class DemoTasksRepository : TasksRepository {

    /**
     * Fake task list used as our local data source.
     *
     * Because this is a mutable list, methods in this repository can add, remove,
     * or update tasks just like a real repository would — only without persistence.
     */
    private val demoTasks = mutableListOf(
        TaskData(
            id = "task-1",
            customer = "John Hansen",
            phoneNumber = "12345678",
            address = "Nørrebrogade 12, København",
            date = "02/04/2026",
            assignTo = "Ahmad El Haj",
            assignedUserId = "user-1",
            taskDetails = "Install a new kitchen sink",
            status = TaskStatus.PENDING
        ),
        TaskData(
            id = "task-2",
            customer = "Sara Nielsen",
            phoneNumber = "87654321",
            address = "Vesterbrogade 25, København",
            date = "03/04/2026",
            assignTo = "Ali Hassan",
            assignedUserId = "user-2",
            taskDetails = "Repair bathroom tiles",
            status = TaskStatus.IN_PROGRESS
        )
    )

    /**
     * Single callback used to simulate "live updates".
     *
     * In a real backend-driven repository this might be a Firestore listener,
     * Flow, LiveData, WebSocket callback, etc. Here we just manually call it
     * whenever the in-memory task list changes.
     */
    private var tasksChangedListener: ((List<TaskData>) -> Unit)? = null

    /**
     * Returns a snapshot copy of the current task list.
     *
     * We return `toList()` instead of the mutable list itself so callers cannot
     * accidentally modify the repository's internal state.
     */
    override suspend fun getTasks(): List<TaskData> {
        return demoTasks.toList()
    }

    /**
     * Starts the demo task listener.
     *
     * Since this repository is purely local, there is no real external data source
     * to listen to. So we simply:
     * 1. store the callback
     * 2. immediately send the current task list
     *
     * `onError` exists because the interface requires it, even though this demo
     * implementation does not currently produce listener errors.
     */
    override fun startTasksListener(
        onTasksChanged: (List<TaskData>) -> Unit,
        onError: (String) -> Unit
    ) {
        tasksChangedListener = onTasksChanged
        onTasksChanged(demoTasks.toList())
    }

    /**
     * Stops the demo listener by clearing the stored callback.
     *
     * This mirrors the cleanup pattern used in real repositories so the calling
     * code can behave the same regardless of data source.
     */
    override fun stopTasksListener() {
        tasksChangedListener = null
    }

    /**
     * Adds a new task to the demo list.
     *
     * If the incoming task does not already have an ID, we generate one locally.
     * That keeps the demo behavior closer to what a real repository would do.
     *
     * `imageUris` is currently ignored here because this demo repository does not
     * actually upload or persist images.
     */
    override suspend fun addTask(
        newTask: TaskData,
        imageUris: List<Uri>
    ): TaskData {
        val taskWithId = newTask.copy(
            id = if (newTask.id.isBlank()) UUID.randomUUID().toString() else newTask.id
        )

        demoTasks.add(taskWithId)
        notifyTasksChanged()

        return taskWithId
    }

    /**
     * Deletes a task by ID.
     *
     * `removeAll` is used here in case duplicate IDs somehow exist in demo data,
     * though ideally task IDs should always be unique.
     */
    override suspend fun deleteTask(taskId: String) {
        demoTasks.removeAll { it.id == taskId }
        notifyTasksChanged()
    }

    /**
     * Replaces an existing task with an updated version.
     *
     * We find the task by matching IDs, then overwrite that specific list entry.
     * If the task is not found, we do nothing.
     */
    override suspend fun updateTask(updatedTask: TaskData) {
        val index = demoTasks.indexOfFirst { it.id == updatedTask.id }

        if (index != -1) {
            demoTasks[index] = updatedTask
            notifyTasksChanged()
        }
    }

    /**
     * Updates only the status of a task.
     *
     * Instead of replacing the whole object manually, we use `copy(...)`
     * to keep all other existing task fields unchanged.
     */
    override suspend fun updateStatus(taskId: String, newStatus: TaskStatus) {
        val index = demoTasks.indexOfFirst { it.id == taskId }

        if (index != -1) {
            demoTasks[index] = demoTasks[index].copy(status = newStatus)
            notifyTasksChanged()
        }
    }

    /**
     * Placeholder implementation for attaching images to a task.
     *
     * Right now this demo repository does not store image data, so we simply
     * return the original task unchanged.
     *
     * This method still exists because the real repository contract supports
     * image uploads, and keeping the same API makes it easy to swap between
     * demo and production implementations.
     */
    override suspend fun addImagesToTask(
        task: TaskData,
        imageUris: List<Uri>,
        uploadedByUserId: String,
        uploadedByName: String
    ): TaskData {
        return task
    }

    /**
     * Notifies the active listener, if one is registered, with a fresh copy
     * of the current task list.
     *
     * We again use `toList()` to avoid exposing the mutable internal list.
     */
    private fun notifyTasksChanged() {
        tasksChangedListener?.invoke(demoTasks.toList())
    }
}