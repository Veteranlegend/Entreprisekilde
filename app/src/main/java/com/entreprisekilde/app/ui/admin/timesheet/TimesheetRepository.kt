package com.entreprisekilde.app.ui.admin.timesheet

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.entreprisekilde.app.data.AppDemoData

class TimesheetRepository {

    private val timesheetEntries = AppDemoData.createTimesheetEntries()

    fun getEntries(): SnapshotStateList<TimesheetEntry> {
        return timesheetEntries
    }

    fun getEmployees(): List<String> {
        return timesheetEntries.map { it.employeeName }.distinct().sorted()
    }

    fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
        return timesheetEntries.filter { it.employeeName == employeeName }
    }

    fun approveEntry(employeeName: String, localIndex: Int) {
        val realIndexes = timesheetEntries.mapIndexedNotNull { index, entry ->
            if (entry.employeeName == employeeName) index else null
        }

        if (localIndex in realIndexes.indices) {
            val realIndex = realIndexes[localIndex]
            val oldEntry = timesheetEntries[realIndex]

            val submittedHoursToUse =
                if (oldEntry.submittedHours > 0) oldEntry.submittedHours
                else oldEntry.assignedHours

            timesheetEntries[realIndex] = oldEntry.copy(
                submittedHours = submittedHoursToUse,
                approvalStatus = ShiftApprovalStatus.Approved
            )
        }
    }

    fun declineEntry(employeeName: String, localIndex: Int) {
        val realIndexes = timesheetEntries.mapIndexedNotNull { index, entry ->
            if (entry.employeeName == employeeName) index else null
        }

        if (localIndex in realIndexes.indices) {
            val realIndex = realIndexes[localIndex]
            timesheetEntries[realIndex] = timesheetEntries[realIndex].copy(
                approvalStatus = ShiftApprovalStatus.Declined
            )
        }
    }

    fun deleteEntry(employeeName: String, localIndex: Int) {
        val realIndexes = timesheetEntries.mapIndexedNotNull { index, entry ->
            if (entry.employeeName == employeeName) index else null
        }

        if (localIndex in realIndexes.indices) {
            val realIndex = realIndexes[localIndex]
            timesheetEntries.removeAt(realIndex)
        }
    }

    fun assignShift(newEntry: TimesheetEntry) {
        timesheetEntries.add(0, newEntry)
    }
}