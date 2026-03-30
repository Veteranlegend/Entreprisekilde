package com.entreprisekilde.app.data.repository.tasks

import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTasksRepository : TasksRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val tasksCollection = firestore.collection("tasks")

    override suspend fun getTasks(): List<TaskData> {
        return try {
            val snapshot = tasksCollection.get().await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    TaskData(
                        id = doc.id,
                        customer = doc.getString("customer") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        address = doc.getString("address") ?: "",
                        date = doc.getString("date") ?: "",
                        assignTo = doc.getString("assignTo") ?: "",
                        assignedUserId = doc.getString("assignedUserId") ?: "",
                        taskDetails = doc.getString("taskDetails") ?: "",
                        status = parseStatus(doc.getString("status"))
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addTask(newTask: TaskData) {
        val data = hashMapOf(
            "customer" to newTask.customer,
            "phoneNumber" to newTask.phoneNumber,
            "address" to newTask.address,
            "date" to newTask.date,
            "assignTo" to newTask.assignTo,
            "assignedUserId" to newTask.assignedUserId,
            "taskDetails" to newTask.taskDetails,
            "status" to newTask.status.name,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        tasksCollection.add(data).await()
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

    private fun parseStatus(status: String?): TaskStatus {
        return try {
            TaskStatus.valueOf(status ?: TaskStatus.PENDING.name)
        } catch (e: Exception) {
            TaskStatus.PENDING
        }
    }
}