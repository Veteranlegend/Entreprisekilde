package com.entreprisekilde.app.ui.navigation

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object Messages : Screen()
    object Chat : Screen()
    object Notifications : Screen()
    object Profile : Screen()
    object Tasks : Screen()
    object CreateTask : Screen()
    object Employees : Screen()
    object CreateUser : Screen()
    object UserDetails : Screen()
    object Calendar : Screen()
    object CalendarDay : Screen()
    object TaskDetails : Screen()
    object TimesheetEmployees : Screen()
    object Timesheet : Screen()
}