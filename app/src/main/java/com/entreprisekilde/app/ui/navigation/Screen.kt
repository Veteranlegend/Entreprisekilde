package com.entreprisekilde.app.ui.navigation

// Central list of app screens used for navigation.
// A sealed class is a good fit here because it keeps the set of valid screens fixed
// and makes navigation-related when/branch logic safer and easier to maintain.
sealed class Screen {

    // Authentication / entry screen.
    object Login : Screen()

    // Main landing screen after login.
    object Dashboard : Screen()

    // Message overview screen.
    object Messages : Screen()

    // Individual chat conversation screen.
    object Chat : Screen()

    // Notifications screen.
    object Notifications : Screen()

    // User profile screen.
    object Profile : Screen()

    // Task list / task overview screen.
    object Tasks : Screen()

    // Screen for creating a new task.
    object CreateTask : Screen()

    // Employee / user list screen.
    object Employees : Screen()

    // Screen for creating a new user.
    object CreateUser : Screen()

    // Detailed view for a selected user.
    object UserDetails : Screen()

    // Calendar overview screen.
    object Calendar : Screen()

    // Detailed day view inside the calendar flow.
    object CalendarDay : Screen()

    // Detailed view for a selected task.
    object TaskDetails : Screen()

    // Timesheet employee selection / overview screen.
    object TimesheetEmployees : Screen()

    // Individual timesheet screen.
    object Timesheet : Screen()
}