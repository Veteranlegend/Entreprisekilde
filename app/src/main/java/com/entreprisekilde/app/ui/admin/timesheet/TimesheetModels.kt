package com.entreprisekilde.app.ui.admin.timesheet

enum class ShiftApprovalStatus {
    Pending,
    Approved,
    Declined
}

data class TimesheetEntry(
    val date: String,
    val fromTime: String,
    val toTime: String,
    val employeeName: String,
    val submittedHours: Int,
    val assignedHours: Int,
    val approvalStatus: ShiftApprovalStatus
)