package com.entreprisekilde.app.data.repository.tasks

import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus

interface TasksRepository {

    suspend fun getTasks(): List<TaskData>

    suspend fun addTask(newTask: TaskData)

    suspend fun deleteTask(taskId: String)

    suspend fun updateTask(updatedTask: TaskData)

    suspend fun updateStatus(taskId: String, newStatus: TaskStatus)
}