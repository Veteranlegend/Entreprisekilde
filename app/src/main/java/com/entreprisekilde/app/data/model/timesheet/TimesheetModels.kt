package com.entreprisekilde.app.data.model.timesheet
import com.entreprisekilde.app.data.repository.timesheet.TimesheetRepository
import java.util.UUID

enum class ShiftApprovalStatus {
    PENDING,
    APPROVED,
    DECLINED
}

data class TimesheetEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val fromTime: String,
    val toTime: String,
    val employeeName: String,
    val submittedHours: Int,
    val assignedHours: Int,
    val approvalStatus: ShiftApprovalStatus
)