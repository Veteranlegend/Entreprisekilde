package com.entreprisekilde.app.ui.admin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalendarDayScreen(
    selectedDate: String,
    tasksForDay: List<TaskData>,
    onBack: () -> Unit = {},
    onTaskClick: (TaskData) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredTasks = tasksForDay.filter {
        searchQuery.isBlank() ||
                it.customer.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true) ||
                it.assignTo.contains(searchQuery, ignoreCase = true) ||
                it.taskDetails.contains(searchQuery, ignoreCase = true) ||
                it.status.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6DADA))
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "9:41 AM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "▮▮▮  ◠  ▱",
                    fontSize = 13.sp,
                    color = Color.Black
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0A673))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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

                Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                Text(
                    text = "Calendar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text(
                    text = prettyDate(selectedDate),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.padding(vertical = 10.dp))

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
                        Text("Search")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = "${filteredTasks.size} Results",
                    fontSize = 16.sp,
                    color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(filteredTasks) { _, task ->
                        CalendarTaskCard(
                            task = task,
                            onClick = { onTaskClick(task) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem("Home", Icons.Outlined.Home, Color.Black)
                BottomNavItem("Message", Icons.Outlined.ChatBubbleOutline, Color(0xFF9F98AA))
                BottomNavItem("Notification", Icons.Outlined.Inventory2, Color(0xFF9F98AA))
                BottomNavItem("Profile", Icons.Outlined.PersonOutline, Color(0xFF9F98AA))
            }
        }
    }
}

@Composable
private fun CalendarTaskCard(
    task: TaskData,
    onClick: () -> Unit = {}
) {
    val backgroundColor = when (task.status) {
        "Complete" -> Color(0xFF0F8A70)
        else -> Color(0xFFFF6B00)
    }

    val chipColor = when (task.status) {
        "Complete" -> Color(0xFFD7F4C8)
        else -> Color(0xFFFCE9B3)
    }

    val chipTextColor = when (task.status) {
        "Complete" -> Color(0xFF16925F)
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
            Text(
                text = "◌",
                color = Color.White,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.padding(horizontal = 5.dp))

            Text(
                text = task.customer,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .background(chipColor, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = task.status,
                    color = chipTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.padding(vertical = 5.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.padding(horizontal = 3.dp))

            Text(
                text = "16:30",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.padding(horizontal = 10.dp))

            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.padding(horizontal = 3.dp))

            Text(
                text = task.address,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

private fun prettyDate(date: String): String {
    return try {
        val parsed = java.time.LocalDate.parse(
            date,
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        )
        val dayName = parsed.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.ENGLISH
        )
        val monthName = parsed.month.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.ENGLISH
        )
        "$dayName ${parsed.dayOfMonth} $monthName ${parsed.year}"
    } catch (_: Exception) {
        date
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = color
        )
    }
}