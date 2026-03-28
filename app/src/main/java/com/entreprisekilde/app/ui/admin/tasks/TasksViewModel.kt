package com.entreprisekilde.app.ui.admin.tasks

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.repository.tasks.TasksRepository
import kotlinx.coroutines.launch

class TasksViewModel(
    private val tasksRepository: TasksRepository
) : ViewModel() {

    val tasks = mutableStateListOf<TaskData>()
    val selectedTaskIndex = mutableStateOf(-1)

    init {
        loadTasks()
    }

    fun selectTask(index: Int) {
        selectedTaskIndex.value = index
    }

    fun clearSelectedTask() {
        selectedTaskIndex.value = -1
    }

    fun addTask(newTask: TaskData) {
        viewModelScope.launch {
            tasksRepository.addTask(newTask)
            refreshTasks()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            tasksRepository.deleteTask(taskId)
            refreshTasks()
        }
    }

    fun updateTask(updatedTask: TaskData) {
        viewModelScope.launch {
            tasksRepository.updateTask(updatedTask)
            refreshTasks()
        }
    }

    fun updateStatus(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            tasksRepository.updateStatus(taskId, newStatus)
            refreshTasks()
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            tasks.clear()
            tasks.addAll(tasksRepository.getTasks())
        }
    }

    private suspend fun refreshTasks() {
        tasks.clear()
        tasks.addAll(tasksRepository.getTasks())
    }
}