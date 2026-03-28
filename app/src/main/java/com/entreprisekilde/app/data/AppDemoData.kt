package com.entreprisekilde.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.ui.admin.messages.ChatMessage
import com.entreprisekilde.app.ui.admin.messages.MessageThread
import com.entreprisekilde.app.ui.admin.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.ui.admin.timesheet.TimesheetEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AppDemoData {

    fun createTasks() = mutableStateListOf(
        TaskData(
            id = "task_1",
            customer = "Painting the Wall",
            phoneNumber = "12345678",
            address = "Roskilde",
            date = "08/03/2026",
            assignTo = "John Miller",
            taskDetails = "Paint wall",
            status = TaskStatus.PENDING
        ),
        TaskData(
            id = "task_2",
            customer = "Installation",
            phoneNumber = "87654321",
            address = "Copenhagen",
            date = "26/03/2026",
            assignTo = "Peter Hansen",
            taskDetails = "Install equipment",
            status = TaskStatus.IN_PROGRESS
        ),
        TaskData(
            id = "task_3",
            customer = "Bathroom Renovation",
            phoneNumber = "11112222",
            address = "Lyngby",
            date = "14/02/2026",
            assignTo = "John Miller",
            taskDetails = "Renovate bathroom",
            status = TaskStatus.COMPLETED
        ),
        TaskData(
            id = "task_4",
            customer = "Fix Sink Leak",
            phoneNumber = "11223344",
            address = "Office",
            date = "14/03/2026",
            assignTo = "John Miller",
            taskDetails = "Fix kitchen sink leak",
            status = TaskStatus.IN_PROGRESS
        ),
        TaskData(
            id = "task_5",
            customer = "Fix Sink Leak",
            phoneNumber = "55667788",
            address = "Office",
            date = "14/03/2026",
            assignTo = "Peter Hansen",
            taskDetails = "Replace damaged pipe",
            status = TaskStatus.IN_PROGRESS
        ),
        TaskData(
            id = "task_6",
            customer = "Fix Sink Leak",
            phoneNumber = "99887766",
            address = "Office",
            date = "14/03/2026",
            assignTo = "John Miller",
            taskDetails = "Final inspection",
            status = TaskStatus.COMPLETED
        )
    )

    fun createMessageThreads() = mutableStateListOf(
        MessageThread(1, "Boss", "Can you work 2 hours extra today?", 2),
        MessageThread(2, "Support Team", "Try to call John before arriving.", 1),
        MessageThread(3, "Shift Planner", "There is 1 extra shift available tomorrow.", 0),
        MessageThread(4, "John Miller", "I finished the task at the customer site.", 0)
    )

    fun createChatMessages() = mutableStateMapOf(
        1 to mutableStateListOf(
            ChatMessage(1, 1, "Can you work 2 hours extra today?", false, "09:10"),
            ChatMessage(2, 1, "Yes, that is fine for me.", true, "09:12"),
            ChatMessage(3, 1, "Perfect, thank you.", false, "09:13")
        ),
        2 to mutableStateListOf(
            ChatMessage(4, 2, "Try to call John before arriving.", false, "08:45"),
            ChatMessage(5, 2, "Okay, I will do that.", true, "08:47")
        ),
        3 to mutableStateListOf(
            ChatMessage(6, 3, "There is 1 extra shift available tomorrow.", false, "07:30")
        ),
        4 to mutableStateListOf(
            ChatMessage(7, 4, "I finished the task at the customer site.", false, "15:05"),
            ChatMessage(8, 4, "Great work, thank you.", true, "15:08")
        )
    )

    fun createTimesheetEntries() = run {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()

        mutableStateListOf(
            TimesheetEntry(today.format(formatter), "09:30 AM", "06:30 PM", "Rasmus Jensen", 9, 9, ShiftApprovalStatus.Pending),
            TimesheetEntry(today.minusDays(1).format(formatter), "09:30 AM", "06:30 PM", "Rasmus Jensen", 9, 9, ShiftApprovalStatus.Approved),
            TimesheetEntry(today.minusDays(2).format(formatter), "09:30 AM", "06:30 PM", "Rasmus Jensen", 8, 9, ShiftApprovalStatus.Pending),
            TimesheetEntry(today.plusDays(1).format(formatter), "09:30 AM", "06:30 PM", "Rasmus Jensen", 0, 9, ShiftApprovalStatus.Pending),
            TimesheetEntry(today.minusDays(1).format(formatter), "08:00 AM", "04:00 PM", "John Miller", 8, 8, ShiftApprovalStatus.Approved),
            TimesheetEntry(today.plusDays(2).format(formatter), "10:00 AM", "06:00 PM", "John Miller", 0, 8, ShiftApprovalStatus.Pending),
            TimesheetEntry(today.format(formatter), "09:00 AM", "05:30 PM", "Peter Hansen", 8, 8, ShiftApprovalStatus.Pending)
        )
    }
}