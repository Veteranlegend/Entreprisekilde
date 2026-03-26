package com.entreprisekilde.app.ui.admin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ShiftApprovalStatus {
    Pending,
    Approved,
    Declined
}

data class TimesheetEntry(
    val date: String,
    val fromTime: String,
    val toTime: String,
    val employeeName: String,
    val submittedHours: Int,
    val assignedHours: Int,
    val approvalStatus: ShiftApprovalStatus
)

@Composable
fun TimesheetScreen(
    employeeName: String,
    timesheets: List<TimesheetEntry>,
    onBack: () -> Unit = {},
    onApproveEntry: (Int) -> Unit = {},
    onDeclineEntry: (Int) -> Unit = {},
    onDeleteEntry: (Int) -> Unit = {},
    onAssignShift: (TimesheetEntry) -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var showAssignDialog by remember { mutableStateOf(false) }

    val filteredEntries = timesheets.filter {
        searchText.isBlank() ||
                it.date.contains(searchText, ignoreCase = true) ||
                it.fromTime.contains(searchText, ignoreCase = true) ||
                it.toTime.contains(searchText, ignoreCase = true) ||
                it.approvalStatus.name.contains(searchText, ignoreCase = true)
    }

    val totalSubmittedHours = filteredEntries.sumOf { it.submittedHours }
    val totalAssignedHours = filteredEntries.sumOf { it.assignedHours }

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

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = employeeName,
                    fontSize = 20.sp,
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
                    fontSize = 19.sp,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF4F4F4),
                        unfocusedContainerColor = Color(0xFFF4F4F4),
                        disabledContainerColor = Color(0xFFF4F4F4)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredEntries.size} Results",
                        fontSize = 16.sp,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Submitted:",
                        fontSize = 16.sp,
                        color = Color(0xFF7A7A7A)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${totalSubmittedHours} hrs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Assigned:",
                        fontSize = 16.sp,
                        color = Color(0xFF7A7A7A)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${totalAssignedHours} hrs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(filteredEntries) { _, entry ->
                        val originalIndex = timesheets.indexOf(entry)
                        TimesheetCard(
                            entry = entry,
                            onApprove = {
                                if (originalIndex != -1) onApproveEntry(originalIndex)
                            },
                            onDecline = {
                                if (originalIndex != -1) onDeclineEntry(originalIndex)
                            },
                            onDelete = {
                                if (originalIndex != -1) onDeleteEntry(originalIndex)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showAssignDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
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
}

@Composable
private fun TimesheetCard(
    entry: TimesheetEntry,
    onApprove: () -> Unit = {},
    onDecline: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val shiftType = classifyShift(entry.date)

    val topLabel = when (shiftType) {
        "future" -> "Upcoming"
        "today" -> "Today"
        else -> prettyTimesheetDate(entry.date)
    }

    val borderColor = when (shiftType) {
        "future" -> Color(0xFF8BB8F2)
        "today" -> Color(0xFFF0B366)
        else -> when (entry.approvalStatus) {
            ShiftApprovalStatus.Approved -> Color(0xFF88C999)
            ShiftApprovalStatus.Pending -> Color(0xFFE0C57A)
            ShiftApprovalStatus.Declined -> Color(0xFFE59A9A)
        }
    }

    val statusChipColor = when (shiftType) {
        "future" -> Color(0xFFD7E9FF)
        "today" -> Color(0xFFFFE3BF)
        else -> when (entry.approvalStatus) {
            ShiftApprovalStatus.Approved -> Color(0xFFDDF5E2)
            ShiftApprovalStatus.Pending -> Color(0xFFF8EDC3)
            ShiftApprovalStatus.Declined -> Color(0xFFF7D7D7)
        }
    }

    val statusText = when (shiftType) {
        "future" -> "Future"
        "today" -> if (entry.approvalStatus == ShiftApprovalStatus.Pending) "Ongoing" else entry.approvalStatus.name
        else -> entry.approvalStatus.name
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

            Text(
                text = "Submitted:",
                fontSize = 14.sp,
                color = Color(0xFF7A7A7A)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "${entry.submittedHours} hrs",
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onDecline,
                enabled = shiftType != "future",
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE58C8C),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFF1D1D1),
                    disabledContentColor = Color(0xFF8A8A8A)
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
                enabled = shiftType != "future",
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFAED7B3),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFDCEEDC),
                    disabledContentColor = Color(0xFF8A8A8A)
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

@Composable
private fun AssignShiftDialog(
    employeeName: String,
    onDismiss: () -> Unit,
    onAssign: (TimesheetEntry) -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
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
                            approvalStatus = ShiftApprovalStatus.Pending
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