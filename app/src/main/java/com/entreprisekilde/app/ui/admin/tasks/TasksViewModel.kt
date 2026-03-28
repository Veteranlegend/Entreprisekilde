package com.entreprisekilde.app.ui.admin.tasks

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.repository.TasksRepository

class TasksViewModel(
    private val tasksRepository: TasksRepository
) : ViewModel() {

    val tasks = tasksRepository.getTasks()

    val selectedTaskIndex = mutableStateOf(-1)

    fun selectTask(index: Int) {
        selectedTaskIndex.value = index
    }

    fun clearSelectedTask() {
        selectedTaskIndex.value = -1
    }

    fun addTask(newTask: TaskData) {
        tasksRepository.addTask(newTask)
    }

    fun deleteTask(taskId: String) {
        tasksRepository.deleteTask(taskId)
    }

    fun updateTask(updatedTask: TaskData) {
        tasksRepository.updateTask(updatedTask)
    }

    fun updateStatus(taskId: String, newStatus: TaskStatus) {
        tasksRepository.updateStatus(taskId, newStatus)
    }
}