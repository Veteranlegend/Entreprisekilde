package com.entreprisekilde.app.data.model.task

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

data class TaskData(
    val id: String = "",
    val customer: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val date: String = "",
    val assignTo: String = "",
    val taskDetails: String = "",
    val status: TaskStatus = TaskStatus.PENDING
)