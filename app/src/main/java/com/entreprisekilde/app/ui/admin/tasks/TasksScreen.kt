package com.entreprisekilde.app.ui.admin.tasks

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Represents the kind of quick inline task update the admin performed.
 *
 * This is mainly used to:
 * - decide what confirmation message to show
 * - keep update handling clear and explicit
 */
private enum class TaskChangeType {
    DATE,
    ASSIGNEE,
    STATUS
}

/**
 * Small UI model used for the temporary confirmation banner shown at the bottom.
 *
 * We keep:
 * - the affected task ID
 * - the human-readable message
 * - the type of change that happened
 */
private data class TaskChangeConfirmation(
    val taskId: String,
    val message: String,
    val type: TaskChangeType
)

/**
 * Main admin screen for browsing and managing tasks.
 *
 * This screen supports:
 * - searching tasks
 * - loading / error / empty states
 * - quick inline task edits (status, date, assignee)
 * - deleting tasks
 * - opening full task details
 * - navigating to task creation
 *
 * Tasks are sorted so the most actionable items appear first:
 * 1. Pending
 * 2. In Progress
 * 3. Completed
 *
 * Within each status group, newer dates are shown first.
 */
@Composable
fun TasksScreen(
    tasks: List<TaskData>,
    assignedUserOptions: List<String> = emptyList(),
    unreadNotificationCount: Int = 0,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
    onCreateTaskClick: () -> Unit = {},
    onDeleteTask: (String) -> Unit = {},
    onTaskClick: (TaskData) -> Unit = {},
    onStatusChange: (String, TaskStatus) -> Unit = { _, _ -> },
    onQuickUpdateTask: (TaskData) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Search input used to filter visible tasks.
    var searchQuery by remember { mutableStateOf("") }

    // Holds the task that is currently waiting for delete confirmation.
    var taskPendingDelete by remember { mutableStateOf<TaskData?>(null) }

    // Controls the temporary "change saved" confirmation banner.
    var confirmation by remember { mutableStateOf<TaskChangeConfirmation?>(null) }

    /**
     * Automatically hide the confirmation banner after a short delay.
     *
     * This gives quick visual feedback without forcing the user to dismiss anything.
     */
    LaunchedEffect(confirmation) {
        if (confirmation != null) {
            delay(2200)
            confirmation = null
        }
    }

    // Search across the most useful fields so the admin can find tasks flexibly.
    val filteredTasks = tasks.filter { task ->
        searchQuery.isBlank() ||
                task.customer.contains(searchQuery, ignoreCase = true) ||
                task.address.contains(searchQuery, ignoreCase = true) ||
                task.assignTo.contains(searchQuery, ignoreCase = true) ||
                task.date.contains(searchQuery, ignoreCase = true) ||
                task.taskDetails.contains(searchQuery, ignoreCase = true) ||
                task.status.name.contains(searchQuery, ignoreCase = true) ||
                taskStatusLabel(task.status).contains(searchQuery, ignoreCase = true)
    }

    // Sort tasks by status priority, then by date descending inside each group.
    val sortedTasks = filteredTasks.sortedWith(
        compareBy<TaskData>(
            { statusSortOrder(it.status) },
            { -dateToSortableNumber(it.date) }
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0A673))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() },
                    tint = Color.Black
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "All Tasks",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Assignment,
                        contentDescription = "All Tasks",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Simple task search bar.
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Main content area switches between loading, error, empty, and list states.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading tasks...",
                                    color = Color(0xFF555555)
                                )
                            }
                        }

                        !errorMessage.isNullOrBlank() -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFFB3261E),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF79A7D8),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Retry")
                                }
                            }
                        }

                        sortedTasks.isEmpty() -> {
                            Text(
                                text = "No tasks found.",
                                color = Color(0xFF666666),
                                fontSize = 15.sp
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(sortedTasks, key = { it.id }) { task ->
                                    TaskCard(
                                        task = task,
                                        assignedUserOptions = assignedUserOptions,
                                        onClick = { onTaskClick(task) },
                                        onDeleteClick = { taskPendingDelete = task },
                                        onStatusChange = { newStatus ->
                                            onStatusChange(task.id, newStatus)

                                            confirmation = TaskChangeConfirmation(
                                                taskId = task.id,
                                                message = "Task status changed to ${taskStatusLabel(newStatus)}.",
                                                type = TaskChangeType.STATUS
                                            )
                                        },
                                        onQuickUpdateTask = { updatedTask, changeType ->
                                            onQuickUpdateTask(updatedTask)

                                            // Build a human-friendly banner message based on what changed.
                                            confirmation = when (changeType) {
                                                TaskChangeType.DATE -> TaskChangeConfirmation(
                                                    taskId = task.id,
                                                    message = "Task date changed to ${formatDateForDisplay(updatedTask.date)}.",
                                                    type = TaskChangeType.DATE
                                                )

                                                TaskChangeType.ASSIGNEE -> TaskChangeConfirmation(
                                                    taskId = task.id,
                                                    message = "Task assigned to ${updatedTask.assignTo}.",
                                                    type = TaskChangeType.ASSIGNEE
                                                )

                                                TaskChangeType.STATUS -> TaskChangeConfirmation(
                                                    taskId = task.id,
                                                    message = "Task status changed to ${taskStatusLabel(updatedTask.status)}.",
                                                    type = TaskChangeType.STATUS
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary action button for creating a new task.
                Button(
                    onClick = onCreateTaskClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF79A7D8),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Create a Task",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AppBottomNavBar(
                selectedItem = BottomNavDestination.HOME,
                unreadNotificationCount = unreadNotificationCount,
                onHomeClick = onHomeClick,
                onMessagesClick = onMessagesClick,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick
            )
        }

        // Floating confirmation banner shown above the bottom nav.
        confirmation?.let { currentConfirmation ->
            TaskChangeConfirmationBanner(
                message = currentConfirmation.message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
            )
        }
    }

    // Delete confirmation dialog.
    taskPendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text("Delete task?") },
            text = { Text("Are you sure you want to delete \"${task.customer}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTask(task.id)
                        taskPendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { taskPendingDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Reusable card showing a single task in the task list.
 *
 * This card supports quick admin actions directly from the list:
 * - change status
 * - change date
 * - change assignee
 * - delete task
 * - open full task details
 *
 * The idea is to make common admin workflows fast without forcing a full detail view every time.
 */
@Composable
private fun TaskCard(
    task: TaskData,
    assignedUserOptions: List<String>,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onStatusChange: (TaskStatus) -> Unit = {},
    onQuickUpdateTask: (TaskData, TaskChangeType) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Controls for the two dropdown menus inside the card.
    var statusExpanded by remember { mutableStateOf(false) }
    var assignExpanded by remember { mutableStateOf(false) }

    // Visual color of the status pill based on the current task state.
    val statusColor = when (task.status) {
        TaskStatus.PENDING -> Color(0xFFE5E7EB)
        TaskStatus.IN_PROGRESS -> Color(0xFFF2E2A8)
        TaskStatus.COMPLETED -> Color(0xFFC7EBC4)
    }

    // Parse the current task date so the date picker can open on the existing value.
    val currentTaskDate = parseTaskDate(task.date)

    val calendar = remember(task.id, task.date) {
        Calendar.getInstance().apply {
            val parsedDate = currentTaskDate
            if (parsedDate != null) {
                set(Calendar.YEAR, parsedDate.year)
                set(Calendar.MONTH, parsedDate.monthValue - 1)
                set(Calendar.DAY_OF_MONTH, parsedDate.dayOfMonth)
            }
        }
    }

    // Native Android date picker used for quick inline date changes.
    val datePickerDialog = remember(task.id, task.date) {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                val newDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                // Only trigger an update if the normalized value actually changed.
                // This avoids unnecessary writes caused by format differences.
                if (normalizeTaskDate(task.date) != normalizeTaskDate(newDate)) {
                    onQuickUpdateTask(task.copy(date = newDate), TaskChangeType.DATE)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.customer,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            // Status pill acts like a dropdown trigger.
            Box {
                Box(
                    modifier = Modifier
                        .background(statusColor, RoundedCornerShape(50))
                        .clickable { statusExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = taskStatusLabel(task.status),
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }

                DropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    listOf(
                        TaskStatus.PENDING,
                        TaskStatus.IN_PROGRESS,
                        TaskStatus.COMPLETED
                    ).forEach { value ->
                        DropdownMenuItem(
                            text = { Text(taskStatusLabel(value)) },
                            onClick = {
                                if (value != task.status) {
                                    onStatusChange(value)
                                }
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = task.address,
            fontSize = 14.sp,
            color = Color(0xFF444444)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Inline quick-edit row for date and assignee.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE9E9EB), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date section
            Row(
                modifier = Modifier.clickable { datePickerDialog.show() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Change date",
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = formatDateForDisplay(task.date),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = "Open date picker",
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Divider(
                modifier = Modifier
                    .height(18.dp)
                    .width(1.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Assignee section
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { assignExpanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "Change assigned user",
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = task.assignTo,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Open assigned user dropdown",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = assignExpanded,
                    onDismissRequest = { assignExpanded = false }
                ) {
                    assignedUserOptions.forEach { fullName ->
                        DropdownMenuItem(
                            text = { Text(fullName) },
                            onClick = {
                                if (fullName != task.assignTo) {
                                    onQuickUpdateTask(
                                        task.copy(assignTo = fullName),
                                        TaskChangeType.ASSIGNEE
                                    )
                                }
                                assignExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show a short preview of task details when available.
            if (task.taskDetails.isNotBlank()) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notes,
                        contentDescription = "Task details preview",
                        tint = Color(0xFF7A7A7A),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = task.taskDetails.trim(),
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = Color(0xFFB85C5C)
                )
            }
        }
    }
}

/**
 * Small success/info banner shown after quick inline changes.
 *
 * This is intentionally lightweight and temporary so it confirms the action
 * without interrupting the user's flow.
 */
@Composable
private fun TaskChangeConfirmationBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEDF7EE),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color(0xFFD7F0DA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Confirmation",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = message,
                color = Color(0xFF234F2A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Converts enum values into human-friendly labels for the UI.
 */
private fun taskStatusLabel(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "Pending"
        TaskStatus.IN_PROGRESS -> "In Progress"
        TaskStatus.COMPLETED -> "Completed"
    }
}

/**
 * Defines the sort priority for task statuses.
 *
 * Lower number = appears earlier in the list.
 */
private fun statusSortOrder(status: TaskStatus): Int {
    return when (status) {
        TaskStatus.PENDING -> 0
        TaskStatus.IN_PROGRESS -> 1
        TaskStatus.COMPLETED -> 2
    }
}

/**
 * Converts a task date into an integer like yyyyMMdd for easy sorting.
 *
 * Returns 0 when parsing fails, which effectively sends invalid/unknown dates
 * toward the end of date-based sorting.
 */
private fun dateToSortableNumber(date: String): Int {
    val parsedDate = parseTaskDate(date) ?: return 0
    return parsedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toIntOrNull() ?: 0
}

/**
 * Tries to parse a task date using the supported formats used across the app.
 *
 * Supported:
 * - dd/MM/yyyy
 * - yyyy-MM-dd
 *
 * Returns null if parsing fails for all supported formats.
 */
private fun parseTaskDate(date: String): LocalDate? {
    val supportedFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    for (formatter in supportedFormats) {
        try {
            return LocalDate.parse(date, formatter)
        } catch (_: Exception) {
            // Ignore parse failure and continue trying the next format.
        }
    }

    return null
}

/**
 * Normalizes a task date into a consistent dd/MM/yyyy string when possible.
 *
 * This helps avoid false "changes" caused only by different input formats.
 */
private fun normalizeTaskDate(date: String): String {
    val parsedDate = parseTaskDate(date) ?: return date
    return parsedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

/**
 * Formats task dates for display in the UI.
 *
 * Right now this simply normalizes the value, but keeping it as a separate
 * helper makes future display-format changes easier.
 */
private fun formatDateForDisplay(date: String): String {
    return normalizeTaskDate(date)
}