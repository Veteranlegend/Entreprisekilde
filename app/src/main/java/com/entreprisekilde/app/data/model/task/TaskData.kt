package com.entreprisekilde.app.data.model.task

import java.util.UUID

data class TaskImageData(
    val id: String = UUID.randomUUID().toString(),
    val imageUrl: String = "",
    val uploadedByUserId: String = "",
    val uploadedByName: String = "",
    val uploadedAt: Long = 0L,
    val source: TaskImageSource = TaskImageSource.CREATED
)

enum class TaskImageSource {
    CREATED,
    DETAILS
}

data class TaskData(
    val id: String = UUID.randomUUID().toString(),
    val customer: String,
    val phoneNumber: String,
    val address: String,
    val date: String,
    val assignTo: String,
    val assignedUserId: String = "",
    val taskDetails: String,
    val status: TaskStatus = TaskStatus.PENDING,

    // old field kept temporarily so existing UI/repositories do not break
    val imageUrls: List<String> = emptyList(),

    // new structured field for future differentiation
    val images: List<TaskImageData> = emptyList()
)

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}