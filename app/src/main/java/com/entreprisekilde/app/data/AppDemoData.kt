package com.entreprisekilde.app.data

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DemoSeedData {

    fun createTasks(): List<TaskData> {
        return listOf(
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
    }

    fun createMessageThreads(): List<MessageThread> {
        return listOf(
            MessageThread(
                id = 1,
                recipientId = "boss",
                recipientName = "Boss",
                lastMessage = "Can you work 2 hours extra today?",
                unreadCount = 2
            ),
            MessageThread(
                id = 2,
                recipientId = "support_team",
                recipientName = "Support Team",
                lastMessage = "Try to call John before arriving.",
                unreadCount = 1
            ),
            MessageThread(
                id = 3,
                recipientId = "shift_planner",
                recipientName = "Shift Planner",
                lastMessage = "There is 1 extra shift available tomorrow.",
                unreadCount = 0
            ),
            MessageThread(
                id = 4,
                recipientId = "john_miller",
                recipientName = "John Miller",
                lastMessage = "I finished the task at the customer site.",
                unreadCount = 0
            )
        )
    }

    fun createChatMessages(): Map<Int, List<ChatMessage>> {
        return mapOf(
            1 to listOf(
                ChatMessage(
                    id = 1,
                    threadId = 1,
                    senderId = "boss",
                    text = "Can you work 2 hours extra today?",
                    time = "09:10"
                ),
                ChatMessage(
                    id = 2,
                    threadId = 1,
                    senderId = "me",
                    text = "Yes, that is fine for me.",
                    time = "09:12"
                ),
                ChatMessage(
                    id = 3,
                    threadId = 1,
                    senderId = "boss",
                    text = "Perfect, thank you.",
                    time = "09:13"
                )
            ),
            2 to listOf(
                ChatMessage(
                    id = 4,
                    threadId = 2,
                    senderId = "support_team",
                    text = "Try to call John before arriving.",
                    time = "08:45"
                ),
                ChatMessage(
                    id = 5,
                    threadId = 2,
                    senderId = "me",
                    text = "Okay, I will do that.",
                    time = "08:47"
                )
            ),
            3 to listOf(
                ChatMessage(
                    id = 6,
                    threadId = 3,
                    senderId = "shift_planner",
                    text = "There is 1 extra shift available tomorrow.",
                    time = "07:30"
                )
            ),
            4 to listOf(
                ChatMessage(
                    id = 7,
                    threadId = 4,
                    senderId = "john_miller",
                    text = "I finished the task at the customer site.",
                    time = "15:05"
                ),
                ChatMessage(
                    id = 8,
                    threadId = 4,
                    senderId = "me",
                    text = "Great work, thank you.",
                    time = "15:08"
                )
            )
        )
    }
    fun createTimesheetEntries(): List<TimesheetEntry> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()

        return listOf(
            TimesheetEntry(
                id = "timesheet_1",
                date = today.format(formatter),
                fromTime = "09:30 AM",
                toTime = "06:30 PM",
                employeeName = "Rasmus Jensen",
                submittedHours = 9,
                assignedHours = 9,
                approvalStatus = ShiftApprovalStatus.PENDING
            ),
            TimesheetEntry(
                id = "timesheet_2",
                date = today.minusDays(1).format(formatter),
                fromTime = "09:30 AM",
                toTime = "06:30 PM",
                employeeName = "Rasmus Jensen",
                submittedHours = 9,
                assignedHours = 9,
                approvalStatus = ShiftApprovalStatus.APPROVED
            ),
            TimesheetEntry(
                id = "timesheet_3",
                date = today.minusDays(2).format(formatter),
                fromTime = "09:30 AM",
                toTime = "06:30 PM",
                employeeName = "Rasmus Jensen",
                submittedHours = 8,
                assignedHours = 9,
                approvalStatus = ShiftApprovalStatus.PENDING
            ),
            TimesheetEntry(
                id = "timesheet_4",
                date = today.plusDays(1).format(formatter),
                fromTime = "09:30 AM",
                toTime = "06:30 PM",
                employeeName = "Rasmus Jensen",
                submittedHours = 0,
                assignedHours = 9,
                approvalStatus = ShiftApprovalStatus.PENDING
            ),
            TimesheetEntry(
                id = "timesheet_5",
                date = today.minusDays(1).format(formatter),
                fromTime = "08:00 AM",
                toTime = "04:00 PM",
                employeeName = "John Miller",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.APPROVED
            ),
            TimesheetEntry(
                id = "timesheet_6",
                date = today.plusDays(2).format(formatter),
                fromTime = "10:00 AM",
                toTime = "06:00 PM",
                employeeName = "John Miller",
                submittedHours = 0,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.PENDING
            ),
            TimesheetEntry(
                id = "timesheet_7",
                date = today.format(formatter),
                fromTime = "09:00 AM",
                toTime = "05:30 PM",
                employeeName = "Peter Hansen",
                submittedHours = 8,
                assignedHours = 8,
                approvalStatus = ShiftApprovalStatus.PENDING
            )
        )
    }
}