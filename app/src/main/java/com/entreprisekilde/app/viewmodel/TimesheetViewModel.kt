package com.entreprisekilde.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import com.entreprisekilde.app.data.repository.timesheet.TimesheetRepository
import kotlinx.coroutines.launch

class TimesheetViewModel(
    private val repository: TimesheetRepository
) : ViewModel() {

    // Holds the currently selected employee in the timesheet flow.
    // The UI reads this to know whose entries should be shown.
    val selectedEmployee = mutableStateOf("Rasmus Jensen")

    // List of employee names that have timesheet-related data.
    // Using a state list means Compose will recompose automatically when this changes.
    val employees = mutableStateListOf<String>()

    // The visible timesheet entries for the currently selected employee.
    val entriesForSelectedEmployee = mutableStateListOf<TimesheetEntry>()

    init {
        // Load initial data as soon as the ViewModel is created.
        // We first load employee names, then load entries for the default selected employee.
        loadEmployees()
        loadEntriesForSelectedEmployee()
    }

    fun selectEmployee(employeeName: String) {
        // Update the selected employee and immediately refresh the list
        // so the screen always reflects the latest selection.
        selectedEmployee.value = employeeName
        loadEntriesForSelectedEmployee()
    }

    fun approveEntry(entryId: String) {
        viewModelScope.launch {
            // Persist the approval, then reload the current employee's entries
            // so the UI reflects the new status.
            repository.approveEntry(entryId)
            refreshEntriesForSelectedEmployee()
        }
    }

    fun declineEntry(entryId: String) {
        viewModelScope.launch {
            // Same flow as approval: update repository first, then refresh UI state.
            repository.declineEntry(entryId)
            refreshEntriesForSelectedEmployee()
        }
    }

    fun undoEntryStatus(entryId: String) {
        viewModelScope.launch {
            // Resets an entry back to its previous reviewable state (typically pending),
            // then reloads the selected employee's visible entries.
            repository.undoEntryStatus(entryId)
            refreshEntriesForSelectedEmployee()
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            // After deleting an entry, we refresh both:
            // 1. current employee entries, because one item is gone
            // 2. employee list, in case repository logic changes who should appear there
            repository.deleteEntry(entryId)
            refreshEntriesForSelectedEmployee()
            refreshEmployees()
        }
    }

    fun assignShift(newEntry: TimesheetEntry) {
        viewModelScope.launch {
            // Adding a shift can affect both the current employee's entries
            // and the overall employee list, so we refresh both sources.
            repository.assignShift(newEntry)
            refreshEntriesForSelectedEmployee()
            refreshEmployees()
        }
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            // Initial async load for employee names.
            employees.clear()
            employees.addAll(repository.getEmployees())
        }
    }

    private fun loadEntriesForSelectedEmployee() {
        viewModelScope.launch {
            // Initial async load for the currently selected employee's timesheet entries.
            entriesForSelectedEmployee.clear()
            entriesForSelectedEmployee.addAll(
                repository.getEntriesForEmployee(selectedEmployee.value)
            )
        }
    }

    private suspend fun refreshEmployees() {
        // Shared refresh helper used after operations that may affect employee availability.
        employees.clear()
        employees.addAll(repository.getEmployees())
    }

    private suspend fun refreshEntriesForSelectedEmployee() {
        // Shared refresh helper used after operations that mutate timesheet data.
        entriesForSelectedEmployee.clear()
        entriesForSelectedEmployee.addAll(
            repository.getEntriesForEmployee(selectedEmployee.value)
        )
    }
}