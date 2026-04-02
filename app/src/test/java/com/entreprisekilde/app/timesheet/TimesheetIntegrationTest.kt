package com.entreprisekilde.app.timesheet

import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.repository.timesheet.DemoTimesheetRepository
import com.entreprisekilde.app.viewmodel.TimesheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimesheetIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DemoTimesheetRepository
    private lateinit var viewModel: TimesheetViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = DemoTimesheetRepository()
        viewModel = TimesheetViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun approveEntry_shouldChangeEntryStatusToApproved() = runTest {
        // Let init finish
        advanceUntilIdle()

        // Default selected employee is "Rasmus Jensen"
        val pendingEntry = viewModel.entriesForSelectedEmployee.firstOrNull {
            it.approvalStatus == ShiftApprovalStatus.PENDING
        }

        assertNotNull(pendingEntry)

        // Act
        viewModel.approveEntry(pendingEntry!!.id)
        advanceUntilIdle()

        // Assert
        val updatedEntry = viewModel.entriesForSelectedEmployee.firstOrNull {
            it.id == pendingEntry.id
        }

        assertNotNull(updatedEntry)
        assertEquals(ShiftApprovalStatus.APPROVED, updatedEntry!!.approvalStatus)
    }
}