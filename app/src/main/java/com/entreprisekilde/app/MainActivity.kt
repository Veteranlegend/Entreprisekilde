package com.entreprisekilde.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.entreprisekilde.app.ui.admin.dashboard.AdminDashboardScreen
import com.entreprisekilde.app.ui.admin.management.CalendarDayScreen
import com.entreprisekilde.app.ui.admin.management.CalendarScreen
import com.entreprisekilde.app.ui.admin.management.CreateTaskScreen
import com.entreprisekilde.app.ui.admin.management.CreateUserScreen
import com.entreprisekilde.app.ui.admin.management.EmployeeScreen
import com.entreprisekilde.app.ui.admin.management.TaskData
import com.entreprisekilde.app.ui.admin.management.TasksScreen
import com.entreprisekilde.app.ui.auth.login.LoginScreen
import com.entreprisekilde.app.ui.theme.EntreprisekildeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EntreprisekildeTheme {

                val currentScreen = remember { mutableStateOf("login") }
                val selectedCalendarDate = remember { mutableStateOf("") }

                val tasks = remember {
                    mutableStateListOf(
                        TaskData(
                            customer = "Painting the Wall",
                            phoneNumber = "12345678",
                            address = "Roskilde",
                            date = "08/03/2026",
                            assignTo = "John",
                            taskDetails = "Paint wall",
                            status = "Pending"
                        ),
                        TaskData(
                            customer = "Installation",
                            phoneNumber = "87654321",
                            address = "Copenhagen",
                            date = "26/03/2026",
                            assignTo = "Peter",
                            taskDetails = "Install equipment",
                            status = "In-progress"
                        ),
                        TaskData(
                            customer = "Bathroom Renovation",
                            phoneNumber = "11112222",
                            address = "Lyngby",
                            date = "14/02/2026",
                            assignTo = "John",
                            taskDetails = "Renovate bathroom",
                            status = "Complete"
                        ),
                        TaskData(
                            customer = "Fix Sink Leak",
                            phoneNumber = "11223344",
                            address = "Office",
                            date = "14/03/2026",
                            assignTo = "John",
                            taskDetails = "Fix kitchen sink leak",
                            status = "In-progress"
                        ),
                        TaskData(
                            customer = "Fix Sink Leak",
                            phoneNumber = "55667788",
                            address = "Office",
                            date = "14/03/2026",
                            assignTo = "Peter",
                            taskDetails = "Replace damaged pipe",
                            status = "In-progress"
                        ),
                        TaskData(
                            customer = "Fix Sink Leak",
                            phoneNumber = "99887766",
                            address = "Office",
                            date = "14/03/2026",
                            assignTo = "John",
                            taskDetails = "Final inspection",
                            status = "Complete"
                        )
                    )
                }

                when (currentScreen.value) {

                    "login" -> LoginScreen(
                        onLoginClick = {
                            currentScreen.value = "dashboard"
                        }
                    )

                    "dashboard" -> AdminDashboardScreen(
                        onAllTasksClick = {
                            currentScreen.value = "tasks"
                        },
                        onCreateTaskClick = {
                            currentScreen.value = "createTask"
                        },
                        onCalendarClick = {
                            currentScreen.value = "calendar"
                        },
                        onUsersClick = {
                            currentScreen.value = "employees"
                        }
                    )

                    "tasks" -> TasksScreen(
                        tasks = tasks,
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onCreateTaskClick = {
                            currentScreen.value = "createTask"
                        },
                        onDeleteTask = { index ->
                            if (index in tasks.indices) {
                                tasks.removeAt(index)
                            }
                        }
                    )

                    "createTask" -> CreateTaskScreen(
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onCreateTask = { newTask ->
                            tasks.add(0, newTask)
                            currentScreen.value = "tasks"
                        }
                    )

                    "employees" -> EmployeeScreen(
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onCreateUserClick = {
                            currentScreen.value = "createUser"
                        }
                    )

                    "createUser" -> CreateUserScreen(
                        onBack = {
                            currentScreen.value = "employees"
                        },
                        onAddUserClick = {
                            currentScreen.value = "employees"
                        }
                    )

                    "calendar" -> CalendarScreen(
                        tasks = tasks,
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onDayClick = { date: String ->
                            selectedCalendarDate.value = date
                            currentScreen.value = "calendarDay"
                        }
                    )

                    "calendarDay" -> CalendarDayScreen(
                        selectedDate = selectedCalendarDate.value,
                        tasksForDay = tasks.filter { it.date == selectedCalendarDate.value },
                        onBack = {
                            currentScreen.value = "calendar"
                        }
                    )
                }
            }
        }
    }
}