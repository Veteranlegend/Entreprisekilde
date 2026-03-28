package com.entreprisekilde.app.ui.navigation
import com.entreprisekilde.app.data.repository.timesheet.DemoTimesheetRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreprisekilde.app.data.repository.users.DemoUsersRepository
import com.entreprisekilde.app.ui.admin.calendar.CalendarDayScreen
import com.entreprisekilde.app.ui.admin.calendar.CalendarScreen
import com.entreprisekilde.app.data.repository.tasks.DemoTasksRepository
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
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.entreprisekilde.app.ui.notifications.NotificationViewModel
import com.entreprisekilde.app.data.repository.messages.DemoMessagesRepository
import com.entreprisekilde.app.data.repository.notifications.DemoNotificationRepository
@Composable
fun EntreprisekildeApp() {

    val currentScreen = remember { mutableStateOf<Screen>(Screen.Login) }
    val selectedCalendarDate = remember { mutableStateOf("") }
    val taskDetailsBackTarget = remember { mutableStateOf<Screen>(Screen.Tasks) }
    val profileImageUri = remember { mutableStateOf<String?>(null) }
    val selectedTaskId = remember { mutableStateOf<String?>(null) }


    val userRepository = remember { DemoUsersRepository() }
    val tasksRepository = remember { DemoTasksRepository() }
    val messagesRepository = remember { DemoMessagesRepository() }
    val timesheetRepository = remember { DemoTimesheetRepository() }
    val notificationRepository = remember { DemoNotificationRepository() }


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
    val notifications = notificationViewModel.notifications
    val unreadNotificationCount = notificationViewModel.unreadCount()

    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId.value }
    val selectedUser = usersViewModel.selectedUser
    val loggedInUser = usersViewModel.loggedInUser

    val profileFirstName = loggedInUser?.firstName ?: "Admin"
    val profileLastName = loggedInUser?.lastName ?: ""
    val profileEmail = loggedInUser?.email ?: "admin@entreprisekilden.dk"
    val profilePhoneNumber = loggedInUser?.phoneNumber ?: ""

    val timesheetEmployees = timesheetViewModel.getEmployees()

    LaunchedEffect(loggedInUser) {
        if (loggedInUser != null && currentScreen.value == Screen.Login) {
            currentScreen.value = Screen.Dashboard
        }
    }

    val goToDashboard = { currentScreen.value = Screen.Dashboard }
    val goToMessages = { currentScreen.value = Screen.Messages }
    val goToNotifications = { currentScreen.value = Screen.Notifications }
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

            if (thread != null && messages != null) {
                ChatScreen(
                    thread = thread,
                    messages = messages,
                    onBack = goToMessages,
                    onSendMessage = { msg ->
                        messagesViewModel.sendMessage(msg)

                        notificationViewModel.addMessageNotification(
                            senderName = thread.name,
                            threadId = thread.id
                        )
                    }
                )
            }
        }

        Screen.Tasks -> {
            TasksScreen(
                tasks = tasks,
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
                    assignedUserOptions = users
                        .map { it.username }
                        .filter { it.isNotBlank() }
                        .distinct(),
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

                    notificationViewModel.addTaskAssignedNotification(
                        taskName = task.customer,
                        assignedTo = task.assignTo
                    )

                    currentScreen.value = Screen.Tasks
                },
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
                timesheets = timesheetViewModel.getEntriesForSelectedEmployee(),
                unreadNotificationCount = unreadNotificationCount,
                onBack = goToTimesheetEmployees,
                onApproveEntry = { entryId ->
                    timesheetViewModel.approveEntry(entryId)
                },
                onDeclineEntry = { entryId ->
                    timesheetViewModel.declineEntry(entryId)
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
                        currentScreen.value = Screen.Employees
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
                onBack = goToUsers,
                onCreateUser = { firstName, lastName, email, phoneNumber, username, password ->
                    usersViewModel.addUser(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phoneNumber = phoneNumber,
                        username = username,
                        password = password
                    )
                    currentScreen.value = Screen.Employees
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
                onMarkAllAsRead = { notificationViewModel.markAllAsRead() },
                onNotificationClick = { notification ->
                    notificationViewModel.markAsRead(notification.id)

                    when (notification.type) {
                        NotificationType.MESSAGE -> {
                            val id = notification.relatedThreadId
                            if (id != null && messagesViewModel.selectThreadById(id)) {
                                currentScreen.value = Screen.Chat
                            } else {
                                currentScreen.value = Screen.Messages
                            }
                        }

                        NotificationType.TASK_ASSIGNED -> {
                            currentScreen.value = Screen.Tasks
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

        else -> {
            currentScreen.value = Screen.Dashboard
        }
    }
}