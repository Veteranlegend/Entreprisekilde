package com.entreprisekilde.app.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.entreprisekilde.app.data.repository.messages.FirebaseMessagesRepository
import com.entreprisekilde.app.data.repository.notifications.FirebaseNotificationRepository
import com.entreprisekilde.app.data.repository.tasks.FirebaseTasksRepository
import com.entreprisekilde.app.data.repository.timesheet.FirebaseTimesheetRepository
import com.entreprisekilde.app.data.repository.users.FirebaseUsersRepository
import com.entreprisekilde.app.ui.auth.login.LoginScreen
import com.entreprisekilde.app.ui.navigation.admin.AdminAppFlow
import com.entreprisekilde.app.ui.navigation.employee.EmployeeAppFlow
import com.entreprisekilde.app.viewmodel.MessagesViewModel
import com.entreprisekilde.app.viewmodel.NotificationViewModel
import com.entreprisekilde.app.viewmodel.TasksViewModel
import com.entreprisekilde.app.viewmodel.TimesheetViewModel
import com.entreprisekilde.app.viewmodel.UsersViewModel

@Composable
fun EntreprisekildeApp() {
    // Grab the Application instance from the current Compose context.
    // UsersViewModel needs this, likely for auth/session handling or other
    // app-level dependencies.
    val application = LocalContext.current.applicationContext as Application

    // Repositories are created here and passed into the relevant ViewModels.
    // These act as the data layer bridge between Firebase and the UI state.
    val userRepository = FirebaseUsersRepository()
    val tasksRepository = FirebaseTasksRepository()
    val messagesRepository = FirebaseMessagesRepository()
    val timesheetRepository = FirebaseTimesheetRepository()
    val notificationRepository = FirebaseNotificationRepository()

    // UsersViewModel handles login state, logged-in user data, and auth checks.
    //
    // A custom factory is used because this ViewModel requires constructor params
    // instead of a no-arg constructor.
    val usersViewModel: UsersViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UsersViewModel(application, userRepository) as T
            }
        }
    )

    // TasksViewModel for task-related screens and business logic.
    val tasksViewModel: TasksViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TasksViewModel(tasksRepository) as T
            }
        }
    )

    // MessagesViewModel handles chat/message state and live updates.
    val messagesViewModel: MessagesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MessagesViewModel(messagesRepository) as T
            }
        }
    )

    // TimesheetViewModel manages timesheet data and related actions.
    val timesheetViewModel: TimesheetViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TimesheetViewModel(timesheetRepository) as T
            }
        }
    )

    // NotificationViewModel manages notification data and real-time listeners.
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationViewModel(notificationRepository) as T
            }
        }
    )

    // Pull the two key auth-related values from the users ViewModel.
    val loggedInUser = usersViewModel.loggedInUser
    val isCheckingAuth = usersViewModel.isCheckingAuth

    // Start observing authentication state once when the app composable enters.
    // This is what decides whether we show login or route into the app.
    LaunchedEffect(Unit) {
        usersViewModel.startAuthObserver()
    }

    // While auth is still being checked, do not render the app yet.
    // Right now this exits early without showing a loading UI.
    if (isCheckingAuth) return

    // No authenticated user yet -> show login screen.
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

    // Once we have a logged-in user, start live listeners for user-specific
    // notifications and messages.
    //
    // DisposableEffect ensures those listeners are cleaned up automatically
    // when the logged-in user changes or this composable leaves composition.
    DisposableEffect(loggedInUser.id) {
        notificationViewModel.startListeningForUser(loggedInUser.id)
        messagesViewModel.startListeningForUser(loggedInUser.id)

        onDispose {
            notificationViewModel.stopListening()
            messagesViewModel.stopListening()
        }
    }

    // Route the user into the correct app flow based on their role.
    // Admins get the full admin navigation graph with all shared ViewModels.
    // Everyone else is treated as an employee.
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