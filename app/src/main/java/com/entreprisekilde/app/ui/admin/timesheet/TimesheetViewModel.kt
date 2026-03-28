package com.entreprisekilde.app.ui.admin.timesheet

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class TimesheetViewModel(
    private val repository: TimesheetRepository
) : ViewModel() {

    val selectedEmployee = mutableStateOf("Rasmus Jensen")

    fun getEmployees(): List<String> {
        return repository.getEmployees()
    }

    fun selectEmployee(employeeName: String) {
        selectedEmployee.value = employeeName
    }

    fun getEntriesForSelectedEmployee(): List<TimesheetEntry> {
        return repository.getEntriesForEmployee(selectedEmployee.value)
    }

    fun approveEntry(localIndex: Int) {
        repository.approveEntry(selectedEmployee.value, localIndex)
    }

    fun declineEntry(localIndex: Int) {
        repository.declineEntry(selectedEmployee.value, localIndex)
    }

    fun deleteEntry(localIndex: Int) {
        repository.deleteEntry(selectedEmployee.value, localIndex)
    }

    fun assignShift(newEntry: TimesheetEntry) {
        repository.assignShift(newEntry)
    }
}