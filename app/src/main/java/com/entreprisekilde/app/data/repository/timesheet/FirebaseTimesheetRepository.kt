package com.entreprisekilde.app.data.repository.timesheet

import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed implementation of [TimesheetRepository].
 *
 * Handles:
 * - fetching timesheet entries
 * - filtering by employee
 * - approving / declining shifts
 * - assigning new shifts
 * - deleting entries
 *
 * This repository works directly with Firestore documents and manually maps them
 * into [TimesheetEntry] objects.
 */
class FirebaseTimesheetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TimesheetRepository {

    // Firestore collection where all timesheet entries are stored
    private val timesheetCollection = firestore.collection("timesheetEntries")

    /**
     * Fetches all timesheet entries.
     */
    override suspend fun getEntries(): List<TimesheetEntry> {
        val snapshot = timesheetCollection.get().await()

        return snapshot.documents.mapNotNull { document ->
            documentToTimesheetEntry(document.id, document.data)
        }
    }

    /**
     * Returns a unique, sorted list of employee names found in the timesheet entries.
     *
     * Useful for dropdowns / filters in the UI.
     */
    override suspend fun getEmployees(): List<String> {
        val snapshot = timesheetCollection.get().await()

        return snapshot.documents
            .mapNotNull { it.getString("employeeName") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    /**
     * Fetches all timesheet entries for a specific employee.
     */
    override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
        val snapshot = timesheetCollection
            .whereEqualTo("employeeName", employeeName)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            documentToTimesheetEntry(document.id, document.data)
        }
    }

    /**
     * Approves a timesheet entry.
     *
     * Logic detail:
     * - If the employee submitted hours (> 0), we keep those
     * - Otherwise, we fallback to assigned hours
     *
     * This ensures we always store a valid approved hour value.
     */
    override suspend fun approveEntry(entryId: String) {
        val docRef = timesheetCollection.document(entryId)
        val snapshot = docRef.get().await()

        val submittedHours = snapshot.getLong("submittedHours")?.toInt() ?: 0
        val assignedHours = snapshot.getLong("assignedHours")?.toInt() ?: 0

        // Prefer submitted hours if available, otherwise fallback to assigned hours
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

    /**
     * Marks an entry as declined.
     */
    override suspend fun declineEntry(entryId: String) {
        timesheetCollection
            .document(entryId)
            .update("approvalStatus", ShiftApprovalStatus.DECLINED.name)
            .await()
    }

    /**
     * Resets an entry back to PENDING status.
     *
     * Useful if a manager wants to undo a previous decision.
     */
    override suspend fun undoEntryStatus(entryId: String) {
        timesheetCollection
            .document(entryId)
            .update("approvalStatus", ShiftApprovalStatus.PENDING.name)
            .await()
    }

    /**
     * Deletes a timesheet entry permanently.
     */
    override suspend fun deleteEntry(entryId: String) {
        timesheetCollection
            .document(entryId)
            .delete()
            .await()
    }

    /**
     * Creates or updates a shift assignment.
     *
     * If the entry has no ID, we generate a new Firestore document ID.
     * Otherwise, we overwrite/update the existing document.
     */
    override suspend fun assignShift(newEntry: TimesheetEntry) {
        val entryId = if (newEntry.id.isBlank()) {
            timesheetCollection.document().id
        } else {
            newEntry.id
        }

        // Convert the entry into a Firestore-friendly map
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

    /**
     * Converts raw Firestore document data into a [TimesheetEntry].
     *
     * This method is defensive:
     * - If required fields are missing → returns null
     * - If approvalStatus is invalid → defaults to PENDING
     *
     * This prevents crashes from malformed or unexpected Firestore data.
     */
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

        val approvalStatusString =
            data["approvalStatus"] as? String ?: ShiftApprovalStatus.PENDING.name

        val approvalStatus = try {
            ShiftApprovalStatus.valueOf(approvalStatusString)
        } catch (_: Exception) {
            // Fallback if the stored value is invalid or unknown
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