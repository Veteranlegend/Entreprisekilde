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

/**
 * Integration test for timesheet approval behavior.
 *
 * This test checks that approving a pending entry through the ViewModel
 * actually updates the selected employee's entry status to APPROVED.
 *
 * Since this goes through the real demo repository and the ViewModel,
 * it gives us confidence that the flow works across multiple layers,
 * not just in one isolated function.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimesheetIntegrationTest {

    /**
     * Test dispatcher used to control coroutine execution in a predictable way.
     *
     * This is especially useful because the ViewModel likely launches work
     * during initialization, and we want full control over when that work runs.
     */
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DemoTimesheetRepository
    private lateinit var viewModel: TimesheetViewModel

    @Before
    fun setup() {
        // Replace the Main dispatcher so coroutine-based ViewModel code can run in tests.
        Dispatchers.setMain(testDispatcher)

        repository = DemoTimesheetRepository()
        viewModel = TimesheetViewModel(repository)
    }

    @After
    fun tearDown() {
        // Restore the real Main dispatcher after each test to avoid leaking test setup.
        Dispatchers.resetMain()
    }

    @Test
    fun approveEntry_shouldChangeEntryStatusToApproved() = runTest {
        // Let any coroutine work started during ViewModel init complete first.
        advanceUntilIdle()

        // The test assumes the default selected employee is "Rasmus Jensen".
        // From that employee's entries, find one that is still pending approval.
        val pendingEntry = viewModel.entriesForSelectedEmployee.firstOrNull {
            it.approvalStatus == ShiftApprovalStatus.PENDING
        }

        // Sanity check: the test data should contain at least one pending entry.
        assertNotNull(pendingEntry)

        // Act
        // Approve the pending entry, then allow all queued coroutine work to finish.
        viewModel.approveEntry(pendingEntry!!.id)
        advanceUntilIdle()

        // Assert
        // Re-read the entry from the current ViewModel state after approval.
        val updatedEntry = viewModel.entriesForSelectedEmployee.firstOrNull {
            it.id == pendingEntry.id
        }

        // Make sure the entry still exists and now has the approved status.
        assertNotNull(updatedEntry)
        assertEquals(ShiftApprovalStatus.APPROVED, updatedEntry!!.approvalStatus)
    }
}