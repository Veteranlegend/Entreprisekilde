package com.entreprisekilde.app.data.model.task

import java.util.UUID

/**
 * Represents a single image attached to a task.
 *
 * This model allows us to store metadata about each image,
 * instead of just storing raw URLs (which was the old approach).
 */
data class TaskImageData(

    // Unique ID for the image (generated locally)
    val id: String = UUID.randomUUID().toString(),

    // URL of the uploaded image (stored in cloud storage)
    val imageUrl: String = "",

    // ID of the user who uploaded the image
    val uploadedByUserId: String = "",

    // Display name of the user who uploaded the image
    val uploadedByName: String = "",

    // Timestamp of when the image was uploaded
    val uploadedAt: Long = 0L,

    // Indicates where the image was added from (creation vs details screen)
    val source: TaskImageSource = TaskImageSource.CREATED
)

/**
 * Defines where a task image originates from.
 *
 * This helps differentiate between:
 * - images added during task creation
 * - images added later from task details
 */
enum class TaskImageSource {
    CREATED,
    DETAILS
}

/**
 * Represents a task in the system.
 *
 * This is a core business model used across:
 * - UI (task screens)
 * - Firestore (storage)
 * - ViewModels (state management)
 */
data class TaskData(

    // Unique ID of the task
    val id: String = UUID.randomUUID().toString(),

    // Customer name related to the task
    val customer: String,

    // Customer phone number
    val phoneNumber: String,

    // Address where the task should be performed
    val address: String,

    // Date of the task (currently stored as String for simplicity)
    val date: String,

    // Name of the assigned user (used for UI display)
    val assignTo: String,

    // ID of the assigned user (used for logic and database relations)
    val assignedUserId: String = "",

    // Detailed description of the task
    val taskDetails: String,

    // Current status of the task
    val status: TaskStatus = TaskStatus.PENDING,

    /**
     * Legacy field (OLD SYSTEM).
     *
     * Previously, images were stored only as a list of URLs.
     * This is kept temporarily to avoid breaking existing code
     * while migrating to the new structured image model.
     */
    val imageUrls: List<String> = emptyList(),

    /**
     * New structured image system.
     *
     * Allows storing:
     * - uploader info
     * - timestamps
     * - source of image
     *
     * This is the preferred field moving forward.
     */
    val images: List<TaskImageData> = emptyList()
)

/**
 * Represents the lifecycle state of a task.
 *
 * Used for:
 * - UI display (e.g. badges, colors)
 * - filtering tasks
 * - business logic (e.g. what actions are allowed)
 */
enum class TaskStatus {

    // Task has been created but not started
    PENDING,

    // Task is currently being worked on
    IN_PROGRESS,

    // Task has been completed
    COMPLETED
}