package com.entreprisekilde.app.viewmodel

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

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    init {
        loadTasks()
    }

    fun selectTask(index: Int) {
        selectedTaskIndex.value = index
    }

    fun clearSelectedTask() {
        selectedTaskIndex.value = -1
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun retryLoadTasks() {
        loadTasks()
    }

    fun addTask(newTask: TaskData) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            try {
                tasksRepository.addTask(newTask)
                refreshTasks()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to add task."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            try {
                tasksRepository.deleteTask(taskId)
                refreshTasks()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to delete task."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateTask(updatedTask: TaskData) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            try {
                tasksRepository.updateTask(updatedTask)
                refreshTasks()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to update task."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateStatus(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            try {
                tasksRepository.updateStatus(taskId, newStatus)
                refreshTasks()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to update status."
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            try {
                tasks.clear()
                tasks.addAll(tasksRepository.getTasks())
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to load tasks."
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun refreshTasks() {
        try {
            errorMessage.value = null
            tasks.clear()
            tasks.addAll(tasksRepository.getTasks())
        } catch (e: Exception) {
            errorMessage.value = e.message ?: "Failed to refresh tasks."
        }
    }
}