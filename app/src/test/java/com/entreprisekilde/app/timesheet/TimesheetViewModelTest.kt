package com.entreprisekilde.app.viewmodel

import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import com.entreprisekilde.app.data.repository.timesheet.TimesheetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimesheetViewModelTest {

    // Test dispatcher lets us control coroutine execution manually in each test.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Replace Main dispatcher so ViewModel coroutines run on the test dispatcher.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Restore the original Main dispatcher after each test.
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsEmployees_andEntriesForDefaultSelectedEmployee() = runTest {
        // Arrange
        val fakeEmployees = listOf("Rasmus Jensen", "Ahmad Ali")

        val fakeEntries = listOf(
            TimesheetEntry(
                id = "e1",
                date = "02/04/2026",
                fromTime = "08:00",
                toTime = "16:00",
                employeeName = "Rasmus Jensen",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.PENDING
            ),
            TimesheetEntry(
                id = "e2",
                date = "03/04/2026",
                fromTime = "09:00",
                toTime = "17:00",
                employeeName = "Rasmus Jensen",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.APPROVED
            )
        )

        val fakeRepository = object : TimesheetRepository {
            override suspend fun getEntries(): List<TimesheetEntry> = fakeEntries

            override suspend fun getEmployees(): List<String> = fakeEmployees

            override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
                return fakeEntries.filter { it.employeeName == employeeName }
            }

            override suspend fun approveEntry(entryId: String) {}
            override suspend fun declineEntry(entryId: String) {}
            override suspend fun undoEntryStatus(entryId: String) {}
            override suspend fun deleteEntry(entryId: String) {}
            override suspend fun assignShift(newEntry: TimesheetEntry) {}
        }

        // Act
        val viewModel = TimesheetViewModel(fakeRepository)
        advanceUntilIdle()

        // Assert
        // The ViewModel should load employees on init,
        // select the default employee, and fetch that employee's entries.
        assertEquals(2, viewModel.employees.size)
        assertEquals("Rasmus Jensen", viewModel.selectedEmployee.value)
        assertEquals(2, viewModel.entriesForSelectedEmployee.size)
        assertEquals("e1", viewModel.entriesForSelectedEmployee[0].id)
        assertEquals("e2", viewModel.entriesForSelectedEmployee[1].id)
    }

    @Test
    fun selectEmployee_updatesSelectedEmployee_andLoadsCorrectEntries() = runTest {
        // Arrange
        val fakeEmployees = listOf("Rasmus Jensen", "Ahmad Ali")

        val allEntries = listOf(
            TimesheetEntry(
                id = "e1",
                date = "02/04/2026",
                fromTime = "08:00",
                toTime = "16:00",
                employeeName = "Rasmus Jensen",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.PENDING
            ),
            TimesheetEntry(
                id = "e2",
                date = "03/04/2026",
                fromTime = "09:00",
                toTime = "17:00",
                employeeName = "Ahmad Ali",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.APPROVED
            )
        )

        val fakeRepository = object : TimesheetRepository {
            override suspend fun getEntries(): List<TimesheetEntry> = allEntries

            override suspend fun getEmployees(): List<String> = fakeEmployees

            override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
                return allEntries.filter { it.employeeName == employeeName }
            }

            override suspend fun approveEntry(entryId: String) {}
            override suspend fun declineEntry(entryId: String) {}
            override suspend fun undoEntryStatus(entryId: String) {}
            override suspend fun deleteEntry(entryId: String) {}
            override suspend fun assignShift(newEntry: TimesheetEntry) {}
        }

        val viewModel = TimesheetViewModel(fakeRepository)
        advanceUntilIdle()

        // Act
        viewModel.selectEmployee("Ahmad Ali")
        advanceUntilIdle()

        // Assert
        // After changing the selected employee,
        // only that employee's entries should be loaded into the UI state.
        assertEquals("Ahmad Ali", viewModel.selectedEmployee.value)
        assertEquals(1, viewModel.entriesForSelectedEmployee.size)
        assertEquals("e2", viewModel.entriesForSelectedEmployee[0].id)
    }

    @Test
    fun approveEntry_updatesEntryStatusToApproved() = runTest {
        // Arrange
        val fakeEmployees = listOf("Rasmus Jensen")

        val fakeEntries = mutableListOf(
            TimesheetEntry(
                id = "e1",
                date = "02/04/2026",
                fromTime = "08:00",
                toTime = "16:00",
                employeeName = "Rasmus Jensen",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.PENDING
            )
        )

        val fakeRepository = object : TimesheetRepository {
            override suspend fun getEntries(): List<TimesheetEntry> = fakeEntries.toList()

            override suspend fun getEmployees(): List<String> = fakeEmployees

            override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
                return fakeEntries.filter { it.employeeName == employeeName }
            }

            override suspend fun approveEntry(entryId: String) {
                val index = fakeEntries.indexOfFirst { it.id == entryId }
                if (index != -1) {
                    fakeEntries[index] = fakeEntries[index].copy(
                        approvalStatus = ShiftApprovalStatus.APPROVED
                    )
                }
            }

            override suspend fun declineEntry(entryId: String) {}
            override suspend fun undoEntryStatus(entryId: String) {}
            override suspend fun deleteEntry(entryId: String) {}
            override suspend fun assignShift(newEntry: TimesheetEntry) {}
        }

        val viewModel = TimesheetViewModel(fakeRepository)
        advanceUntilIdle()

        // Act
        viewModel.approveEntry("e1")
        advanceUntilIdle()

        // Assert
        // Approving an entry should refresh local state
        // so the changed approval status is visible immediately.
        assertEquals(1, viewModel.entriesForSelectedEmployee.size)
        assertEquals(
            ShiftApprovalStatus.APPROVED,
            viewModel.entriesForSelectedEmployee[0].approvalStatus
        )
    }

    @Test
    fun deleteEntry_removesEntry_andRefreshesEmployees() = runTest {
        // Arrange
        val fakeEmployees = mutableListOf("Rasmus Jensen")

        val fakeEntries = mutableListOf(
            TimesheetEntry(
                id = "e1",
                date = "02/04/2026",
                fromTime = "08:00",
                toTime = "16:00",
                employeeName = "Rasmus Jensen",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.PENDING
            )
        )

        val fakeRepository = object : TimesheetRepository {
            override suspend fun getEntries(): List<TimesheetEntry> = fakeEntries.toList()

            override suspend fun getEmployees(): List<String> = fakeEmployees.toList()

            override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
                return fakeEntries.filter { it.employeeName == employeeName }
            }

            override suspend fun approveEntry(entryId: String) {}
            override suspend fun declineEntry(entryId: String) {}
            override suspend fun undoEntryStatus(entryId: String) {}

            override suspend fun deleteEntry(entryId: String) {
                fakeEntries.removeAll { it.id == entryId }

                // Rebuild the employee list based on remaining entries.
                // This mimics how the real repository might behave after deletion.
                val remainingEmployees = fakeEntries.map { it.employeeName }.distinct()
                fakeEmployees.clear()
                fakeEmployees.addAll(remainingEmployees)
            }

            override suspend fun assignShift(newEntry: TimesheetEntry) {}
        }

        val viewModel = TimesheetViewModel(fakeRepository)
        advanceUntilIdle()

        // Sanity check
        assertEquals(1, viewModel.entriesForSelectedEmployee.size)
        assertEquals(1, viewModel.employees.size)

        // Act
        viewModel.deleteEntry("e1")
        advanceUntilIdle()

        // Assert
        // Deleting the only entry should also remove the only employee from the refreshed list.
        assertEquals(0, viewModel.entriesForSelectedEmployee.size)
        assertEquals(0, viewModel.employees.size)
    }

    @Test
    fun assignShift_addsEntry_andRefreshesEntriesAndEmployees() = runTest {
        // Arrange
        val fakeEmployees = mutableListOf("Rasmus Jensen")

        val fakeEntries = mutableListOf(
            TimesheetEntry(
                id = "e1",
                date = "02/04/2026",
                fromTime = "08:00",
                toTime = "16:00",
                employeeName = "Rasmus Jensen",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.PENDING
            )
        )

        val newEntry = TimesheetEntry(
            id = "e2",
            date = "03/04/2026",
            fromTime = "09:00",
            toTime = "17:00",
            employeeName = "Rasmus Jensen",
            submittedHours = 8,
            assignedHours = 8,
            approvalStatus = ShiftApprovalStatus.PENDING
        )

        val fakeRepository = object : TimesheetRepository {
            override suspend fun getEntries(): List<TimesheetEntry> = fakeEntries.toList()

            override suspend fun getEmployees(): List<String> = fakeEmployees.toList()

            override suspend fun getEntriesForEmployee(employeeName: String): List<TimesheetEntry> {
                return fakeEntries.filter { it.employeeName == employeeName }
            }

            override suspend fun approveEntry(entryId: String) {}
            override suspend fun declineEntry(entryId: String) {}
            override suspend fun undoEntryStatus(entryId: String) {}
            override suspend fun deleteEntry(entryId: String) {}

            override suspend fun assignShift(newEntry: TimesheetEntry) {
                fakeEntries.add(newEntry)

                // Refresh the employee source as well,
                // even though in this case the employee already exists.
                val updatedEmployees = fakeEntries.map { it.employeeName }.distinct()
                fakeEmployees.clear()
                fakeEmployees.addAll(updatedEmployees)
            }
        }

        val viewModel = TimesheetViewModel(fakeRepository)
        advanceUntilIdle()

        // Sanity check
        assertEquals(1, viewModel.entriesForSelectedEmployee.size)
        assertEquals(1, viewModel.employees.size)

        // Act
        viewModel.assignShift(newEntry)
        advanceUntilIdle()

        // Assert
        // The newly assigned shift should appear in the selected employee's entries,
        // and the employee list should still be valid after refresh.
        assertEquals(2, viewModel.entriesForSelectedEmployee.size)
        assertEquals("e2", viewModel.entriesForSelectedEmployee[1].id)
        assertEquals(1, viewModel.employees.size)
        assertEquals("Rasmus Jensen", viewModel.employees[0])
    }
}