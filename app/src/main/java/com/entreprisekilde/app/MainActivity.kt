package com.entreprisekilde.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.entreprisekilde.app.ui.admin.dashboard.AdminDashboardScreen
import com.entreprisekilde.app.ui.admin.management.CreateTaskScreen
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
                        )
                    )
                }

                when (currentScreen.value) {

                    // 🔹 Login → Dashboard
                    "login" -> LoginScreen(
                        onLoginClick = {
                            currentScreen.value = "dashboard"
                        }
                    )

                    // 🔹 Dashboard
                    "dashboard" -> AdminDashboardScreen(
                        onAllTasksClick = {
                            currentScreen.value = "tasks"
                        },
                        onCreateTaskClick = {
                            currentScreen.value = "createTask"
                        }
                    )

                    // 🔹 All Tasks (with back to dashboard)
                    "tasks" -> TasksScreen(
                        tasks = tasks,
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onCreateTaskClick = {
                            currentScreen.value = "createTask"
                        }
                    )

                    // 🔹 Create Task
                    "createTask" -> CreateTaskScreen(
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onCreateTask = { newTask ->
                            tasks.add(0, newTask)
                            currentScreen.value = "tasks"
                        }
                    )
                }
            }
        }
    }
}