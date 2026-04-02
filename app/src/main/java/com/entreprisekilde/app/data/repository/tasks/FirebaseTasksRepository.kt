package com.entreprisekilde.app.data.repository.tasks

import android.net.Uri
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskImageData
import com.entreprisekilde.app.data.model.task.TaskImageSource
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseTasksRepository : TasksRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val tasksCollection = firestore.collection("tasks")

    override suspend fun getTasks(): List<TaskData> {
        return try {
            val snapshot = tasksCollection.get().await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val legacyImageUrls =
                        (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty()

                    val structuredImages = (doc.get("images") as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null

                        val sourceValue = map["source"] as? String
                        val parsedSource = try {
                            TaskImageSource.valueOf(sourceValue ?: TaskImageSource.CREATED.name)
                        } catch (_: Exception) {
                            TaskImageSource.CREATED
                        }

                        TaskImageData(
                            id = map["id"] as? String ?: UUID.randomUUID().toString(),
                            imageUrl = map["imageUrl"] as? String ?: "",
                            uploadedByUserId = map["uploadedByUserId"] as? String ?: "",
                            uploadedByName = map["uploadedByName"] as? String ?: "",
                            uploadedAt = (map["uploadedAt"] as? Number)?.toLong() ?: 0L,
                            source = parsedSource
                        )
                    }.orEmpty()

                    val finalImages = if (structuredImages.isNotEmpty()) {
                        structuredImages
                    } else {
                        legacyImageUrls.map { url ->
                            TaskImageData(
                                id = UUID.randomUUID().toString(),
                                imageUrl = url,
                                uploadedByUserId = "",
                                uploadedByName = "",
                                uploadedAt = 0L,
                                source = TaskImageSource.CREATED
                            )
                        }
                    }

                    TaskData(
                        id = doc.id,
                        customer = doc.getString("customer") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        address = doc.getString("address") ?: "",
                        date = doc.getString("date") ?: "",
                        assignTo = doc.getString("assignTo") ?: "",
                        assignedUserId = doc.getString("assignedUserId") ?: "",
                        taskDetails = doc.getString("taskDetails") ?: "",
                        status = parseStatus(doc.getString("status")),
                        imageUrls = finalImages.map { it.imageUrl },
                        images = finalImages
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun addTask(
        newTask: TaskData,
        imageUris: List<Uri>
    ): TaskData {
        val taskDocRef = tasksCollection.document()
        val taskId = taskDocRef.id
        val now = System.currentTimeMillis()

        val createdImages = uploadTaskImages(
            taskId = taskId,
            imageUris = imageUris,
            source = TaskImageSource.CREATED,
            uploadedByUserId = "",
            uploadedByName = ""
        )

        val taskToSave = newTask.copy(
            id = taskId,
            imageUrls = createdImages.map { it.imageUrl },
            images = createdImages
        )

        val data = hashMapOf(
            "customer" to taskToSave.customer,
            "phoneNumber" to taskToSave.phoneNumber,
            "address" to taskToSave.address,
            "date" to taskToSave.date,
            "assignTo" to taskToSave.assignTo,
            "assignedUserId" to taskToSave.assignedUserId,
            "taskDetails" to taskToSave.taskDetails,
            "status" to taskToSave.status.name,
            "imageUrls" to taskToSave.imageUrls,
            "images" to taskToSave.images.map { image ->
                mapOf(
                    "id" to image.id,
                    "imageUrl" to image.imageUrl,
                    "uploadedByUserId" to image.uploadedByUserId,
                    "uploadedByName" to image.uploadedByName,
                    "uploadedAt" to image.uploadedAt,
                    "source" to image.source.name
                )
            },
            "createdAt" to now,
            "updatedAt" to now
        )

        taskDocRef.set(data).await()
        return taskToSave
    }

    override suspend fun addImagesToTask(
        task: TaskData,
        imageUris: List<Uri>,
        uploadedByUserId: String,
        uploadedByName: String
    ): TaskData {
        if (imageUris.isEmpty()) return task

        val newImages = uploadTaskImages(
            taskId = task.id,
            imageUris = imageUris,
            source = TaskImageSource.DETAILS,
            uploadedByUserId = uploadedByUserId,
            uploadedByName = uploadedByName
        )

        val updatedTask = task.copy(
            imageUrls = task.imageUrls + newImages.map { it.imageUrl },
            images = task.images + newImages
        )

        tasksCollection.document(task.id)
            .update(
                mapOf(
                    "imageUrls" to updatedTask.imageUrls,
                    "images" to updatedTask.images.map { image ->
                        mapOf(
                            "id" to image.id,
                            "imageUrl" to image.imageUrl,
                            "uploadedByUserId" to image.uploadedByUserId,
                            "uploadedByName" to image.uploadedByName,
                            "uploadedAt" to image.uploadedAt,
                            "source" to image.source.name
                        )
                    },
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()

        return updatedTask
    }

    override suspend fun deleteTask(taskId: String) {
        tasksCollection.document(taskId).delete().await()
    }

    override suspend fun updateTask(updatedTask: TaskData) {
        val data = hashMapOf(
            "customer" to updatedTask.customer,
            "phoneNumber" to updatedTask.phoneNumber,
            "address" to updatedTask.address,
            "date" to updatedTask.date,
            "assignTo" to updatedTask.assignTo,
            "assignedUserId" to updatedTask.assignedUserId,
            "taskDetails" to updatedTask.taskDetails,
            "status" to updatedTask.status.name,
            "imageUrls" to updatedTask.imageUrls,
            "images" to updatedTask.images.map { image ->
                mapOf(
                    "id" to image.id,
                    "imageUrl" to image.imageUrl,
                    "uploadedByUserId" to image.uploadedByUserId,
                    "uploadedByName" to image.uploadedByName,
                    "uploadedAt" to image.uploadedAt,
                    "source" to image.source.name
                )
            },
            "updatedAt" to System.currentTimeMillis()
        )

        tasksCollection.document(updatedTask.id).update(data as Map<String, Any>).await()
    }

    override suspend fun updateStatus(taskId: String, newStatus: TaskStatus) {
        tasksCollection.document(taskId)
            .update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    private suspend fun uploadTaskImages(
        taskId: String,
        imageUris: List<Uri>,
        source: TaskImageSource,
        uploadedByUserId: String,
        uploadedByName: String
    ): List<TaskImageData> {
        if (imageUris.isEmpty()) return emptyList()

        val uploadedImages = mutableListOf<TaskImageData>()

        imageUris.forEachIndexed { index, uri ->
            val imageRef = storage.reference
                .child("task_images")
                .child(taskId)
                .child("${System.currentTimeMillis()}_${index}_${UUID.randomUUID()}.jpg")

            imageRef.putFile(uri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            if (downloadUrl.isNotBlank()) {
                uploadedImages.add(
                    TaskImageData(
                        id = UUID.randomUUID().toString(),
                        imageUrl = downloadUrl,
                        uploadedByUserId = uploadedByUserId,
                        uploadedByName = uploadedByName,
                        uploadedAt = System.currentTimeMillis(),
                        source = source
                    )
                )
            }
        }

        return uploadedImages
    }

    private fun parseStatus(status: String?): TaskStatus {
        return try {
            TaskStatus.valueOf(status ?: TaskStatus.PENDING.name)
        } catch (_: Exception) {
            TaskStatus.PENDING
        }
    }
}