package com.entreprisekilde.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreprisekilde.app.data.repository.messages.FirebaseMessagesRepository
import com.entreprisekilde.app.data.repository.notifications.FirebaseNotificationRepository
import com.entreprisekilde.app.data.repository.tasks.FirebaseTasksRepository
import com.entreprisekilde.app.data.repository.timesheet.FirebaseTimesheetRepository
import com.entreprisekilde.app.data.repository.users.FirebaseUsersRepository
import com.entreprisekilde.app.ui.admin.messages.MessagesViewModel
import com.entreprisekilde.app.ui.admin.tasks.TasksViewModel
import com.entreprisekilde.app.ui.admin.timesheet.TimesheetViewModel
import com.entreprisekilde.app.ui.admin.users.UsersViewModel
import com.entreprisekilde.app.ui.auth.login.LoginScreen
import com.entreprisekilde.app.ui.navigation.admin.AdminAppFlow
import com.entreprisekilde.app.ui.navigation.employee.EmployeeAppFlow
import com.entreprisekilde.app.ui.notifications.NotificationViewModel

@Composable
fun EntreprisekildeApp() {

    // 🔹 Repositories
    val userRepository = FirebaseUsersRepository()
    val tasksRepository = FirebaseTasksRepository()
    val messagesRepository = FirebaseMessagesRepository()
    val timesheetRepository = FirebaseTimesheetRepository()
    val notificationRepository = FirebaseNotificationRepository()

    // 🔹 ViewModels
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

    val loggedInUser = usersViewModel.loggedInUser
    val isCheckingAuth = usersViewModel.isCheckingAuth

    // 🔹 Start auth listener
    LaunchedEffect(Unit) {
        usersViewModel.startAuthObserver()
    }

    // 🔹 WAIT until auth is checked
    if (isCheckingAuth) return

    // 🔹 NOT LOGGED IN → LOGIN
    if (loggedInUser == null) {
        LoginScreen(
            errorMessage = usersViewModel.loginErrorMessage,
            infoMessage = usersViewModel.loginInfoMessage,
            isLoading = usersViewModel.isLoading,
            isLocked = usersViewModel.isLocked,
            onLoginClick = { username, password ->
                usersViewModel.login(username, password)
            }
        )
        return
    }

    // 🔹 LOGGED IN → START NOTIFICATIONS
    LaunchedEffect(loggedInUser.id) {
        notificationViewModel.startListeningForUser(loggedInUser.id)
    }

    // 🔹 ROUTE BY ROLE
    if (loggedInUser.role.equals("admin", ignoreCase = true)) {
        AdminAppFlow(
            usersViewModel = usersViewModel,
            tasksViewModel = tasksViewModel,
            messagesViewModel = messagesViewModel,
            timesheetViewModel = timesheetViewModel,
            notificationViewModel = notificationViewModel
        )
    } else {
        EmployeeAppFlow()
    }
}