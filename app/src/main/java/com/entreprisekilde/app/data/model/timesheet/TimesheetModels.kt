package com.entreprisekilde.app.data.model.timesheet

import java.util.UUID

/**
 * Represents the approval state of a timesheet entry.
 *
 * This is used to track whether a submitted shift:
 * - is still waiting for review
 * - has been approved
 * - has been declined
 */
enum class ShiftApprovalStatus {

    // Entry has been submitted but not yet reviewed
    PENDING,

    // Entry has been approved by admin
    APPROVED,

    // Entry has been rejected by admin
    DECLINED
}

/**
 * Represents a single timesheet entry (shift).
 *
 * This model is used for:
 * - employees submitting worked hours
 * - admins reviewing and approving shifts
 * - displaying timesheet data in the UI
 */
data class TimesheetEntry(

    // Unique ID for the timesheet entry
    val id: String = UUID.randomUUID().toString(),

    // Date of the shift (stored as String for simplicity)
    val date: String,

    // Start time of the shift
    val fromTime: String,

    // End time of the shift
    val toTime: String,

    // Name of the employee (used for display in UI)
    val employeeName: String,

    /**
     * Number of hours submitted by the employee.
     *
     * This represents what the employee claims they worked.
     */
    val submittedHours: Int,

    /**
     * Number of hours assigned/approved by admin.
     *
     * This may differ from submittedHours if adjustments are made.
     */
    val assignedHours: Int,

    // Current approval status of this entry
    val approvalStatus: ShiftApprovalStatus
)