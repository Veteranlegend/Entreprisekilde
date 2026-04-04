package com.entreprisekilde.app.ui.navigation.admin

/**
 * Represents all possible screens/routes within the admin section of the app.
 *
 * We use a sealed class here instead of enums or strings because:
 * - It gives us type safety (no invalid screen names)
 * - Makes navigation easier to manage and refactor
 * - Allows us to use `when` statements exhaustively (compiler helps us)
 *
 * Each object represents a unique screen in the admin flow.
 */
sealed class AdminScreen {

    /**
     * Main landing screen for admins.
     * Likely shows an overview such as stats, quick actions, etc.
     */
    object Dashboard : AdminScreen()

    /**
     * Screen showing internal messages or communication threads.
     */
    object Messages : AdminScreen()

    /**
     * Dedicated chat interface (possibly real-time messaging).
     */
    object Chat : AdminScreen()

    /**
     * Displays system or user-related notifications.
     */
    object Notifications : AdminScreen()

    /**
     * Admin's personal profile screen (settings, info, etc.).
     */
    object Profile : AdminScreen()

    /**
     * List or overview of tasks assigned or created.
     */
    object Tasks : AdminScreen()

    /**
     * Screen used to create a new task.
     */
    object CreateTask : AdminScreen()

    /**
     * Displays a list of employees.
     */
    object Employees : AdminScreen()

    /**
     * Screen for creating a new user/employee.
     */
    object CreateUser : AdminScreen()

    /**
     * Detailed view of a specific user.
     * Likely opened after selecting a user from a list.
     */
    object UserDetails : AdminScreen()

    /**
     * Calendar overview (month/week view).
     */
    object Calendar : AdminScreen()

    /**
     * Detailed view of a single day in the calendar.
     */
    object CalendarDay : AdminScreen()

    /**
     * Detailed view of a specific task.
     */
    object TaskDetails : AdminScreen()

    /**
     * Overview of employee timesheets (admin perspective).
     */
    object TimesheetEmployees : AdminScreen()

    /**
     * Individual timesheet screen (likely per employee or admin).
     */
    object Timesheet : AdminScreen()
}