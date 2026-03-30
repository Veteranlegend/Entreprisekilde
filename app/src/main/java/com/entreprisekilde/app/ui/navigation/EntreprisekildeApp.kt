package com.entreprisekilde.app.ui.navigation

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.entreprisekilde.app.data.repository.messages.FirebaseMessagesRepository
import com.entreprisekilde.app.data.repository.notifications.FirebaseNotificationRepository
import com.entreprisekilde.app.data.repository.tasks.FirebaseTasksRepository
import com.entreprisekilde.app.data.repository.timesheet.FirebaseTimesheetRepository
import com.entreprisekilde.app.data.repository.users.FirebaseUsersRepository
import com.entreprisekilde.app.ui.admin.calendar.CalendarDayScreen
import com.entreprisekilde.app.ui.admin.calendar.CalendarScreen
import com.entreprisekilde.app.ui.admin.dashboard.AdminDashboardScreen
import com.entreprisekilde.app.ui.admin.messages.ChatScreen
import com.entreprisekilde.app.ui.admin.messages.MessagesScreen
import com.entreprisekilde.app.ui.admin.messages.MessagesViewModel
import com.entreprisekilde.app.ui.admin.profile.ProfileScreen
import com.entreprisekilde.app.ui.admin.tasks.CreateTaskScreen
import com.entreprisekilde.app.ui.admin.tasks.TaskDetailsScreen
import com.entreprisekilde.app.ui.admin.tasks.TasksScreen
import com.entreprisekilde.app.ui.admin.tasks.TasksViewModel
import com.entreprisekilde.app.ui.admin.timesheet.TimesheetEmployeeListScreen
import com.entreprisekilde.app.ui.admin.timesheet.TimesheetScreen
import com.entreprisekilde.app.ui.admin.timesheet.TimesheetViewModel
import com.entreprisekilde.app.ui.admin.users.CreateUserScreen
import com.entreprisekilde.app.ui.admin.users.EmployeeScreen
import com.entreprisekilde.app.ui.admin.users.UserDetailsScreen
import com.entreprisekilde.app.ui.admin.users.UsersViewModel
import com.entreprisekilde.app.ui.auth.login.LoginScreen
import com.entreprisekilde.app.ui.notifications.NotificationScreen
import com.entreprisekilde.app.ui.notifications.NotificationViewModel

@Composable
fun EntreprisekildeApp() {
    val currentScreen = remember { mutableStateOf<Screen>(Screen.Login) }
    val selectedCalendarDate = remember { mutableStateOf("") }
    val taskDetailsBackTarget = remember { mutableStateOf<Screen>(Screen.Tasks) }
    val profileImageUri = remember { mutableStateOf<String?>(null) }
    val selectedTaskId = remember { mutableStateOf<String?>(null) }
    val showNewChatDialog = remember { mutableStateOf(false) }

    val userRepository = remember { FirebaseUsersRepository() }
    val tasksRepository = remember { FirebaseTasksRepository() }
    val messagesRepository = remember { FirebaseMessagesRepository() }
    val timesheetRepository = remember { FirebaseTimesheetRepository() }
    val notificationRepository = remember { FirebaseNotificationRepository() }

    val usersViewModel: UsersViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UsersViewModel(userRepository) as T
            }
        }
    )

    val tasksViewModel: TasksViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TasksViewModel(tasksRepository) as T
            }
        }
    )

    val messagesViewModel: MessagesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MessagesViewModel(messagesRepository) as T
            }
        }
    )

    val timesheetViewModel: TimesheetViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TimesheetViewModel(timesheetRepository) as T
            }
        }
    )

    val notificationViewModel: NotificationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationViewModel(notificationRepository) as T
            }
        }
    )

    val users = usersViewModel.users
    val tasks = tasksViewModel.tasks
    val taskAssignedUsers = users
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }

    android.util.Log.d("USER_DEBUG", "ALL USERS = ${users.map { "${it.fullName} | id=${it.id}" }}")
    android.util.Log.d("USER_DEBUG", "TASK USERS = ${taskAssignedUsers.map { "${it.fullName} | id=${it.id}" }}")

    val taskAssignedUserOptions = taskAssignedUsers
        .map { it.fullName }
        .filter { it.isNotBlank() }
        .distinct()

    val notifications = notificationViewModel.notifications
    val unreadNotificationCount = notificationViewModel.unreadCount

    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId.value }
    val selectedUser = usersViewModel.selectedUser
    val loggedInUser = usersViewModel.loggedInUser
    val isCheckingAuth = usersViewModel.isCheckingAuth

    val profileFirstName = loggedInUser?.firstName ?: "Admin"
    val profileLastName = loggedInUser?.lastName ?: ""
    val profileEmail = loggedInUser?.email ?: "admin@entreprisekilden.dk"
    val profilePhoneNumber = loggedInUser?.phoneNumber ?: ""

    val timesheetEmployees = timesheetViewModel.employees

    val availableChatRecipients = users.filter { user ->
        user.id.isNotBlank() && user.id != loggedInUser?.id
    }

    LaunchedEffect(Unit) {
        usersViewModel.startAuthObserver()
    }



    LaunchedEffect(loggedInUser?.id, isCheckingAuth) {
        if (!isCheckingAuth) {
            if (loggedInUser == null) {
                notificationViewModel.stopListening()
                currentScreen.value = Screen.Login
            } else {
                notificationViewModel.startListeningForUser(loggedInUser.id)

                if (currentScreen.value == Screen.Login) {
                    currentScreen.value = Screen.Dashboard
                }
            }
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
                                        showNewChatDialog.value = false
                                        messagesViewModel.createOrGetThread(
                                            recipientId = user.id,
                                            recipientName = user.fullName
                                        ) {
                                            currentScreen.value = Screen.Chat
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

    val goToDashboard = { currentScreen.value = Screen.Dashboard }
    val goToMessages = { currentScreen.value = Screen.Messages }
    val goToNotifications = {
        notificationViewModel.onNotificationsOpened()
        currentScreen.value = Screen.Notifications
    }
    val goToProfile = { currentScreen.value = Screen.Profile }
    val goToTasks = { currentScreen.value = Screen.Tasks }
    val goToCreateTask = { currentScreen.value = Screen.CreateTask }
    val goToCalendar = { currentScreen.value = Screen.Calendar }
    val goToUsers = { currentScreen.value = Screen.Employees }
    val goToCreateUser = { currentScreen.value = Screen.CreateUser }
    val goToTimesheetEmployees = { currentScreen.value = Screen.TimesheetEmployees }

    when (currentScreen.value) {
        Screen.Login -> {
            LoginScreen(
                errorMessage = usersViewModel.loginErrorMessage,
                onLoginClick = { username, password ->
                    usersViewModel.login(username, password)
                }
            )
        }

        Screen.Dashboard -> {
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

        Screen.Messages -> {
            MessagesScreen(
                threads = messagesViewModel.messageThreads,
                unreadNotificationCount = unreadNotificationCount,
                onThreadClick = {
                    messagesViewModel.selectThread(it)
                    currentScreen.value = Screen.Chat
                },
                onNewChatClick = {
                    showNewChatDialog.value = true
                },
                onDeleteThread = {
                    messagesViewModel.deleteThread(it)
                },
                onBack = goToDashboard,
                onHomeClick = goToDashboard,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        Screen.Chat -> {
            val thread = messagesViewModel.selectedThread.value
            val messages = messagesViewModel.getMessagesForSelectedThread()

            if (thread != null) {
                ChatScreen(
                    thread = thread,
                    messages = messages,
                    loggedInUserId = loggedInUser?.id ?: "",
                    onBack = goToMessages,
                    onSendMessage = { msg ->
                        val senderId = loggedInUser?.id ?: return@ChatScreen
                        messagesViewModel.sendMessage(
                            senderId = senderId,
                            message = msg
                        )
                    }
                )
            } else {
                currentScreen.value = Screen.Messages
            }
        }

        Screen.Tasks -> {
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
                    }
                    tasksViewModel.deleteTask(taskId)
                },
                onTaskClick = { task ->
                    selectedTaskId.value = task.id
                    taskDetailsBackTarget.value = Screen.Tasks
                    currentScreen.value = Screen.TaskDetails
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

        Screen.TaskDetails -> {
            val task = selectedTask
            if (task != null) {
                TaskDetailsScreen(
                    task = task,
                    onBack = {
                        currentScreen.value = taskDetailsBackTarget.value
                    },
                    onSaveEdit = { updatedTask ->
                        tasksViewModel.updateTask(updatedTask)
                        currentScreen.value = taskDetailsBackTarget.value
                    },
                    assignedUserOptions = taskAssignedUserOptions,
                    unreadNotificationCount = unreadNotificationCount,
                    onHomeClick = goToDashboard,
                    onMessagesClick = goToMessages,
                    onNotificationsClick = goToNotifications,
                    onProfileClick = goToProfile
                )
            } else {
                currentScreen.value = Screen.Tasks
            }
        }

        Screen.CreateTask -> {
                CreateTaskScreen(
                    unreadNotificationCount = unreadNotificationCount,
                    onBack = goToDashboard,
                    onCreateTask = { task ->
                        tasksViewModel.addTask(task)

                        if (task.assignedUserId.isNotBlank()) {
                            notificationViewModel.addTaskAssignedNotification(
                                taskName = task.customer,
                                assignedUserId = task.assignedUserId,
                                assignedToName = task.assignTo
                            )
                        }
                    },
                    assignedUserOptions = taskAssignedUsers,
                    onHomeClick = goToDashboard,
                    onMessagesClick = goToMessages,
                    onNotificationsClick = goToNotifications,
                    onProfileClick = goToProfile
                )
            }

        Screen.Calendar -> {
            CalendarScreen(
                tasks = tasks,
                unreadNotificationCount = unreadNotificationCount,
                onBack = goToDashboard,
                onDayClick = { selectedDate ->
                    selectedCalendarDate.value = selectedDate
                    currentScreen.value = Screen.CalendarDay
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        Screen.CalendarDay -> {
            CalendarDayScreen(
                selectedDate = selectedCalendarDate.value,
                tasksForDay = tasks.filter { it.date == selectedCalendarDate.value },
                onBack = goToCalendar,
                onTaskClick = { task ->
                    selectedTaskId.value = task.id
                    taskDetailsBackTarget.value = Screen.CalendarDay
                    currentScreen.value = Screen.TaskDetails
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        Screen.TimesheetEmployees -> {
            TimesheetEmployeeListScreen(
                employees = timesheetEmployees,
                unreadNotificationCount = unreadNotificationCount,
                onBack = goToDashboard,
                onEmployeeClick = { employeeName ->
                    timesheetViewModel.selectEmployee(employeeName)
                    currentScreen.value = Screen.Timesheet
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        Screen.Timesheet -> {
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

        Screen.Employees -> {
            EmployeeScreen(
                users = users,
                onBack = goToDashboard,
                onCreateUserClick = goToCreateUser,
                onUserClick = { user ->
                    usersViewModel.selectUser(user)
                    currentScreen.value = Screen.UserDetails
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        Screen.UserDetails -> {
            val user = selectedUser
            if (user != null) {
                UserDetailsScreen(
                    user = user,
                    onBack = goToUsers,
                    onSaveUser = { updatedUser ->
                        usersViewModel.updateUser(updatedUser)
                    },
                    onHomeClick = goToDashboard,
                    onMessagesClick = goToMessages,
                    onNotificationsClick = goToNotifications,
                    onProfileClick = goToProfile
                )
            } else {
                currentScreen.value = Screen.Employees
            }
        }

        Screen.CreateUser -> {
            CreateUserScreen(
                onBack = {
                    usersViewModel.clearCreateUserMessages()
                    goToUsers()
                },
                onCreateUser = { firstName, lastName, email, phoneNumber, username, password ->
                    usersViewModel.addUser(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phoneNumber = phoneNumber,
                        username = username,
                        password = password
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

        Screen.Profile -> {
            ProfileScreen(
                email = profileEmail,
                firstName = profileFirstName,
                lastName = profileLastName,
                phoneNumber = profilePhoneNumber,
                profileImageUri = profileImageUri.value,
                onProfileImageChange = { newUri ->
                    profileImageUri.value = newUri
                },
                onLogoutClick = {
                    selectedTaskId.value = null
                    usersViewModel.logout()
                    currentScreen.value = Screen.Login
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onNotificationsClick = goToNotifications,
                onProfileClick = goToProfile
            )
        }

        Screen.Notifications -> {
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
                                if (threadId != null && messagesViewModel.selectThreadById(threadId)) {
                                    currentScreen.value = Screen.Chat
                                } else {
                                    currentScreen.value = Screen.Messages
                                }
                            }

                            NotificationType.TASK_ASSIGNED -> {
                                currentScreen.value = Screen.Tasks
                            }
                        }
                    }
                },
                onDeleteNotification = {
                    notificationViewModel.deleteNotification(it.id)
                },
                onHomeClick = goToDashboard,
                onMessagesClick = goToMessages,
                onProfileClick = goToProfile
            )
        }
    }
}