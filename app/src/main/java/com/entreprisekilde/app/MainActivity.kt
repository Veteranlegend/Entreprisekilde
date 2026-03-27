package com.entreprisekilde.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.entreprisekilde.app.ui.admin.dashboard.AdminDashboardScreen
import com.entreprisekilde.app.ui.admin.management.CalendarDayScreen
import com.entreprisekilde.app.ui.admin.management.CalendarScreen
import com.entreprisekilde.app.ui.admin.management.ChatMessage
import com.entreprisekilde.app.ui.admin.management.ChatScreen
import com.entreprisekilde.app.ui.admin.management.CreateTaskScreen
import com.entreprisekilde.app.ui.admin.management.CreateUserScreen
import com.entreprisekilde.app.ui.admin.management.EmployeeScreen
import com.entreprisekilde.app.ui.admin.management.EmployeeUser
import com.entreprisekilde.app.ui.admin.management.MessageThread
import com.entreprisekilde.app.ui.admin.management.MessagesScreen
import com.entreprisekilde.app.ui.admin.management.ProfileScreen
import com.entreprisekilde.app.ui.admin.management.ShiftApprovalStatus
import com.entreprisekilde.app.ui.admin.management.TaskData
import com.entreprisekilde.app.ui.admin.management.TaskDetailsScreen
import com.entreprisekilde.app.ui.admin.management.TasksScreen
import com.entreprisekilde.app.ui.admin.management.TimesheetEmployeeListScreen
import com.entreprisekilde.app.ui.admin.management.TimesheetEntry
import com.entreprisekilde.app.ui.admin.management.TimesheetScreen
import com.entreprisekilde.app.ui.admin.management.UserDetailsScreen
import com.entreprisekilde.app.ui.auth.login.LoginScreen
import com.entreprisekilde.app.ui.notifications.NotificationRepository
import com.entreprisekilde.app.ui.notifications.NotificationScreen
import com.entreprisekilde.app.ui.theme.EntreprisekildeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EntreprisekildeTheme {

                val currentScreen = remember { mutableStateOf("login") }
                val selectedCalendarDate = remember { mutableStateOf("") }
                val selectedTaskIndex = remember { mutableStateOf(-1) }
                val taskDetailsBackTarget = remember { mutableStateOf("tasks") }
                val selectedTimesheetEmployee = remember { mutableStateOf("Rasmus Jensen") }
                val selectedUser = remember { mutableStateOf<EmployeeUser?>(null) }
                val profileImageUri = remember { mutableStateOf<String?>(null) }
                val selectedThread = remember { mutableStateOf<MessageThread?>(null) }

                val users = remember {
                    mutableStateListOf(
                        EmployeeUser(1, "Rasmus", "Jensen", "rasmus.jensen@entreprisekilde.dk", "12341234"),
                        EmployeeUser(2, "Tomas", "Larsen", "tomas.larsen@entreprisekilde.dk", "22334455"),
                        EmployeeUser(3, "Peter", "Hansen", "peter.hansen@entreprisekilde.dk", "33445566"),
                        EmployeeUser(4, "John", "Miller", "john.miller@entreprisekilde.dk", "44556677"),
                        EmployeeUser(5, "Ahmad", "El Haj", "ahmad.elhaj@entreprisekilde.dk", "55667788"),
                        EmployeeUser(6, "Lars", "Nielsen", "lars.nielsen@entreprisekilde.dk", "66778899")
                    )
                }

                val tasks = remember {
                    mutableStateListOf(
                        TaskData("Painting the Wall", "12345678", "Roskilde", "08/03/2026", "John", "Paint wall", "Pending"),
                        TaskData("Installation", "87654321", "Copenhagen", "26/03/2026", "Peter", "Install equipment", "In-progress"),
                        TaskData("Bathroom Renovation", "11112222", "Lyngby", "14/02/2026", "John", "Renovate bathroom", "Complete"),
                        TaskData("Fix Sink Leak", "11223344", "Office", "14/03/2026", "John", "Fix kitchen sink leak", "In-progress"),
                        TaskData("Fix Sink Leak", "55667788", "Office", "14/03/2026", "Peter", "Replace damaged pipe", "In-progress"),
                        TaskData("Fix Sink Leak", "99887766", "Office", "14/03/2026", "John", "Final inspection", "Complete")
                    )
                }

                val messageThreads = remember {
                    mutableStateListOf(
                        MessageThread(1, "Boss", "Can you work 2 hours extra today?", 2),
                        MessageThread(2, "Support Team", "Try to call John before arriving.", 1),
                        MessageThread(3, "Shift Planner", "There is 1 extra shift available tomorrow.", 0),
                        MessageThread(4, "John Miller", "I finished the task at the customer site.", 0)
                    )
                }

                val chatMessages = remember {
                    mutableStateMapOf(
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
                }

                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val today = LocalDate.now()

                val timesheetEntries = remember {
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

                val notifications = NotificationRepository.notifications
                val unreadNotificationCount = NotificationRepository.unreadCount()

                when (currentScreen.value) {

                    "login" -> LoginScreen(
                        onLoginClick = {
                            currentScreen.value = "dashboard"
                        }
                    )

                    "dashboard" -> AdminDashboardScreen(
                        unreadNotificationCount = unreadNotificationCount,
                        onAllTasksClick = { currentScreen.value = "tasks" },
                        onCreateTaskClick = { currentScreen.value = "createTask" },
                        onCalendarClick = { currentScreen.value = "calendar" },
                        onMessagesClick = { currentScreen.value = "messages" },
                        onTimesheetClick = { currentScreen.value = "timesheetEmployees" },
                        onUsersClick = { currentScreen.value = "employees" },
                        onNotificationsClick = { currentScreen.value = "notifications" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "messages" -> MessagesScreen(
                        threads = messageThreads,
                        unreadNotificationCount = unreadNotificationCount,
                        onThreadClick = { thread ->
                            selectedThread.value = thread

                            val index = messageThreads.indexOfFirst { it.id == thread.id }
                            if (index != -1) {
                                messageThreads[index] = messageThreads[index].copy(unreadCount = 0)
                            }

                            currentScreen.value = "chat"
                        },
                        onDeleteThread = { thread ->
                            messageThreads.removeAll { it.id == thread.id }
                            chatMessages.remove(thread.id)

                            if (selectedThread.value?.id == thread.id) {
                                selectedThread.value = null
                            }
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onNotificationsClick = { currentScreen.value = "notifications" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "chat" -> {
                        val thread = selectedThread.value
                        if (thread != null) {
                            val threadMessages = chatMessages.getOrPut(thread.id) {
                                mutableStateListOf()
                            }

                            ChatScreen(
                                thread = thread,
                                messages = threadMessages,
                                onBack = { currentScreen.value = "messages" },
                                onSendMessage = { sentMessage ->
                                    val threadIndex = messageThreads.indexOfFirst { it.id == thread.id }
                                    if (threadIndex != -1) {
                                        val oldThread = messageThreads[threadIndex]
                                        messageThreads[threadIndex] = oldThread.copy(
                                            lastMessage = sentMessage
                                        )
                                    }

                                    NotificationRepository.addMessageNotification(thread.name)
                                }
                            )
                        }
                    }

                    "notifications" -> NotificationScreen(
                        notifications = notifications,
                        unreadCount = unreadNotificationCount,
                        onBack = { currentScreen.value = "dashboard" },
                        onMarkAllAsRead = {
                            NotificationRepository.markAllAsRead()
                        },
                        onNotificationClick = { notification ->
                            NotificationRepository.markAsRead(notification.id)
                        },
                        onDeleteNotification = { notification ->
                            NotificationRepository.deleteNotification(notification.id)
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onMessagesClick = { currentScreen.value = "messages" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "profile" -> ProfileScreen(
                        email = "tomas.larsen@entreprisekilde.dk",
                        firstName = "Tomas",
                        lastName = "Larsen",
                        phoneNumber = "1234123456",
                        profileImageUri = profileImageUri.value,
                        onProfileImageChange = { newUri ->
                            profileImageUri.value = newUri
                        },
                        onLogoutClick = {
                            currentScreen.value = "login"
                        },
                        onHomeClick = {
                            currentScreen.value = "dashboard"
                        },
                        onMessagesClick = {
                            currentScreen.value = "messages"
                        },
                        onNotificationsClick = {
                            currentScreen.value = "notifications"
                        },
                        onProfileClick = {
                            currentScreen.value = "profile"
                        }
                    )

                    "tasks" -> TasksScreen(
                        tasks = tasks,
                        onBack = { currentScreen.value = "dashboard" },
                        onCreateTaskClick = { currentScreen.value = "createTask" },
                        onDeleteTask = { index ->
                            if (index in tasks.indices) tasks.removeAt(index)
                        },
                        onTaskClick = { clickedTask ->
                            selectedTaskIndex.value = tasks.indexOf(clickedTask)
                            if (selectedTaskIndex.value != -1) {
                                taskDetailsBackTarget.value = "tasks"
                                currentScreen.value = "taskDetails"
                            }
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "createTask" -> CreateTaskScreen(
                        unreadNotificationCount = unreadNotificationCount,
                        onBack = { currentScreen.value = "dashboard" },
                        onCreateTask = { newTask ->
                            tasks.add(0, newTask)
                            NotificationRepository.addTaskAssignedNotification(
                                taskName = newTask.customer,
                                assignedTo = newTask.assignTo
                            )
                            currentScreen.value = "tasks"
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onMessagesClick = { currentScreen.value = "messages" },
                        onNotificationsClick = { currentScreen.value = "notifications" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "employees" -> EmployeeScreen(
                        users = users,
                        onBack = { currentScreen.value = "dashboard" },
                        onCreateUserClick = { currentScreen.value = "createUser" },
                        onUserClick = { user ->
                            selectedUser.value = user
                            currentScreen.value = "userDetails"
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "createUser" -> CreateUserScreen(
                        onBack = { currentScreen.value = "employees" },
                        onAddUserClick = { currentScreen.value = "employees" },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "userDetails" -> {
                        val currentUser = selectedUser.value
                        if (currentUser != null) {
                            UserDetailsScreen(
                                user = currentUser,
                                onBack = { currentScreen.value = "employees" },
                                onSaveUser = { updatedUser ->
                                    val index = users.indexOfFirst { it.id == updatedUser.id }
                                    if (index != -1) {
                                        users[index] = updatedUser
                                        selectedUser.value = updatedUser
                                    }
                                },
                                onHomeClick = { currentScreen.value = "dashboard" },
                                onProfileClick = { currentScreen.value = "profile" }
                            )
                        }
                    }

                    "calendar" -> CalendarScreen(
                        tasks = tasks,
                        unreadNotificationCount = unreadNotificationCount,
                        onBack = { currentScreen.value = "dashboard" },
                        onDayClick = { date: String ->
                            selectedCalendarDate.value = date
                            currentScreen.value = "calendarDay"
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onMessagesClick = { currentScreen.value = "messages" },
                        onNotificationsClick = { currentScreen.value = "notifications" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "calendarDay" -> CalendarDayScreen(
                        selectedDate = selectedCalendarDate.value,
                        tasksForDay = tasks.filter { it.date == selectedCalendarDate.value },
                        onBack = { currentScreen.value = "calendar" },
                        onTaskClick = { clickedTask ->
                            selectedTaskIndex.value = tasks.indexOf(clickedTask)
                            if (selectedTaskIndex.value != -1) {
                                taskDetailsBackTarget.value = "calendarDay"
                                currentScreen.value = "taskDetails"
                            }
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onMessagesClick = { currentScreen.value = "messages" },
                        onNotificationsClick = { currentScreen.value = "notifications" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "taskDetails" -> {
                        val taskIndex = selectedTaskIndex.value
                        if (taskIndex in tasks.indices) {
                            TaskDetailsScreen(
                                task = tasks[taskIndex],
                                onBack = { currentScreen.value = taskDetailsBackTarget.value },
                                onSaveEdit = { updatedTask ->
                                    if (taskIndex in tasks.indices) {
                                        tasks[taskIndex] = updatedTask
                                    }
                                },
                                unreadNotificationCount = unreadNotificationCount,
                                onHomeClick = { currentScreen.value = "dashboard" },
                                onMessagesClick = { currentScreen.value = "messages" },
                                onNotificationsClick = { currentScreen.value = "notifications" },
                                onProfileClick = { currentScreen.value = "profile" }
                            )
                        }
                    }

                    "timesheetEmployees" -> TimesheetEmployeeListScreen(
                        employees = timesheetEntries.map { it.employeeName }.distinct(),
                        unreadNotificationCount = unreadNotificationCount,
                        onBack = { currentScreen.value = "dashboard" },
                        onEmployeeClick = { employee ->
                            selectedTimesheetEmployee.value = employee
                            currentScreen.value = "timesheet"
                        },
                        onHomeClick = { currentScreen.value = "dashboard" },
                        onMessagesClick = { currentScreen.value = "messages" },
                        onNotificationsClick = { currentScreen.value = "notifications" },
                        onProfileClick = { currentScreen.value = "profile" }
                    )

                    "timesheet" -> {
                        val employeeEntries = timesheetEntries.filter {
                            it.employeeName == selectedTimesheetEmployee.value
                        }

                        TimesheetScreen(
                            employeeName = selectedTimesheetEmployee.value,
                            timesheets = employeeEntries,
                            unreadNotificationCount = unreadNotificationCount,
                            onBack = { currentScreen.value = "timesheetEmployees" },
                            onApproveEntry = { localIndex ->
                                val realIndexes = timesheetEntries.mapIndexedNotNull { index, entry ->
                                    if (entry.employeeName == selectedTimesheetEmployee.value) index else null
                                }
                                if (localIndex in realIndexes.indices) {
                                    val realIndex = realIndexes[localIndex]
                                    timesheetEntries[realIndex] =
                                        timesheetEntries[realIndex].copy(
                                            approvalStatus = ShiftApprovalStatus.Approved
                                        )
                                }
                            },
                            onDeclineEntry = { localIndex ->
                                val realIndexes = timesheetEntries.mapIndexedNotNull { index, entry ->
                                    if (entry.employeeName == selectedTimesheetEmployee.value) index else null
                                }
                                if (localIndex in realIndexes.indices) {
                                    val realIndex = realIndexes[localIndex]
                                    timesheetEntries[realIndex] =
                                        timesheetEntries[realIndex].copy(
                                            approvalStatus = ShiftApprovalStatus.Declined
                                        )
                                }
                            },
                            onDeleteEntry = { localIndex ->
                                val realIndexes = timesheetEntries.mapIndexedNotNull { index, entry ->
                                    if (entry.employeeName == selectedTimesheetEmployee.value) index else null
                                }
                                if (localIndex in realIndexes.indices) {
                                    val realIndex = realIndexes[localIndex]
                                    timesheetEntries.removeAt(realIndex)
                                }
                            },
                            onAssignShift = { newEntry ->
                                timesheetEntries.add(0, newEntry)
                            },
                            onHomeClick = { currentScreen.value = "dashboard" },
                            onMessagesClick = { currentScreen.value = "messages" },
                            onNotificationsClick = { currentScreen.value = "notifications" },
                            onProfileClick = { currentScreen.value = "profile" }
                        )
                    }
                }
            }
        }
    }
}