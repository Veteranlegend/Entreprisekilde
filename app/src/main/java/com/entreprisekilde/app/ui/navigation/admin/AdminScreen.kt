package com.entreprisekilde.app.ui.navigation.admin

sealed class AdminScreen {
    object Dashboard : AdminScreen()
    object Messages : AdminScreen()
    object Chat : AdminScreen()
    object Notifications : AdminScreen()
    object Profile : AdminScreen()
    object Tasks : AdminScreen()
    object CreateTask : AdminScreen()
    object Employees : AdminScreen()
    object CreateUser : AdminScreen()
    object UserDetails : AdminScreen()
    object Calendar : AdminScreen()
    object CalendarDay : AdminScreen()
    object TaskDetails : AdminScreen()
    object TimesheetEmployees : AdminScreen()
    object Timesheet : AdminScreen()
}