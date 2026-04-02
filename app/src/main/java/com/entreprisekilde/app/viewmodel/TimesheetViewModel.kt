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

    val selectedEmployee = mutableStateOf("Rasmus Jensen")
    val employees = mutableStateListOf<String>()
    val entriesForSelectedEmployee = mutableStateListOf<TimesheetEntry>()

    init {
        loadEmployees()
        loadEntriesForSelectedEmployee()
    }

    fun selectEmployee(employeeName: String) {
        selectedEmployee.value = employeeName
        loadEntriesForSelectedEmployee()
    }

    fun approveEntry(entryId: String) {
        viewModelScope.launch {
            repository.approveEntry(entryId)
            refreshEntriesForSelectedEmployee()
        }
    }

    fun declineEntry(entryId: String) {
        viewModelScope.launch {
            repository.declineEntry(entryId)
            refreshEntriesForSelectedEmployee()
        }
    }

    fun undoEntryStatus(entryId: String) {
        viewModelScope.launch {
            repository.undoEntryStatus(entryId)
            refreshEntriesForSelectedEmployee()
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
            refreshEntriesForSelectedEmployee()
            refreshEmployees()
        }
    }

    fun assignShift(newEntry: TimesheetEntry) {
        viewModelScope.launch {
            repository.assignShift(newEntry)
            refreshEntriesForSelectedEmployee()
            refreshEmployees()
        }
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            employees.clear()
            employees.addAll(repository.getEmployees())
        }
    }

    private fun loadEntriesForSelectedEmployee() {
        viewModelScope.launch {
            entriesForSelectedEmployee.clear()
            entriesForSelectedEmployee.addAll(
                repository.getEntriesForEmployee(selectedEmployee.value)
            )
        }
    }

    private suspend fun refreshEmployees() {
        employees.clear()
        employees.addAll(repository.getEmployees())
    }

    private suspend fun refreshEntriesForSelectedEmployee() {
        entriesForSelectedEmployee.clear()
        entriesForSelectedEmployee.addAll(
            repository.getEntriesForEmployee(selectedEmployee.value)
        )
    }
}