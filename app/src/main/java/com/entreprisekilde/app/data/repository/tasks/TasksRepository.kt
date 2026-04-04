package com.entreprisekilde.app.data.repository.tasks

import android.net.Uri
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus

/**
 * Contract (interface) for task-related data operations.
 *
 * This defines WHAT the app can do with tasks,
 * but not HOW it is done.
 *
 * Different implementations can exist, for example:
 * - FirebaseTasksRepository (real backend)
 * - DemoTasksRepository (in-memory demo)
 * - Local database (Room, etc.)
 *
 * The rest of the app should depend only on this interface,
 * making it easy to swap implementations without breaking anything.
 */
interface TasksRepository {

    /**
     * Fetch all tasks.
     *
     * This is a one-time fetch (not a live listener).
     * Useful for initial loading or manual refresh.
     */
    suspend fun getTasks(): List<TaskData>

    /**
     * Starts observing tasks in "real time".
     *
     * Implementations should call `onTasksChanged` whenever the task list updates.
     *
     * Examples:
     * - Firebase → snapshot listener
     * - Demo → manual callback triggers
     *
     * `onError` allows the implementation to report issues (network errors, etc.).
     */
    fun startTasksListener(
        onTasksChanged: (List<TaskData>) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Stops the active task listener.
     *
     * Important to avoid:
     * - memory leaks
     * - duplicate listeners
     * - unnecessary updates when UI is not visible
     */
    fun stopTasksListener()

    /**
     * Adds a new task.
     *
     * `imageUris` allows attaching images at creation time.
     * The implementation decides how these are handled (upload, ignore, etc.).
     *
     * Returns the created task (often with an assigned ID).
     */
    suspend fun addTask(
        newTask: TaskData,
        imageUris: List<Uri>
    ): TaskData

    /**
     * Deletes a task by its ID.
     */
    suspend fun deleteTask(taskId: String)

    /**
     * Updates an entire task object.
     *
     * Typically used when multiple fields are edited at once.
     */
    suspend fun updateTask(updatedTask: TaskData)

    /**
     * Updates only the status of a task.
     *
     * This is separated from `updateTask` because status changes are very common
     * and often triggered independently (e.g. "mark as completed").
     */
    suspend fun updateStatus(taskId: String, newStatus: TaskStatus)

    /**
     * Adds images to an existing task.
     *
     * Parameters:
     * - task: the task being updated
     * - imageUris: images to attach
     * - uploadedByUserId / uploadedByName: useful for tracking who added the images
     *
     * Returns the updated task (implementation may modify image list, metadata, etc.).
     */
    suspend fun addImagesToTask(
        task: TaskData,
        imageUris: List<Uri>,
        uploadedByUserId: String,
        uploadedByName: String
    ): TaskData
}