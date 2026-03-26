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
import com.entreprisekilde.app.ui.admin.management.EmployeeUser
import com.entreprisekilde.app.ui.admin.management.ShiftApprovalStatus
import com.entreprisekilde.app.ui.admin.management.TaskData
import com.entreprisekilde.app.ui.admin.management.TaskDetailsScreen
import com.entreprisekilde.app.ui.admin.management.TasksScreen
import com.entreprisekilde.app.ui.admin.management.TimesheetEmployeeListScreen
import com.entreprisekilde.app.ui.admin.management.TimesheetEntry
import com.entreprisekilde.app.ui.admin.management.TimesheetScreen
import com.entreprisekilde.app.ui.admin.management.UserDetailsScreen
import com.entreprisekilde.app.ui.auth.login.LoginScreen
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

                val users = remember {
                    mutableStateListOf(
                        EmployeeUser(
                            id = 1,
                            firstName = "Rasmus",
                            lastName = "Jensen",
                            email = "rasmus.jensen@entreprisekilde.dk",
                            phoneNumber = "12341234"
                        ),
                        EmployeeUser(
                            id = 2,
                            firstName = "Tomas",
                            lastName = "Larsen",
                            email = "tomas.larsen@entreprisekilde.dk",
                            phoneNumber = "22334455"
                        ),
                        EmployeeUser(
                            id = 3,
                            firstName = "Peter",
                            lastName = "Hansen",
                            email = "peter.hansen@entreprisekilde.dk",
                            phoneNumber = "33445566"
                        ),
                        EmployeeUser(
                            id = 4,
                            firstName = "John",
                            lastName = "Miller",
                            email = "john.miller@entreprisekilde.dk",
                            phoneNumber = "44556677"
                        ),
                        EmployeeUser(
                            id = 5,
                            firstName = "Ahmad",
                            lastName = "El Haj",
                            email = "ahmad.elhaj@entreprisekilde.dk",
                            phoneNumber = "55667788"
                        ),
                        EmployeeUser(
                            id = 6,
                            firstName = "Lars",
                            lastName = "Nielsen",
                            email = "lars.nielsen@entreprisekilde.dk",
                            phoneNumber = "66778899"
                        )
                    )
                }

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

                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val today = LocalDate.now()

                val timesheetEntries = remember {
                    mutableStateListOf(
                        TimesheetEntry(
                            date = today.format(formatter),
                            fromTime = "09:30 AM",
                            toTime = "06:30 PM",
                            employeeName = "Rasmus Jensen",
                            submittedHours = 9,
                            assignedHours = 9,
                            approvalStatus = ShiftApprovalStatus.Pending
                        ),
                        TimesheetEntry(
                            date = today.minusDays(1).format(formatter),
                            fromTime = "09:30 AM",
                            toTime = "06:30 PM",
                            employeeName = "Rasmus Jensen",
                            submittedHours = 9,
                            assignedHours = 9,
                            approvalStatus = ShiftApprovalStatus.Approved
                        ),
                        TimesheetEntry(
                            date = today.minusDays(2).format(formatter),
                            fromTime = "09:30 AM",
                            toTime = "06:30 PM",
                            employeeName = "Rasmus Jensen",
                            submittedHours = 8,
                            assignedHours = 9,
                            approvalStatus = ShiftApprovalStatus.Pending
                        ),
                        TimesheetEntry(
                            date = today.plusDays(1).format(formatter),
                            fromTime = "09:30 AM",
                            toTime = "06:30 PM",
                            employeeName = "Rasmus Jensen",
                            submittedHours = 0,
                            assignedHours = 9,
                            approvalStatus = ShiftApprovalStatus.Pending
                        ),
                        TimesheetEntry(
                            date = today.minusDays(1).format(formatter),
                            fromTime = "08:00 AM",
                            toTime = "04:00 PM",
                            employeeName = "John Miller",
                            submittedHours = 8,
                            assignedHours = 8,
                            approvalStatus = ShiftApprovalStatus.Approved
                        ),
                        TimesheetEntry(
                            date = today.plusDays(2).format(formatter),
                            fromTime = "10:00 AM",
                            toTime = "06:00 PM",
                            employeeName = "John Miller",
                            submittedHours = 0,
                            assignedHours = 8,
                            approvalStatus = ShiftApprovalStatus.Pending
                        ),
                        TimesheetEntry(
                            date = today.format(formatter),
                            fromTime = "09:00 AM",
                            toTime = "05:30 PM",
                            employeeName = "Peter Hansen",
                            submittedHours = 8,
                            assignedHours = 8,
                            approvalStatus = ShiftApprovalStatus.Pending
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
                        onTimesheetClick = {
                            currentScreen.value = "timesheetEmployees"
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
                        },
                        onTaskClick = { clickedTask ->
                            selectedTaskIndex.value = tasks.indexOf(clickedTask)
                            if (selectedTaskIndex.value != -1) {
                                taskDetailsBackTarget.value = "tasks"
                                currentScreen.value = "taskDetails"
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
                        users = users,
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onCreateUserClick = {
                            currentScreen.value = "createUser"
                        },
                        onUserClick = { user ->
                            selectedUser.value = user
                            currentScreen.value = "userDetails"
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

                    "userDetails" -> {
                        val currentUser = selectedUser.value
                        if (currentUser != null) {
                            UserDetailsScreen(
                                user = currentUser,
                                onBack = {
                                    currentScreen.value = "employees"
                                },
                                onSaveUser = { updatedUser ->
                                    val index = users.indexOfFirst { it.id == updatedUser.id }
                                    if (index != -1) {
                                        users[index] = updatedUser
                                        selectedUser.value = updatedUser
                                    }
                                }
                            )
                        }
                    }

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
                        },
                        onTaskClick = { clickedTask ->
                            selectedTaskIndex.value = tasks.indexOf(clickedTask)
                            if (selectedTaskIndex.value != -1) {
                                taskDetailsBackTarget.value = "calendarDay"
                                currentScreen.value = "taskDetails"
                            }
                        }
                    )

                    "taskDetails" -> {
                        val taskIndex = selectedTaskIndex.value
                        if (taskIndex in tasks.indices) {
                            TaskDetailsScreen(
                                task = tasks[taskIndex],
                                onBack = {
                                    currentScreen.value = taskDetailsBackTarget.value
                                },
                                onSaveEdit = { updatedTask ->
                                    if (taskIndex in tasks.indices) {
                                        tasks[taskIndex] = updatedTask
                                    }
                                }
                            )
                        }
                    }

                    "timesheetEmployees" -> TimesheetEmployeeListScreen(
                        employees = timesheetEntries.map { it.employeeName },
                        onBack = {
                            currentScreen.value = "dashboard"
                        },
                        onEmployeeClick = { employee ->
                            selectedTimesheetEmployee.value = employee
                            currentScreen.value = "timesheet"
                        }
                    )

                    "timesheet" -> {
                        val employeeEntries = timesheetEntries.filter {
                            it.employeeName == selectedTimesheetEmployee.value
                        }

                        TimesheetScreen(
                            employeeName = selectedTimesheetEmployee.value,
                            timesheets = employeeEntries,
                            onBack = {
                                currentScreen.value = "timesheetEmployees"
                            },
                            onApproveEntry = { localIndex ->
                                val realIndexes = timesheetEntries.mapIndexedNotNull { index, entry ->
                                    if (entry.employeeName == selectedTimesheetEmployee.value) index else null
                                }
                                if (localIndex in realIndexes.indices) {
                                    val realIndex = realIndexes[localIndex]
                                    timesheetEntries[realIndex] = timesheetEntries[realIndex].copy(
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
                                    timesheetEntries[realIndex] = timesheetEntries[realIndex].copy(
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
                            }
                        )
                    }
                }
            }
        }
    }
}