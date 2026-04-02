package com.entreprisekilde.app.data.repository.tasks

import android.net.Uri
import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskImageData
import com.entreprisekilde.app.data.model.task.TaskImageSource
import com.entreprisekilde.app.data.model.task.TaskStatus
import java.util.UUID

class DemoTasksRepository : TasksRepository {

    private val tasks = DemoSeedData.createTasks().toMutableList()

    override suspend fun getTasks(): List<TaskData> {
        return tasks.toList()
    }

    override suspend fun addTask(
        newTask: TaskData,
        imageUris: List<Uri>
    ): TaskData {
        val taskId = if (newTask.id.isBlank()) {
            UUID.randomUUID().toString()
        } else {
            newTask.id
        }

        val createdImages = imageUris.mapIndexed { index, _ ->
            TaskImageData(
                id = UUID.randomUUID().toString(),
                imageUrl = "demo://task_images/$taskId/created_${System.currentTimeMillis()}_$index.jpg",
                uploadedByUserId = "",
                uploadedByName = "",
                uploadedAt = System.currentTimeMillis(),
                source = TaskImageSource.CREATED
            )
        }

        val taskToInsert = newTask.copy(
            id = taskId,
            imageUrls = createdImages.map { it.imageUrl },
            images = createdImages
        )

        tasks.add(0, taskToInsert)
        return taskToInsert
    }

    override suspend fun addImagesToTask(
        task: TaskData,
        imageUris: List<Uri>,
        uploadedByUserId: String,
        uploadedByName: String
    ): TaskData {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index == -1) return task

        val newImages = imageUris.mapIndexed { imageIndex, _ ->
            TaskImageData(
                id = UUID.randomUUID().toString(),
                imageUrl = "demo://task_images/${task.id}/details_${System.currentTimeMillis()}_$imageIndex.jpg",
                uploadedByUserId = uploadedByUserId,
                uploadedByName = uploadedByName,
                uploadedAt = System.currentTimeMillis(),
                source = TaskImageSource.DETAILS
            )
        }

        val updatedTask = tasks[index].copy(
            imageUrls = tasks[index].imageUrls + newImages.map { it.imageUrl },
            images = tasks[index].images + newImages
        )

        tasks[index] = updatedTask
        return updatedTask
    }

    override suspend fun deleteTask(taskId: String) {
        tasks.removeAll { it.id == taskId }
    }

    override suspend fun updateTask(updatedTask: TaskData) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
        }
    }

    override suspend fun updateStatus(taskId: String, newStatus: TaskStatus) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            tasks[index] = tasks[index].copy(status = newStatus)
        }
    }
}