package com.entreprisekilde.app.ui.admin.calendar

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Screen that shows all tasks for one selected calendar day.
 *
 * Main responsibilities:
 * - display the chosen date in a readable format
 * - let the user search/filter tasks for that day
 * - show the filtered results as clickable cards
 * - keep the app's bottom navigation visible
 *
 * This screen assumes [tasksForDay] is already pre-filtered by date by the caller.
 * The local search here is only for searching within that one day's task list.
 */
@Composable
fun CalendarDayScreen(
    selectedDate: String,
    tasksForDay: List<TaskData>,
    onBack: () -> Unit = {},
    onTaskClick: (TaskData) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Local search text entered by the user.
    var searchQuery by remember { mutableStateOf("") }

    /**
     * Filter tasks in-memory based on the current search query.
     *
     * We search across several human-meaningful fields so the search feels useful:
     * - customer
     * - address
     * - assignee
     * - task details
     * - raw enum name
     * - display label for the task status
     *
     * That last part matters because users may type "In Progress" while the enum
     * value is actually IN_PROGRESS.
     */
    val filteredTasks = tasksForDay.filter { task ->
        searchQuery.isBlank() ||
                task.customer.contains(searchQuery, ignoreCase = true) ||
                task.address.contains(searchQuery, ignoreCase = true) ||
                task.assignTo.contains(searchQuery, ignoreCase = true) ||
                task.taskDetails.contains(searchQuery, ignoreCase = true) ||
                task.status.name.contains(searchQuery, ignoreCase = true) ||
                taskStatusLabel(task.status).contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))

            // Respect safe drawing insets so content does not collide with
            // status bars, cutouts, or navigation areas.
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        /**
         * Top bar / header section.
         *
         * This is custom instead of using a standard TopAppBar because the design
         * clearly wants a more branded layout.
         */
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
                text = "Calendar",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            // Decorative calendar icon on the right side of the header.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            /**
             * Shows the chosen date in a more human-friendly format,
             * e.g. "Monday 14 March 2026" instead of "14/03/2026".
             */
            Text(
                text = prettyDate(selectedDate),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Spacer(modifier = Modifier.height(14.dp))

            /**
             * Search field for narrowing down tasks within the selected day.
             */
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                },
                placeholder = {
                    Text("Search tasks")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            /**
             * Simple result count so the user gets immediate feedback while typing.
             */
            Text(
                text = "${filteredTasks.size} Results",
                fontSize = 15.sp,
                color = Color(0xFF1F2937)
            )

            Spacer(modifier = Modifier.height(12.dp))

            /**
             * Scrollable list of matching tasks.
             *
             * Using LazyColumn here is the right choice because it scales better
             * than a regular Column when the task list grows.
             */
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(filteredTasks) { _, task ->
                    CalendarTaskCard(
                        task = task,
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }

        /**
         * Bottom navigation remains visible on this screen so users can quickly
         * jump to other main sections of the app.
         *
         * HOME is selected here because this calendar screen appears to live
         * under the home/admin flow.
         */
        AppBottomNavBar(
            selectedItem = BottomNavDestination.HOME,
            onHomeClick = onHomeClick,
            onMessagesClick = onMessagesClick,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )
    }
}

/**
 * Small reusable card used to render one task inside the day view list.
 *
 * Visual behavior:
 * - completed tasks use a green theme
 * - non-completed tasks use an orange theme
 *
 * That color split gives users a quick visual status cue without needing to read
 * every label.
 */
@Composable
private fun CalendarTaskCard(
    task: TaskData,
    onClick: () -> Unit = {}
) {
    val backgroundColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF0F8A70)
        else -> Color(0xFFFF6B00)
    }

    val chipColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFFD7F4C8)
        else -> Color(0xFFFCE9B3)
    }

    val chipTextColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF16925F)
        else -> Color(0xFFB36B00)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Small white dot for a little extra visual polish / structure.
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.White, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = task.customer,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            /**
             * Status chip on the right side.
             *
             * This uses a softer color than the card background so the status
             * stays readable and visually separated.
             */
            Box(
                modifier = Modifier
                    .background(chipColor, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = taskStatusLabel(task.status),
                    color = chipTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        /**
         * Secondary metadata row:
         * - task date
         * - task address
         */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = task.date,
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = task.address,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Converts a [TaskStatus] enum into a user-friendly label for the UI.
 *
 * This keeps display text centralized instead of scattering hardcoded labels
 * across the screen.
 */
private fun taskStatusLabel(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "Pending"
        TaskStatus.IN_PROGRESS -> "In Progress"
        TaskStatus.COMPLETED -> "Completed"
    }
}

/**
 * Converts a raw date string into a friendlier display format.
 *
 * Supported input formats:
 * - dd/MM/yyyy
 * - yyyy-MM-dd
 *
 * Example output:
 * - "Friday 14 March 2026"
 *
 * If parsing fails, we simply return the original value rather than crashing or
 * showing a broken UI. That makes this helper very safe to use with mixed data.
 */
private fun prettyDate(date: String): String {
    val supportedFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    for (formatter in supportedFormats) {
        try {
            val parsed = LocalDate.parse(date, formatter)
            val dayName = parsed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val monthName = parsed.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            return "$dayName ${parsed.dayOfMonth} $monthName ${parsed.year}"
        } catch (_: Exception) {
            // Try the next supported format.
        }
    }

    // Fall back to the raw input if none of the known formats match.
    return date
}