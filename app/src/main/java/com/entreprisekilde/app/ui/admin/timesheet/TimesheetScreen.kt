package com.entreprisekilde.app.ui.admin.timesheet
import com.entreprisekilde.app.data.model.timesheet.TimesheetEntry
import com.entreprisekilde.app.data.model.timesheet.ShiftApprovalStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TimesheetScreen(
    employeeName: String,
    timesheets: List<TimesheetEntry>,
    unreadNotificationCount: Int = 0,
    onBack: () -> Unit = {},
    onApproveEntry: (String) -> Unit = {},
    onDeclineEntry: (String) -> Unit = {},
    onDeleteEntry: (String) -> Unit = {},
    onAssignShift: (TimesheetEntry) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var showAssignDialog by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredEntries = timesheets.filter {
        searchText.isBlank() ||
                it.date.contains(searchText, ignoreCase = true) ||
                it.fromTime.contains(searchText, ignoreCase = true) ||
                it.toTime.contains(searchText, ignoreCase = true) ||
                it.approvalStatus.name.contains(searchText, ignoreCase = true)
    }

    val sortedEntries = filteredEntries.sortedByDescending { parseTimesheetDate(it.date) }

    val pendingEntries = sortedEntries.filter {
        classifyShift(it.date) != "future" && it.approvalStatus == ShiftApprovalStatus.PENDING
    }

    val upcomingEntries = sortedEntries.filter {
        classifyShift(it.date) == "future"
    }

    val approvedEntries = sortedEntries.filter {
        it.approvalStatus == ShiftApprovalStatus.APPROVED
    }

    val declinedEntries = sortedEntries.filter {
        classifyShift(it.date) != "future" && it.approvalStatus == ShiftApprovalStatus.DECLINED
    }

    val approvedMinutesTotal = approvedEntries.sumOf {
        durationInMinutes(it.fromTime, it.toTime)
    }

    val assignedMinutesTotal = sortedEntries
        .filter { it.approvalStatus != ShiftApprovalStatus.DECLINED }
        .sumOf { durationInMinutes(it.fromTime, it.toTime) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
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
                text = employeeName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ModeEdit,
                    contentDescription = null,
                    tint = Color(0xFF666666)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Timesheet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                },
                placeholder = { Text("Search") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${sortedEntries.size} Results",
                    fontSize = 15.sp,
                    color = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Assigned:",
                            fontSize = 14.sp,
                            color = Color(0xFF7A7A7A)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = minutesToDurationString(assignedMinutesTotal),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Approved:",
                            fontSize = 14.sp,
                            color = Color(0xFF7A7A7A)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = minutesToDurationString(approvedMinutesTotal),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (pendingEntries.isNotEmpty()) {
                    item { SectionTitle("Pending review") }

                    items(pendingEntries, key = { it.id }) { entry ->
                        TimesheetCard(
                            entry = entry,
                            showActions = true,
                            showDelete = true,
                            onApprove = { onApproveEntry(entry.id) },
                            onDecline = { onDeclineEntry(entry.id) },
                            onDelete = {
                                deleteTargetId = entry.id
                                showDeleteDialog = true
                            }
                        )
                    }
                }

                if (upcomingEntries.isNotEmpty()) {
                    item { SectionTitle("Upcoming shifts") }

                    items(upcomingEntries, key = { it.id }) { entry ->
                        TimesheetCard(
                            entry = entry,
                            showActions = false,
                            showDelete = true,
                            onDelete = {
                                deleteTargetId = entry.id
                                showDeleteDialog = true
                            }
                        )
                    }
                }

                if (approvedEntries.isNotEmpty()) {
                    item { SectionTitle("Approved") }

                    items(approvedEntries, key = { it.id }) { entry ->
                        TimesheetCard(
                            entry = entry,
                            showActions = false,
                            showDelete = false
                        )
                    }
                }

                if (declinedEntries.isNotEmpty()) {
                    item { SectionTitle("Declined") }

                    items(declinedEntries, key = { it.id }) { entry ->
                        TimesheetCard(
                            entry = entry,
                            showActions = false,
                            showDelete = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { showAssignDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF55A8E3),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Assign Shift",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
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

    if (showAssignDialog) {
        AssignShiftDialog(
            employeeName = employeeName,
            onDismiss = { showAssignDialog = false },
            onAssign = { newEntry ->
                onAssignShift(newEntry)
                showAssignDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteTargetId = null
            },
            title = {
                Text(
                    text = "Delete shift?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete this timesheet entry?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTargetId?.let { onDeleteEntry(it) }
                        showDeleteDialog = false
                        deleteTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE58C8C),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteTargetId = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF3A3A3A),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun TimesheetCard(
    entry: TimesheetEntry,
    showActions: Boolean,
    showDelete: Boolean,
    onApprove: () -> Unit = {},
    onDecline: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val shiftType = classifyShift(entry.date)

    val topLabel = when {
        shiftType == "future" -> prettyTimesheetDate(entry.date)
        shiftType == "today" -> "Today"
        else -> prettyTimesheetDate(entry.date)
    }

    val borderColor = when {
        shiftType == "future" -> Color(0xFF8BB8F2)
        entry.approvalStatus == ShiftApprovalStatus.APPROVED -> Color(0xFF88C999)
        entry.approvalStatus == ShiftApprovalStatus.PENDING -> Color(0xFFE0C57A)
        else -> Color(0xFFE59A9A)
    }

    val statusChipColor = when {
        shiftType == "future" -> Color(0xFFD7E9FF)
        entry.approvalStatus == ShiftApprovalStatus.APPROVED -> Color(0xFFDDF5E2)
        entry.approvalStatus == ShiftApprovalStatus.PENDING -> Color(0xFFF8EDC3)
        else -> Color(0xFFF7D7D7)
    }

    val statusText = when {
        shiftType == "future" -> "Upcoming"
        entry.approvalStatus == ShiftApprovalStatus.APPROVED -> "Approved"
        entry.approvalStatus == ShiftApprovalStatus.PENDING -> "Pending"
        else -> "Declined"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = topLabel,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete shift",
                        tint = Color(0xFFB55B5B)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = calculateDuration(entry.fromTime, entry.toTime),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.fromTime,
                fontSize = 15.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFE0E0E0))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = entry.toTime,
                fontSize = 15.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .background(statusChipColor, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
        }

        if (showActions) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onDecline,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE58C8C),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Decline")
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFAED7B3),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
private fun AssignShiftDialog(
    employeeName: String,
    onDismiss: () -> Unit,
    onAssign: (TimesheetEntry) -> Unit
) {
    var date by remember {
        mutableStateOf(
            LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        )
    }
    var fromTime by remember { mutableStateOf("09:30 AM") }
    var toTime by remember { mutableStateOf("06:30 PM") }
    var assignedHours by remember { mutableStateOf("9") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val hours = assignedHours.toIntOrNull() ?: 0
                    onAssign(
                        TimesheetEntry(
                            date = date,
                            fromTime = fromTime,
                            toTime = toTime,
                            employeeName = employeeName,
                            submittedHours = 0,
                            assignedHours = hours,
                            approvalStatus = ShiftApprovalStatus.PENDING
                        )
                    )
                }
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Assign Shift") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (dd/MM/yyyy)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = fromTime,
                    onValueChange = { fromTime = it },
                    label = { Text("From") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = toTime,
                    onValueChange = { toTime = it },
                    label = { Text("To") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = assignedHours,
                    onValueChange = { assignedHours = it },
                    label = { Text("Assigned hours") },
                    singleLine = true
                )
            }
        }
    )
}

private fun classifyShift(date: String): String {
    return try {
        val parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val today = LocalDate.now()
        when {
            parsedDate.isAfter(today) -> "future"
            parsedDate.isEqual(today) -> "today"
            else -> "past"
        }
    } catch (_: Exception) {
        "past"
    }
}

private fun parseTimesheetDate(date: String): LocalDate {
    return try {
        LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: Exception) {
        LocalDate.MIN
    }
}

private fun prettyTimesheetDate(date: String): String {
    return try {
        val parsed = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        if (parsed.isEqual(LocalDate.now())) {
            "Today"
        } else {
            parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (_: Exception) {
        date
    }
}

private fun calculateDuration(from: String, to: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val start = LocalTime.parse(from, formatter)
        val end = LocalTime.parse(to, formatter)
        val duration = Duration.between(start, end)

        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        String.format("%02d:%02d", hours, minutes)
    } catch (_: Exception) {
        "00:00"
    }
}

private fun durationInMinutes(from: String, to: String): Int {
    return try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val start = LocalTime.parse(from, formatter)
        val end = LocalTime.parse(to, formatter)
        Duration.between(start, end).toMinutes().toInt()
    } catch (_: Exception) {
        0
    }
}

private fun minutesToDurationString(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%02d:%02d", hours, minutes)
}