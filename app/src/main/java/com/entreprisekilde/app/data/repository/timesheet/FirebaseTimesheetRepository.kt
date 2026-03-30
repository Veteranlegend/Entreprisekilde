package com.entreprisekilde.app.data.repository.timesheet

import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTimesheetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TimesheetRepository {

    private val timesheetCollection = firestore.collection("timesheetEntries")

    override suspend fun getEntries(): List<TimesheetEntry> {
        val snapshot = timesheetCollection.get().await()

        return snapshot.documents.mapNotNull { document ->
            documentToTimesheetEntry(document.id, document.data)
        }
    }

    override suspend fun getEmployees(): List<String> {
        val snapshot = timesheetCollection.get().await()

        return snapshot.documents
            .mapNotNull { it.getString("employeeName") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
        val snapshot = timesheetCollection
            .whereEqualTo("employeeName", employeeName)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            documentToTimesheetEntry(document.id, document.data)
        }
    }

    override suspend fun approveEntry(entryId: String) {
        val docRef = timesheetCollection.document(entryId)
        val snapshot = docRef.get().await()

        val submittedHours = snapshot.getLong("submittedHours")?.toInt() ?: 0
        val assignedHours = snapshot.getLong("assignedHours")?.toInt() ?: 0

        val submittedHoursToUse = if (submittedHours > 0) {
            submittedHours
        } else {
            assignedHours
        }

        docRef.update(
            mapOf(
                "submittedHours" to submittedHoursToUse,
                "approvalStatus" to ShiftApprovalStatus.APPROVED.name
            )
        ).await()
    }

    override suspend fun declineEntry(entryId: String) {
        timesheetCollection
            .document(entryId)
            .update("approvalStatus", ShiftApprovalStatus.DECLINED.name)
            .await()
    }

    override suspend fun undoEntryStatus(entryId: String) {
        timesheetCollection
            .document(entryId)
            .update("approvalStatus", ShiftApprovalStatus.PENDING.name)
            .await()
    }

    override suspend fun deleteEntry(entryId: String) {
        timesheetCollection
            .document(entryId)
            .delete()
            .await()
    }

    override suspend fun assignShift(newEntry: TimesheetEntry) {
        val entryId = if (newEntry.id.isBlank()) {
            timesheetCollection.document().id
        } else {
            newEntry.id
        }

        val data = mapOf(
            "date" to newEntry.date,
            "fromTime" to newEntry.fromTime,
            "toTime" to newEntry.toTime,
            "employeeName" to newEntry.employeeName,
            "submittedHours" to newEntry.submittedHours,
            "assignedHours" to newEntry.assignedHours,
            "approvalStatus" to newEntry.approvalStatus.name
        )

        timesheetCollection
            .document(entryId)
            .set(data)
            .await()
    }

    private fun documentToTimesheetEntry(
        documentId: String,
        data: Map<String, Any>?
    ): TimesheetEntry? {
        if (data == null) return null

        val date = data["date"] as? String ?: return null
        val fromTime = data["fromTime"] as? String ?: return null
        val toTime = data["toTime"] as? String ?: return null
        val employeeName = data["employeeName"] as? String ?: return null
        val submittedHours = (data["submittedHours"] as? Number)?.toInt() ?: 0
        val assignedHours = (data["assignedHours"] as? Number)?.toInt() ?: 0
        val approvalStatusString = data["approvalStatus"] as? String ?: ShiftApprovalStatus.PENDING.name

        val approvalStatus = try {
            ShiftApprovalStatus.valueOf(approvalStatusString)
        } catch (_: Exception) {
            ShiftApprovalStatus.PENDING
        }

        return TimesheetEntry(
            id = documentId,
            date = date,
            fromTime = fromTime,
            toTime = toTime,
            employeeName = employeeName,
            submittedHours = submittedHours,
            assignedHours = assignedHours,
            approvalStatus = approvalStatus
        )
    }
}