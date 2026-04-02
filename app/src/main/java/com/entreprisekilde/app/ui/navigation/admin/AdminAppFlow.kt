package com.entreprisekilde.app.ui.navigation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.ui.admin.calendar.CalendarDayScreen
import com.entreprisekilde.app.ui.admin.calendar.CalendarScreen
import com.entreprisekilde.app.ui.admin.dashboard.AdminDashboardScreen
import com.entreprisekilde.app.ui.admin.messages.ChatScreen
import com.entreprisekilde.app.ui.admin.messages.MessagesScreen
import com.entreprisekilde.app.ui.admin.profile.ProfileScreen
import com.entreprisekilde.app.ui.admin.tasks.CreateTaskScreen
import com.entreprisekilde.app.ui.admin.tasks.TaskDetailsScreen
import com.entreprisekilde.app.ui.admin.tasks.TasksScreen
import com.entreprisekilde.app.ui.admin.timesheet.TimesheetEmployeeListScreen
import com.entreprisekilde.app.ui.admin.timesheet.TimesheetScreen
import com.entreprisekilde.app.ui.admin.viewmodel.CreateUserScreen
import com.entreprisekilde.app.ui.admin.viewmodel.EmployeeScreen
import com.entreprisekilde.app.ui.admin.viewmodel.UserDetailsScreen
import com.entreprisekilde.app.ui.notifications.NotificationScreen
import com.entreprisekilde.app.viewmodel.MessagesViewModel
import com.entreprisekilde.app.viewmodel.NotificationViewModel
import com.entreprisekilde.app.viewmodel.TasksViewModel
import com.entreprisekilde.app.viewmodel.TimesheetViewModel
import com.entreprisekilde.app.viewmodel.UsersViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AdminAppFlow(
    usersViewModel: UsersViewModel,
    tasksViewModel: TasksViewModel,
    messagesViewModel: MessagesViewModel,
    timesheetViewModel: TimesheetViewModel,
    notificationViewModel: NotificationViewModel
) {
    val currentScreen = remember { mutableStateOf<AdminScreen>(AdminScreen.Dashboard) }
    val selectedCalendarDate = remember { mutableStateOf("") }
    val taskDetailsBackTarget = remember { mutableStateOf<AdminScreen>(AdminScreen.Tasks) }
    val selectedTaskId = remember { mutableStateOf<String?>(null) }
    val cachedSelectedTask = remember { mutableStateOf<TaskData?>(null) }
    val showNewChatDialog = remember { mutableStateOf(false) }

    val users = usersViewModel.users
    val tasks = tasksViewModel.tasks
    val selectedUser = usersViewModel.selectedUser
    val loggedInUser = usersViewModel.loggedInUser

    LaunchedEffect(loggedInUser?.id) {
        val currentUserId = loggedInUser?.id
        if (!currentUserId.isNullOrBlank()) {
            messagesViewModel.startListeningForUser(currentUserId)
            notificationViewModel.startListeningForUser(currentUserId)
        } else {
            messagesViewModel.stopListening()
            notificationViewModel.stopListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            messagesViewModel.stopListening()
            notificationViewModel.stopListening()
        }
    }

    val taskAssignedUsers = users
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }

    val taskAssignedUserOptions = taskAssignedUsers
        .map { it.fullName }
        .filter { it.isNotBlank() }
        .distinct()

    val notifications = notificationViewModel.notifications
    val unreadNotificationCount = notificationViewModel.unreadCount

    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId.value }

    val profileFirstName = loggedInUser?.firstName ?: "Admin"
    val profileLastName = loggedInUser?.lastName ?: ""
    val profileUsername = loggedInUser?.username ?: ""
    val profileEmail = loggedInUser?.email ?: "admin@entreprisekilden.dk"
    val profilePhoneNumber = loggedInUser?.phoneNumber ?: ""
    val profileRole = loggedInUser?.role ?: "employee"

    val timesheetEmployees = timesheetViewModel.employees

    val availableChatRecipients = users.filter { user ->
        user.id.isNotBlank() && user.id != loggedInUser?.id
    }

    LaunchedEffect(selectedTask?.id, selectedTask?.customer, selectedTask?.date, selectedTask?.address) {
        if (selectedTask != null) {
            cachedSelectedTask.value = selectedTask
        }
    }

    if (showNewChatDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showNewChatDialog.value = false
            },
            title = {
                Text(
                    text = "Select recipient",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (availableChatRecipients.isEmpty()) {
                    Text("No available users found.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(availableChatRecipients, key = { it.id }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .clickable {
                                        val currentUser = loggedInUser ?: return@clickable
                                        showNewChatDialog.value = false
                                        messagesViewModel.createOrGetThread(
                                            currentUserId = currentUser.id,
                                            currentUserName = currentUser.fullName,
                                            recipientId = user.id,
                                            recipientName = user.fullName
                                        ) {
                                            currentScreen.value = AdminScreen.Chat
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFB7DDFC), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF49A7EE)
                                    )
                                }

                                Spacer(modifier = Modifier.size(12.dp))

                                Text(
                                    text = user.fullName,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showNewChatDialog.value = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFFF4F7FB)
        )
    }

    val goToDashboard = { currentScreen.value = AdminScreen.Dashboard }
    val goToMessages = { currentScreen.value = AdminScreen.Messages }
    val goToNotifications = {
        notificationViewModel.onNotificationsOpened()
        currentScreen.value = AdminScreen.Notifications
    }
    val goToProfile = { currentScreen.value = AdminScreen.Profile }
    val goToTasks = { currentScreen.value = AdminScreen.Tasks }
    val goToCreateTask = { currentScreen.value = AdminScreen.CreateTask }
    val goToCalendar = { currentScreen.value = AdminScreen.Calendar }
    val goToUsers = { currentScreen.value = AdminScreen.Employees }
    val goToCreateUser = { currentScreen.value = AdminScreen.CreateUser }
    val goToTimesheetEmployees = { currentScreen.value = AdminScreen.TimesheetEmployees }

    when (currentScreen.value) {
        AdminScreen.Dashboard -> {
            AdminDashboardScreen(
                unreadNotificationCount = unreadNotificationCount,
                onAllTasksClick = goToTasks,
                onCreateTaskClick = goToCreateTask,
                onCalendarClick = goToCalendar,
                onMessagesClick = goToMessages,
                onTimesheetClick = goToTimesheetEmployees,
                onUsersClick = goToUsers,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.Messages -> {
            MessagesScreen(
                threads = messagesViewModel.messageThreads,
                unreadNotificationCount = unreadNotificationCount,
                onThreadClick = { thread ->
                    val currentUser = loggedInUser ?: return@MessagesScreen
                    messagesViewModel.selectThread(
                        thread = thread,
                        currentUserId = currentUser.id
                    )
                    currentScreen.value = AdminScreen.Chat
                },
                onNewChatClick = {
                    showNewChatDialog.value = true
                },
                onDeleteThread = { thread ->
                    messagesViewModel.deleteThread(thread)
                },
                onBack = goToDashboard,
                onHomeClick = goToDashboard,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.Chat -> {
            val thread = messagesViewModel.selectedThread.value
            val messages = messagesViewModel.getMessagesForSelectedThread()

            if (thread != null) {
                LaunchedEffect(thread.id, loggedInUser?.id) {
                    val currentUserId = loggedInUser?.id ?: return@LaunchedEffect
                    messagesViewModel.markCurrentThreadAsRead(currentUserId)
                }

                ChatScreen(
                    thread = thread,
                    messages = messages,
                    loggedInUserId = loggedInUser?.id ?: "",
                    onBack = goToMessages,
                    onSendMessage = { msg ->
                        val currentUser = loggedInUser ?: return@ChatScreen

                        messagesViewModel.sendMessage(
                            senderId = currentUser.id,
                            message = msg
                        )
                    },
                    onSendImage = { imageUri ->
                        val currentUser = loggedInUser ?: return@ChatScreen

                        messagesViewModel.sendImageMessage(
                            senderId = currentUser.id,
                            imageUri = imageUri
                        )
                    },
                    onMessageTextChanged = { text ->
                        val currentUserId = loggedInUser?.id ?: return@ChatScreen
                        messagesViewModel.onMessageInputChanged(currentUserId, text)
                    },
                    onMarkAsRead = {
                        val currentUserId = loggedInUser?.id ?: return@ChatScreen
                        messagesViewModel.markCurrentThreadAsRead(currentUserId)
                    },
                    onStopTyping = {
                        val currentUserId = loggedInUser?.id ?: return@ChatScreen
                        messagesViewModel.clearTypingState(currentUserId)
                    }
                )
            } else {
                currentScreen.value = AdminScreen.Messages
            }
        }

        AdminScreen.Tasks -> {
            TasksScreen(
                tasks = tasks,
                assignedUserOptions = taskAssignedUserOptions,
                unreadNotificationCount = unreadNotificationCount,
                isLoading = tasksViewModel.isLoading.value,
                errorMessage = tasksViewModel.errorMessage.value,
                onRetry = {
                    tasksViewModel.retryLoadTasks()
                },
                onBack = goToDashboard,
                onCreateTaskClick = goToCreateTask,
                onDeleteTask = { taskId ->
                    if (selectedTaskId.value == taskId) {
                        selectedTaskId.value = null
                        cachedSelectedTask.value = null
                    }
                    tasksViewModel.deleteTask(taskId)
                },
                onTaskClick = { task ->
                    selectedTaskId.value = task.id
                    cachedSelectedTask.value = task
                    taskDetailsBackTarget.value = AdminScreen.Tasks
                    currentScreen.value = AdminScreen.TaskDetails
                },
                onStatusChange = { taskId, status ->
                    tasksViewModel.updateStatus(taskId, status)
                },
                onQuickUpdateTask = { updatedTask ->
                    tasksViewModel.updateTask(updatedTask)
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.TaskDetails -> {
            val taskToShow = selectedTask ?: cachedSelectedTask.value

            if (taskToShow != null) {
                TaskDetailsScreen(
                    task = taskToShow.copy(date = normalizeDateForDisplay(taskToShow.date)),
                    onBack = {
                        currentScreen.value = taskDetailsBackTarget.value
                    },
                    onSaveEdit = { updatedTask ->
                        val normalizedTask = updatedTask.copy(
                            date = normalizeDateForDisplay(updatedTask.date)
                        )
                        cachedSelectedTask.value = normalizedTask
                        selectedTaskId.value = normalizedTask.id
                        tasksViewModel.updateTask(normalizedTask)
                    },
                    assignedUserOptions = taskAssignedUserOptions,
                    unreadNotificationCount = unreadNotificationCount,
                    onHomeClick = goToDashboard,
                    onMessagesClick = goToMessages,
                    onNotificationsClick = goToNotifications,
                    onProfileClick = goToProfile
                )
            } else {
                currentScreen.value = AdminScreen.Tasks
            }
        }

        AdminScreen.CreateTask -> {
            CreateTaskScreen(
                unreadNotificationCount = unreadNotificationCount,
                onBack = goToDashboard,
                onCreateTask = { task ->
                    tasksViewModel.addTask(task.copy(date = normalizeDateForDisplay(task.date)))
                },
                assignedUserOptions = taskAssignedUsers,
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.Calendar -> {
            CalendarScreen(
                tasks = tasks.map { it.copy(date = normalizeDateForDisplay(it.date)) },
                unreadNotificationCount = unreadNotificationCount,
                onBack = goToDashboard,
                onDayClick = { selectedDate ->
                    selectedCalendarDate.value = normalizeDateForDisplay(selectedDate)
                    currentScreen.value = AdminScreen.CalendarDay
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.CalendarDay -> {
            val normalizedSelectedDate = normalizeDateForDisplay(selectedCalendarDate.value)
            val normalizedTasksForDay = tasks
                .map { it.copy(date = normalizeDateForDisplay(it.date)) }
                .filter { it.date == normalizedSelectedDate }

            CalendarDayScreen(
                selectedDate = normalizedSelectedDate,
                tasksForDay = normalizedTasksForDay,
                onBack = goToCalendar,
                onTaskClick = { task ->
                    selectedTaskId.value = task.id
                    cachedSelectedTask.value = task
                    taskDetailsBackTarget.value = AdminScreen.CalendarDay
                    currentScreen.value = AdminScreen.TaskDetails
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.TimesheetEmployees -> {
            TimesheetEmployeeListScreen(
                employees = timesheetEmployees,
                unreadNotificationCount = unreadNotificationCount,
                onBack = goToDashboard,
                onEmployeeClick = { employeeName ->
                    timesheetViewModel.selectEmployee(employeeName)
                    currentScreen.value = AdminScreen.Timesheet
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.Timesheet -> {
            TimesheetScreen(
                employeeName = timesheetViewModel.selectedEmployee.value,
                timesheets = timesheetViewModel.entriesForSelectedEmployee,
                unreadNotificationCount = unreadNotificationCount,
                onBack = goToTimesheetEmployees,
                onApproveEntry = { entryId ->
                    timesheetViewModel.approveEntry(entryId)
                },
                onDeclineEntry = { entryId ->
                    timesheetViewModel.declineEntry(entryId)
                },
                onUndoEntryStatus = { entryId ->
                    timesheetViewModel.undoEntryStatus(entryId)
                },
                onDeleteEntry = { entryId ->
                    timesheetViewModel.deleteEntry(entryId)
                },
                onAssignShift = { newEntry ->
                    timesheetViewModel.assignShift(newEntry)
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.Employees -> {
            EmployeeScreen(
                users = users,
                onBack = goToDashboard,
                onCreateUserClick = goToCreateUser,
                onUserClick = { user ->
                    usersViewModel.clearDeleteUserMessages()
                    usersViewModel.selectUser(user)
                    currentScreen.value = AdminScreen.UserDetails
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.UserDetails -> {
            val user = selectedUser
            if (user != null) {
                UserDetailsScreen(
                    user = user,
                    isCurrentLoggedInUser = loggedInUser?.id == user.id,
                    isDeleting = usersViewModel.isDeletingUser,
                    deleteErrorMessage = usersViewModel.deleteUserErrorMessage,
                    onBack = goToUsers,
                    onSaveUser = { updatedUser ->
                        usersViewModel.updateUser(updatedUser)
                    },
                    onDeleteUser = { userId ->
                        usersViewModel.deleteUser(userId) {
                            usersViewModel.clearSelectedUser()
                            currentScreen.value = AdminScreen.Employees
                        }
                    },
                    onClearDeleteMessage = {
                        usersViewModel.clearDeleteUserMessages()
                    },
                    onHomeClick = goToDashboard,
                    onMessagesClick = goToMessages,
                    onNotificationsClick = goToNotifications,
                    onProfileClick = goToProfile
                )
            } else {
                currentScreen.value = AdminScreen.Employees
            }
        }

        AdminScreen.CreateUser -> {
            CreateUserScreen(
                onBack = {
                    usersViewModel.clearCreateUserMessages()
                    goToUsers()
                },
                onCreateUser = { firstName, lastName, email, phoneNumber, username, password, role ->
                    usersViewModel.addUser(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phoneNumber = phoneNumber,
                        username = username,
                        password = password,
                        role = role
                    )
                },
                successMessage = usersViewModel.createUserMessage,
                backendErrorMessage = usersViewModel.createUserErrorMessage,
                onClearMessages = {
                    usersViewModel.clearCreateUserMessages()
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.Profile -> {
            ProfileScreen(
                email = profileEmail,
                firstName = profileFirstName,
                lastName = profileLastName,
                username = profileUsername,
                phoneNumber = profilePhoneNumber,
                role = profileRole,
                passwordSuccessMessage = usersViewModel.changePasswordMessage,
                passwordErrorMessage = usersViewModel.changePasswordErrorMessage,
                isChangingPassword = usersViewModel.isChangingPassword,
                onChangePassword = { currentPassword, newPassword, confirmPassword ->
                    usersViewModel.changeOwnPassword(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword
                    )
                },
                onSaveProfile = { updatedFirstName, updatedLastName, updatedUsername, updatedEmail, updatedPhoneNumber ->
                    val currentUser = usersViewModel.loggedInUser ?: return@ProfileScreen
                    val updatedUser = if (profileRole.equals("admin", ignoreCase = true)) {
                        currentUser.copy(
                            firstName = updatedFirstName,
                            lastName = updatedLastName,
                            username = updatedUsername,
                            email = updatedEmail,
                            phoneNumber = updatedPhoneNumber
                        )
                    } else {
                        currentUser.copy(
                            username = updatedUsername
                        )
                    }

                    usersViewModel.updateUser(updatedUser)
                },
                onClearPasswordMessages = {
                    usersViewModel.clearChangePasswordMessages()
                },
                onLogoutClick = {
                    selectedTaskId.value = null
                    cachedSelectedTask.value = null
                    messagesViewModel.stopListening()
                    notificationViewModel.stopListening()
                    usersViewModel.logout()
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        AdminScreen.Notifications -> {
            NotificationScreen(
                notifications = notifications,
                unreadCount = unreadNotificationCount,
                onBack = goToDashboard,
                onScreenOpened = {
                    notificationViewModel.onNotificationsOpened()
                },
                onMarkAllAsRead = {
                    notificationViewModel.markAllAsRead()
                },
                onNotificationClick = { notification ->
                    notificationViewModel.markAsRead(notification.id) {
                        when (notification.type) {
                            NotificationType.MESSAGE -> {
                                val threadId = notification.relatedThreadId
                                val currentUser = loggedInUser
                                if (
                                    threadId != null &&
                                    currentUser != null &&
                                    messagesViewModel.selectThreadById(
                                        threadId = threadId,
                                        currentUserId = currentUser.id
                                    )
                                ) {
                                    currentScreen.value = AdminScreen.Chat
                                } else {
                                    currentScreen.value = AdminScreen.Messages
                                }
                            }

                            NotificationType.TASK_ASSIGNED -> {
                                currentScreen.value = AdminScreen.Tasks
                            }
                        }
                    }
                },
                onDeleteNotification = {
                    notificationViewModel.deleteNotification(it.id)
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onProfileClick = goToProfile,
                onScreenClosed = {
                    notificationViewModel.setNotificationsScreenOpen(false)
                }
            )
        }
    }
}

private fun normalizeDateForDisplay(date: String): String {
    val inputFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    for (formatter in inputFormats) {
        try {
            val parsed = LocalDate.parse(date, formatter)
            return parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (_: Exception) {
        }
    }

    return date
}