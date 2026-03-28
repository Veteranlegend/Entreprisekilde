package com.entreprisekilde.app.data.repository

import androidx.compose.runtime.mutableStateListOf
import com.entreprisekilde.app.data.AppDemoData
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus

class TasksRepository {

    private val tasks = mutableStateListOf(*AppDemoData.createTasks().toTypedArray())

    fun getTasks(): List<TaskData> {
        return tasks
    }

    fun addTask(newTask: TaskData) {
        tasks.add(0, newTask)
    }

    fun deleteTask(taskId: String) {
        tasks.removeAll { it.id == taskId }
    }

    fun updateTask(updatedTask: TaskData) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
        }
    }

    fun updateStatus(taskId: String, newStatus: TaskStatus) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            tasks[index] = tasks[index].copy(status = newStatus)
        }
    }
}