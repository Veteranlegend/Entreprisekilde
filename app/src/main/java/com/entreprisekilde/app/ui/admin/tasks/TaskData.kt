package com.entreprisekilde.app.data.model.task

import java.util.UUID

data class TaskData(
    val id: String = UUID.randomUUID().toString(),
    val customer: String,
    val phoneNumber: String,
    val address: String,
    val date: String,
    val assignTo: String,
    val taskDetails: String,
    val status: TaskStatus = TaskStatus.PENDING
)

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}