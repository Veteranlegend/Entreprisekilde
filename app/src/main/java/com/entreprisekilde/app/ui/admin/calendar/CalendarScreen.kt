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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Admin calendar screen.
 *
 * This screen shows a month-view calendar and highlights the dates that have tasks.
 * When a highlighted day is tapped, we pass the selected date back through [onDayClick].
 *
 * UI responsibilities:
 * - show current month/year
 * - allow moving between months
 * - visually highlight days that contain tasks
 * - provide bottom navigation + back navigation
 */
@Composable
fun CalendarScreen(
    tasks: List<TaskData>,
    unreadNotificationCount: Int = 0,
    onBack: () -> Unit = {},
    onDayClick: (String) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Keeps track of the month currently displayed in the calendar.
    // `remember` makes sure this survives recomposition while the screen is alive.
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Convert task date strings into LocalDate values so the calendar can compare them.
    // `toSet()` makes lookups fast and also removes duplicates automatically.
    val taskDates = tasks
        .mapNotNull { task ->
            parseTaskDate(task.date)
        }
        .toSet()

    // Basic month metadata used to build the calendar grid.
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = firstDayOffset(firstDayOfMonth.dayOfWeek)

    // Build a list of cells for the month grid.
    // We insert leading nulls so the first real day lands in the correct weekday column.
    val monthCells = buildList<LocalDate?> {
        repeat(startOffset) { add(null) }
        for (day in 1..daysInMonth) {
            add(currentMonth.atDay(day))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top header bar
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

        // Main calendar card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // Month navigation row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMonth.year.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    fontSize = 20.sp,
                    color = Color(0xFF4B5563)
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.clickable {
                        currentMonth = currentMonth.minusMonths(1)
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.clickable {
                        currentMonth = currentMonth.plusMonths(1)
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Weekday labels shown at the top of the calendar grid.
            // Order here is Sunday -> Saturday, so our offset logic must match that.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 13.sp,
                            color = Color(0xFF667085)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Split the month cells into week rows of 7 days each.
            monthCells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { date ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date == null) {
                                // Empty placeholder for the leading/trailing cells
                                // that belong to the previous/next month.
                                Box(modifier = Modifier.size(42.dp))
                            } else {
                                // A date is considered "active" if one or more tasks exist on that day.
                                val hasTasks = taskDates.contains(date)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    // Only allow clicking dates that actually have tasks.
                                    modifier = Modifier.clickable(enabled = hasTasks) {
                                        onDayClick(formatDateForTask(date))
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                color = if (hasTasks) Color(0xFFEAF2FF) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 17.sp,
                                            color = if (hasTasks) Color(0xFF2563EB) else Color(0xFF475467),
                                            fontWeight = if (hasTasks) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Small dot indicator below the day number.
                                    // We show it only when the date has tasks.
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                color = if (hasTasks) Color(0xFF2563EB) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // If the last week has fewer than 7 cells, fill the remaining space
                    // so the row layout stays aligned with the rest of the calendar.
                    if (week.size < 7) {
                        repeat(7 - week.size) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.size(42.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom navigation used throughout the app.
        AppBottomNavBar(
            selectedItem = BottomNavDestination.HOME,
            unreadNotificationCount = unreadNotificationCount,
            onHomeClick = onHomeClick,
            onMessagesClick = onMessagesClick,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )
    }
}

/**
 * Attempts to parse a task date string into [LocalDate].
 *
 * We support multiple formats because task data may come from different sources
 * or older parts of the app using different date formats.
 *
 * Supported:
 * - dd/MM/yyyy
 * - yyyy-MM-dd
 *
 * Returns null if none of the supported formats match.
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
            // Ignore parse failure and try the next supported format.
        }
    }

    return null
}

/**
 * Formats a [LocalDate] into the task date format expected elsewhere in the app.
 *
 * Right now we standardize outgoing day selections as dd/MM/yyyy.
 */
private fun formatDateForTask(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

/**
 * Converts Java's [DayOfWeek] into a calendar start offset where:
 * Sunday = 0, Monday = 1, ... Saturday = 6
 *
 * This is needed because the UI renders weekday headers starting with Sunday.
 */
private fun firstDayOffset(dayOfWeek: DayOfWeek): Int {
    return when (dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}