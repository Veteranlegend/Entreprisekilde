package com.entreprisekilde.app.data.repository.timesheet

import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry

interface TimesheetRepository {

    suspend fun getEntries(): List<TimesheetEntry>

    suspend fun getEmployees(): List<String>

    suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry>

    suspend fun approveEntry(entryId: String)

    suspend fun declineEntry(entryId: String)

    suspend fun deleteEntry(entryId: String)

    suspend fun assignShift(newEntry: TimesheetEntry)
}