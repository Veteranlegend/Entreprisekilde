package com.entreprisekilde.app.data.repository.timesheet

import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import java.util.UUID

/**
 * In-memory demo implementation of [TimesheetRepository].
 *
 * This repository uses seeded demo data and keeps everything in memory,
 * meaning:
 * - No persistence (data resets when app restarts)
 * - Great for testing UI and flows quickly
 * - No backend or database required
 *
 * This behaves like a "fake backend" for development purposes.
 */
class DemoTimesheetRepository : TimesheetRepository {

    /**
     * Internal mutable list of timesheet entries.
     *
     * We initialize it with demo data so the app has something to display
     * immediately during development.
     */
    private val timesheetEntries = DemoSeedData.createTimesheetEntries().toMutableList()

    /**
     * Returns all timesheet entries.
     *
     * We return a copy of the list to prevent external code from modifying
     * our internal state directly.
     */
    override suspend fun getEntries(): List<TimesheetEntry> {
        return timesheetEntries.toList()
    }

    /**
     * Returns a sorted list of unique employee names found in the entries.
     *
     * Useful for dropdowns, filters, or grouping UI.
     */
    override suspend fun getEmployees(): List<String> {
        return timesheetEntries
            .map { it.employeeName }     // extract names
            .filter { it.isNotBlank() }  // ignore empty names
            .distinct()                 // remove duplicates
            .sorted()                   // sort alphabetically
    }

    /**
     * Returns all entries belonging to a specific employee.
     *
     * @param employeeName The employee to filter by.
     */
    override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
        return timesheetEntries.filter { it.employeeName == employeeName }
    }

    /**
     * Approves a timesheet entry.
     *
     * Logic detail:
     * - If the employee has submitted hours (> 0), we keep those.
     * - Otherwise, we fall back to assigned hours.
     *
     * This ensures that approval always results in a valid "final" hour value.
     */
    override suspend fun approveEntry(entryId: String) {
        val index = timesheetEntries.indexOfFirst { it.id == entryId }

        if (index != -1) {
            val oldEntry = timesheetEntries[index]

            val submittedHoursToUse =
                if (oldEntry.submittedHours > 0) oldEntry.submittedHours
                else oldEntry.assignedHours

            timesheetEntries[index] = oldEntry.copy(
                submittedHours = submittedHoursToUse,
                approvalStatus = ShiftApprovalStatus.APPROVED
            )
        }
    }

    /**
     * Marks a timesheet entry as declined.
     */
    override suspend fun declineEntry(entryId: String) {
        val index = timesheetEntries.indexOfFirst { it.id == entryId }

        if (index != -1) {
            timesheetEntries[index] = timesheetEntries[index].copy(
                approvalStatus = ShiftApprovalStatus.DECLINED
            )
        }
    }

    /**
     * Resets a timesheet entry back to "pending" state.
     *
     * Useful when undoing an approval or decline decision.
     */
    override suspend fun undoEntryStatus(entryId: String) {
        val index = timesheetEntries.indexOfFirst { it.id == entryId }

        if (index != -1) {
            timesheetEntries[index] = timesheetEntries[index].copy(
                approvalStatus = ShiftApprovalStatus.PENDING
            )
        }
    }

    /**
     * Deletes a timesheet entry completely.
     *
     * We use removeAll for safety, even though IDs are expected to be unique.
     */
    override suspend fun deleteEntry(entryId: String) {
        timesheetEntries.removeAll { it.id == entryId }
    }

    /**
     * Adds a new shift (timesheet entry).
     *
     * If the entry doesn't already have an ID, we generate one using UUID.
     * The new entry is inserted at the top of the list so it appears first
     * in the UI (most recent behavior).
     */
    override suspend fun assignShift(newEntry: TimesheetEntry) {
        val entryToInsert = if (newEntry.id.isBlank()) {
            newEntry.copy(id = UUID.randomUUID().toString())
        } else {
            newEntry
        }

        timesheetEntries.add(0, entryToInsert)
    }
}