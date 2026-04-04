package com.entreprisekilde.app.data

import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Central place for demo / seed data used across the app.
 *
 * This object helps populate the app with predictable sample content for:
 * - tasks
 * - message threads
 * - chat messages
 * - timesheet entries
 *
 * This is especially useful for:
 * - local development
 * - UI previews
 * - demo builds
 * - testing flows without needing real backend data
 */
object DemoSeedData {

    /**
     * Creates a fixed list of demo tasks.
     *
     * The data here is intentionally simple and readable so it is easy to reason
     * about while developing task screens and task-related flows.
     */
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

    /**
     * Creates demo message threads for the messaging feature.
     *
     * Each thread represents the conversation summary shown in an inbox list.
     * It includes:
     * - the recipient / other participant
     * - the last message preview
     * - unread counts
     * - participant metadata
     * - last sender information
     *
     * These IDs are matched by [createChatMessages], so thread 1 maps to the
     * messages under key 1, thread 2 maps to key 2, and so on.
     */
    fun createMessageThreads(): List<MessageThread> {
        return listOf(
            MessageThread(
                id = 1,
                recipientId = "boss",
                recipientName = "Boss",
                lastMessage = "Can you work 2 hours extra today?",
                unreadCount = 2,
                participantIds = listOf("me", "boss"),
                participantNames = mapOf(
                    "me" to "Me",
                    "boss" to "Boss"
                ),
                unreadCountByUser = mapOf(
                    "me" to 2,
                    "boss" to 0
                ),
                updatedAt = 1L,
                lastMessageSenderId = "boss"
            ),
            MessageThread(
                id = 2,
                recipientId = "support_team",
                recipientName = "Support Team",
                lastMessage = "Try to call John before arriving.",
                unreadCount = 1,
                participantIds = listOf("me", "support_team"),
                participantNames = mapOf(
                    "me" to "Me",
                    "support_team" to "Support Team"
                ),
                unreadCountByUser = mapOf(
                    "me" to 1,
                    "support_team" to 0
                ),
                updatedAt = 2L,
                lastMessageSenderId = "support_team"
            ),
            MessageThread(
                id = 3,
                recipientId = "shift_planner",
                recipientName = "Shift Planner",
                lastMessage = "There is 1 extra shift available tomorrow.",
                unreadCount = 0,
                participantIds = listOf("me", "shift_planner"),
                participantNames = mapOf(
                    "me" to "Me",
                    "shift_planner" to "Shift Planner"
                ),
                unreadCountByUser = mapOf(
                    "me" to 0,
                    "shift_planner" to 0
                ),
                updatedAt = 3L,
                lastMessageSenderId = "shift_planner"
            ),
            MessageThread(
                id = 4,
                recipientId = "john_miller",
                recipientName = "John Miller",
                lastMessage = "I finished the task at the customer site.",
                unreadCount = 0,
                participantIds = listOf("me", "john_miller"),
                participantNames = mapOf(
                    "me" to "Me",
                    "john_miller" to "John Miller"
                ),
                unreadCountByUser = mapOf(
                    "me" to 0,
                    "john_miller" to 0
                ),
                updatedAt = 4L,
                lastMessageSenderId = "john_miller"
            )
        )
    }

    /**
     * Creates demo chat messages grouped by thread ID.
     *
     * The returned map uses the thread ID as the key:
     * - key 1 -> messages for thread 1
     * - key 2 -> messages for thread 2
     * - etc.
     *
     * This makes it easy for a demo repository to fetch messages for a given
     * thread without needing a real database.
     */
    fun createChatMessages(): Map<Int, List<ChatMessage>> {
        return mapOf(
            1 to listOf(
                ChatMessage(
                    id = "1",
                    threadId = 1,
                    senderId = "boss",
                    text = "Can you work 2 hours extra today?",
                    time = "09:10",
                    createdAt = 1L,

                    // Only the sender has read this so far, which helps simulate
                    // unread behavior for the other participant.
                    readByUserIds = listOf("boss")
                ),
                ChatMessage(
                    id = "2",
                    threadId = 1,
                    senderId = "me",
                    text = "Yes, that is fine for me.",
                    time = "09:12",
                    createdAt = 2L,
                    readByUserIds = listOf("me", "boss")
                ),
                ChatMessage(
                    id = "3",
                    threadId = 1,
                    senderId = "boss",
                    text = "Perfect, thank you.",
                    time = "09:13",
                    createdAt = 3L,
                    readByUserIds = listOf("boss")
                )
            ),
            2 to listOf(
                ChatMessage(
                    id = "4",
                    threadId = 2,
                    senderId = "support_team",
                    text = "Try to call John before arriving.",
                    time = "08:45",
                    createdAt = 4L,
                    readByUserIds = listOf("support_team")
                ),
                ChatMessage(
                    id = "5",
                    threadId = 2,
                    senderId = "me",
                    text = "Okay, I will do that.",
                    time = "08:47",
                    createdAt = 5L,
                    readByUserIds = listOf("me", "support_team")
                )
            ),
            3 to listOf(
                ChatMessage(
                    id = "6",
                    threadId = 3,
                    senderId = "shift_planner",
                    text = "There is 1 extra shift available tomorrow.",
                    time = "07:30",
                    createdAt = 6L,

                    // Both users have read this one, so this thread has no unread messages.
                    readByUserIds = listOf("shift_planner", "me")
                )
            ),
            4 to listOf(
                ChatMessage(
                    id = "7",
                    threadId = 4,
                    senderId = "john_miller",
                    text = "I finished the task at the customer site.",
                    time = "15:05",
                    createdAt = 7L,
                    readByUserIds = listOf("john_miller", "me")
                ),
                ChatMessage(
                    id = "8",
                    threadId = 4,
                    senderId = "me",
                    text = "Great work, thank you.",
                    time = "15:08",
                    createdAt = 8L,
                    readByUserIds = listOf("me", "john_miller")
                )
            )
        )
    }

    /**
     * Creates demo timesheet entries.
     *
     * We generate the dates relative to "today" so the timesheet screen always
     * feels current, instead of being locked to old static dates.
     *
     * Example:
     * - today
     * - yesterday
     * - two days ago
     * - tomorrow
     * - two days from now
     *
     * This is a small detail, but it makes the demo experience feel much more real.
     */
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

                // submittedHours = 0 is a useful way to represent a future shift
                // that has been assigned but not yet worked/submitted.
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