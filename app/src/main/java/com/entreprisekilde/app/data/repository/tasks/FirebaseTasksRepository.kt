package com.entreprisekilde.app.data.repository.tasks

import android.net.Uri
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskImageData
import com.entreprisekilde.app.data.model.task.TaskImageSource
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase-backed implementation of [TasksRepository].
 *
 * This repository is responsible for:
 * - reading tasks from Firestore
 * - listening for real-time task updates
 * - creating, updating, and deleting tasks
 * - uploading task-related images to Firebase Storage
 *
 * Firestore stores the task metadata, while Firebase Storage stores the actual
 * image files. The repository then connects those two pieces together by saving
 * image URLs back into the task document.
 */
class FirebaseTasksRepository : TasksRepository {

    /**
     * Main Firestore instance used by this repository.
     */
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Firebase Storage instance used for uploading task images.
     */
    private val storage = FirebaseStorage.getInstance()

    /**
     * Reference to the "tasks" collection in Firestore.
     */
    private val tasksCollection = firestore.collection("tasks")

    /**
     * Holds the active real-time Firestore listener for tasks, if one exists.
     *
     * We keep a reference to it so we can remove the listener later and avoid
     * memory leaks or duplicate listeners.
     */
    private var tasksListenerRegistration: ListenerRegistration? = null

    /**
     * Fetches all tasks once from Firestore.
     *
     * This is a one-time read, unlike [startTasksListener] which keeps listening
     * for future changes.
     *
     * If something goes wrong, we fail safely by returning an empty list.
     * That keeps the app from crashing and lets the caller decide how to react.
     */
    override suspend fun getTasks(): List<TaskData> {
        return try {
            val snapshot = tasksCollection.get().await()

            snapshot.documents.mapNotNull { document ->
                documentToTask(document.data ?: emptyMap<String, Any>(), document.id)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Starts a real-time listener for all tasks in Firestore.
     *
     * Before starting a new listener, we stop any existing one so we do not end
     * up with multiple active listeners pointing at the same collection.
     *
     * Whenever Firestore pushes an update, we convert documents into [TaskData]
     * objects and send the result to the caller.
     */
    override fun startTasksListener(
        onTasksChanged: (List<TaskData>) -> Unit,
        onError: (String) -> Unit
    ) {
        // Always clear any previous listener first.
        stopTasksListener()

        tasksListenerRegistration = tasksCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Failed to listen for task updates.")
                return@addSnapshotListener
            }

            // A null snapshot is unusual, but we still handle it defensively.
            if (snapshot == null) {
                onTasksChanged(emptyList())
                return@addSnapshotListener
            }

            val updatedTasks = snapshot.documents.mapNotNull { document ->
                documentToTask(document.data ?: emptyMap<String, Any>(), document.id)
            }

            onTasksChanged(updatedTasks)
        }
    }

    /**
     * Stops the active Firestore task listener, if present.
     *
     * This is important when a screen/view model is destroyed so the app
     * does not keep receiving updates it no longer needs.
     */
    override fun stopTasksListener() {
        tasksListenerRegistration?.remove()
        tasksListenerRegistration = null
    }

    /**
     * Creates a new task and uploads any initial images attached to it.
     *
     * Flow:
     * 1. Create a new Firestore document reference to generate a task ID
     * 2. Upload the provided images to Firebase Storage
     * 3. Build the final [TaskData] object with the uploaded image metadata
     * 4. Save the task document in Firestore
     *
     * Images added during task creation are marked with [TaskImageSource.CREATED].
     */
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

            // Store the full structured image metadata so we can support
            // richer image handling later, not just plain URL lists.
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

    /**
     * Adds extra images to an existing task.
     *
     * These images are treated as detail/progress images rather than the images
     * originally attached at creation time, so they are marked with
     * [TaskImageSource.DETAILS].
     *
     * We merge newly uploaded images into the existing task instead of replacing
     * the old ones.
     */
    override suspend fun addImagesToTask(
        task: TaskData,
        imageUris: List<Uri>,
        uploadedByUserId: String,
        uploadedByName: String
    ): TaskData {
        // No work needed if nothing was passed in.
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

    /**
     * Deletes a task document from Firestore.
     *
     * Note:
     * This currently deletes only the Firestore document, not the uploaded image
     * files in Firebase Storage. If full cleanup is important, storage cleanup
     * should also be added here.
     */
    override suspend fun deleteTask(taskId: String) {
        tasksCollection.document(taskId).delete().await()
    }

    /**
     * Updates the editable fields of an existing task in Firestore.
     *
     * This writes the latest task values and refreshes the updated timestamp.
     */
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

        tasksCollection.document(updatedTask.id)
            .update(data as Map<String, Any>)
            .await()
    }

    /**
     * Updates only the status field of a task.
     *
     * This is useful for quick status changes without having to rewrite the full
     * task object.
     */
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

    /**
     * Converts raw Firestore document data into a [TaskData] object.
     *
     * This method handles both:
     * - the newer structured "images" format
     * - the older legacy "imageUrls" format
     *
     * That fallback behavior is important for backward compatibility, so older
     * task documents still load correctly after the data model evolved.
     */
    private fun documentToTask(
        documentData: Map<String, Any>,
        documentId: String
    ): TaskData? {
        return try {
            // Older task documents may only contain a plain list of URLs.
            val legacyImageUrls =
                (documentData["imageUrls"] as? List<*>)?.filterIsInstance<String>().orEmpty()

            // Newer task documents store richer metadata for each image.
            val structuredImages = (documentData["images"] as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null

                val sourceValue = map["source"] as? String
                val parsedSource = try {
                    TaskImageSource.valueOf(sourceValue ?: TaskImageSource.CREATED.name)
                } catch (_: Exception) {
                    // Fall back safely if the stored source value is invalid.
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

            // Prefer structured images when available. Otherwise, rebuild image
            // objects from the legacy URL list so the rest of the app can still
            // work with one consistent image model.
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
                id = documentId,
                customer = documentData["customer"] as? String ?: "",
                phoneNumber = documentData["phoneNumber"] as? String ?: "",
                address = documentData["address"] as? String ?: "",
                date = documentData["date"] as? String ?: "",
                assignTo = documentData["assignTo"] as? String ?: "",
                assignedUserId = documentData["assignedUserId"] as? String ?: "",
                taskDetails = documentData["taskDetails"] as? String ?: "",
                status = parseStatus(documentData["status"] as? String),
                imageUrls = finalImages.map { it.imageUrl },
                images = finalImages
            )
        } catch (_: Exception) {
            // If a document is malformed, skip it instead of crashing the whole load.
            null
        }
    }

    /**
     * Uploads one or more task images to Firebase Storage and returns their
     * resulting metadata as [TaskImageData].
     *
     * Each file is stored under:
     * task_images/{taskId}/...
     *
     * The generated filename includes:
     * - current timestamp
     * - the image index
     * - a random UUID
     *
     * That combination makes collisions extremely unlikely.
     */
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

            // Only keep successfully uploaded images with a valid URL.
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

    /**
     * Safely parses a raw status string into [TaskStatus].
     *
     * If the value is missing or invalid, we fall back to [TaskStatus.PENDING]
     * so the app still has a valid state to work with.
     */
    private fun parseStatus(status: String?): TaskStatus {
        return try {
            TaskStatus.valueOf(status ?: TaskStatus.PENDING.name)
        } catch (_: Exception) {
            TaskStatus.PENDING
        }
    }
}