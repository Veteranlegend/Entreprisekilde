package com.entreprisekilde.app.data.repository.tasks

import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import java.util.UUID

class TasksRepository {

    private val tasks = DemoSeedData.createTasks().toMutableList()

    suspend fun getTasks(): List<TaskData> {
        return tasks.toList()
    }

    suspend fun addTask(newTask: TaskData) {
        val taskToInsert = if (newTask.id.isBlank()) {
            newTask.copy(id = UUID.randomUUID().toString())
        } else {
            newTask
        }

        tasks.add(0, taskToInsert)
    }

    suspend fun deleteTask(taskId: String) {
        tasks.removeAll { it.id == taskId }
    }

    suspend fun updateTask(updatedTask: TaskData) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
        }
    }

    suspend fun updateStatus(taskId: String, newStatus: TaskStatus) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            tasks[index] = tasks[index].copy(status = newStatus)
        }
    }
}