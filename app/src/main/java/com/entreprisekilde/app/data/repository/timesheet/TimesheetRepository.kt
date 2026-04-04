package com.entreprisekilde.app.data.repository.timesheet

import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry

/**
 * Contract for all timesheet-related operations in the app.
 *
 * This repository defines how the app interacts with timesheet data,
 * regardless of where that data comes from (demo/in-memory, Firebase,
 * REST API, local database, etc.).
 *
 * The goal is to keep the rest of the app decoupled from the data source.
 */
interface TimesheetRepository {

    /**
     * Fetches all timesheet entries.
     *
     * Typically used for admin views, dashboards, or full overviews.
     *
     * @return A list of all timesheet entries.
     */
    suspend fun getEntries(): List<TimesheetEntry>

    /**
     * Returns a list of unique employee names.
     *
     * Useful for filters, dropdowns, or grouping entries by employee.
     *
     * @return A sorted list of employee names.
     */
    suspend fun getEmployees(): List<String>

    /**
     * Fetches all timesheet entries for a specific employee.
     *
     * @param employeeName The employee to filter entries by.
     * @return A list of entries belonging to that employee.
     */
    suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry>

    /**
     * Approves a specific timesheet entry.
     *
     * The implementation may also finalize submitted hours as part of approval.
     *
     * @param entryId The ID of the entry to approve.
     */
    suspend fun approveEntry(entryId: String)

    /**
     * Declines a specific timesheet entry.
     *
     * @param entryId The ID of the entry to decline.
     */
    suspend fun declineEntry(entryId: String)

    /**
     * Resets an entry's approval status back to "pending".
     *
     * Useful for undoing previous decisions (approve/decline).
     *
     * @param entryId The ID of the entry to reset.
     */
    suspend fun undoEntryStatus(entryId: String)

    /**
     * Deletes a timesheet entry.
     *
     * @param entryId The ID of the entry to delete.
     */
    suspend fun deleteEntry(entryId: String)

    /**
     * Assigns (creates) a new shift/timesheet entry.
     *
     * The implementation may generate an ID if one is not provided.
     *
     * @param newEntry The entry to add.
     */
    suspend fun assignShift(newEntry: TimesheetEntry)
}