package com.entreprisekilde.app.ui.admin.tasks

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus

@Composable
fun TasksScreen(
    tasks: List<TaskData>,
    onBack: () -> Unit = {},
    onCreateTaskClick: () -> Unit = {},
    onDeleteTask: (String) -> Unit = {},
    onTaskClick: (TaskData) -> Unit = {},
    onStatusChange: (String, TaskStatus) -> Unit = { _, _ -> },
    onQuickUpdateTask: (TaskData) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var taskPendingDelete by remember { mutableStateOf<TaskData?>(null) }

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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tasks...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onDeleteClick = { taskPendingDelete = task },
                        onStatusChange = { newStatus ->
                            onStatusChange(task.id, newStatus)
                        },
                        onQuickUpdateTask = onQuickUpdateTask
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Home",
                icon = Icons.Outlined.Home,
                color = Color.Black,
                onClick = onHomeClick
            )
            BottomNavItem(
                label = "Message",
                icon = Icons.Outlined.ChatBubbleOutline,
                color = Color(0xFF9F98AA)
            )
            BottomNavItem(
                label = "Notification",
                icon = Icons.Outlined.Inventory2,
                color = Color(0xFF9F98AA)
            )
            BottomNavItem(
                label = "Profile",
                icon = Icons.Outlined.PersonOutline,
                color = Color(0xFF9F98AA),
                onClick = onProfileClick
            )
        }
    }

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

@Composable
private fun TaskCard(
    task: TaskData,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onStatusChange: (TaskStatus) -> Unit = {},
    onQuickUpdateTask: (TaskData) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDateDialog by remember { mutableStateOf(false) }
    var showEditAssignDialog by remember { mutableStateOf(false) }
    var editedDate by remember(task.id, task.date) { mutableStateOf(task.date) }
    var editedAssignTo by remember(task.id, task.assignTo) { mutableStateOf(task.assignTo) }

    val statusColor = when (task.status) {
        TaskStatus.PENDING -> Color(0xFFE5E7EB)
        TaskStatus.IN_PROGRESS -> Color(0xFFF2E2A8)
        TaskStatus.COMPLETED -> Color(0xFFC7EBC4)
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

            Box {
                Box(
                    modifier = Modifier
                        .background(statusColor, RoundedCornerShape(50))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = taskStatusLabel(task.status),
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(
                        TaskStatus.PENDING,
                        TaskStatus.IN_PROGRESS,
                        TaskStatus.COMPLETED
                    ).forEach { value ->
                        DropdownMenuItem(
                            text = { Text(taskStatusLabel(value)) },
                            onClick = {
                                onStatusChange(value)
                                expanded = false
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE9E9EB), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { showEditDateDialog = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = task.date,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Divider(
                modifier = Modifier
                    .height(18.dp)
                    .width(1.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                modifier = Modifier.clickable { showEditAssignDialog = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = task.assignTo,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onDeleteClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE58C8C),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Delete",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showEditDateDialog) {
        AlertDialog(
            onDismissRequest = { showEditDateDialog = false },
            title = { Text("Edit date") },
            text = {
                OutlinedTextField(
                    value = editedDate,
                    onValueChange = { editedDate = it },
                    label = { Text("Date") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onQuickUpdateTask(task.copy(date = editedDate))
                        showEditDateDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editedDate = task.date
                        showEditDateDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditAssignDialog) {
        AlertDialog(
            onDismissRequest = { showEditAssignDialog = false },
            title = { Text("Edit assigned person") },
            text = {
                OutlinedTextField(
                    value = editedAssignTo,
                    onValueChange = { editedAssignTo = it },
                    label = { Text("Assigned to") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onQuickUpdateTask(task.copy(assignTo = editedAssignTo))
                        showEditAssignDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editedAssignTo = task.assignTo
                        showEditAssignDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
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

private fun taskStatusLabel(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "Pending"
        TaskStatus.IN_PROGRESS -> "In Progress"
        TaskStatus.COMPLETED -> "Completed"
    }
}