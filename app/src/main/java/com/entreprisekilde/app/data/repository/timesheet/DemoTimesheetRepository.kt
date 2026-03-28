package com.entreprisekilde.app.data.repository.timesheet

import com.entreprisekilde.app.data.DemoSeedData
import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import java.util.UUID

class DemoTimesheetRepository : TimesheetRepository {

    private val timesheetEntries = DemoSeedData.createTimesheetEntries().toMutableList()

    override suspend fun getEntries(): List<TimesheetEntry> {
        return timesheetEntries.toList()
    }

    override suspend fun getEmployees(): List<String> {
        return timesheetEntries
            .map { it.employeeName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
        return timesheetEntries.filter { it.employeeName == employeeName }
    }

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

    override suspend fun declineEntry(entryId: String) {
        val index = timesheetEntries.indexOfFirst { it.id == entryId }
        if (index != -1) {
            timesheetEntries[index] = timesheetEntries[index].copy(
                approvalStatus = ShiftApprovalStatus.DECLINED
            )
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        timesheetEntries.removeAll { it.id == entryId }
    }

    override suspend fun assignShift(newEntry: TimesheetEntry) {
        val entryToInsert = if (newEntry.id.isBlank()) {
            newEntry.copy(id = UUID.randomUUID().toString())
        } else {
            newEntry
        }

        timesheetEntries.add(0, entryToInsert)
    }
}