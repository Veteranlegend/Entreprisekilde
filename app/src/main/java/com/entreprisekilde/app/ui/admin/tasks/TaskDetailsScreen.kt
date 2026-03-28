package com.entreprisekilde.app.ui.admin.tasks

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

@Composable
fun TaskDetailsScreen(
    task: TaskData,
    onBack: () -> Unit = {},
    onSaveEdit: (TaskData) -> Unit = {},
    assignedUserOptions: List<String> = emptyList(),
    unreadNotificationCount: Int = 0,
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var customer by remember(task.id) { mutableStateOf(task.customer) }
    var phoneNumber by remember(task.id) { mutableStateOf(task.phoneNumber) }
    var address by remember(task.id) { mutableStateOf(task.address) }
    var date by remember(task.id) { mutableStateOf(task.date) }
    var taskDetails by remember(task.id) { mutableStateOf(task.taskDetails) }
    var assignTo by remember(task.id) { mutableStateOf(task.assignTo) }
    var status by remember(task.id) { mutableStateOf(task.status) }

    var assignedExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )

            Spacer(modifier = Modifier.size(12.dp))

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color(0xFF8EC5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.take(1).uppercase(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Text(
                text = customer.ifBlank { "Task Details" },
                fontSize = 22.sp,
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
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F1F1), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EditableField(
                        label = "Customer",
                        value = customer,
                        onValueChange = { customer = it }
                    )

                    EditableField(
                        label = "Phone Number",
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it }
                    )

                    EditableField(
                        label = "Address",
                        value = address,
                        onValueChange = { address = it }
                    )

                    EditableField(
                        label = "Date",
                        value = date,
                        onValueChange = { date = it }
                    )

                    EditableField(
                        label = "Task Details",
                        value = taskDetails,
                        onValueChange = { taskDetails = it },
                        singleLine = false,
                        minLines = 4
                    )

                    Column {
                        Text(
                            text = "Assigned to",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444444)
                        )

                        Spacer(modifier = Modifier.size(6.dp))

                        if (assignedUserOptions.isNotEmpty()) {
                            Box {
                                DropdownField(
                                    value = assignTo,
                                    onClick = { assignedExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = assignedExpanded,
                                    onDismissRequest = { assignedExpanded = false }
                                ) {
                                    assignedUserOptions.forEach { userName ->
                                        DropdownMenuItem(
                                            text = { Text(userName) },
                                            onClick = {
                                                assignTo = userName
                                                assignedExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            EditableField(
                                label = "",
                                value = assignTo,
                                onValueChange = { assignTo = it },
                                showLabel = false
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444444)
                        )

                        Spacer(modifier = Modifier.size(6.dp))

                        Box {
                            DropdownField(
                                value = taskStatusLabel(status),
                                onClick = { statusExpanded = true }
                            )

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
                                            status = value
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Button(
                        onClick = {
                            onSaveEdit(
                                task.copy(
                                    customer = customer,
                                    phoneNumber = phoneNumber,
                                    address = address,
                                    date = date,
                                    taskDetails = taskDetails,
                                    assignTo = assignTo,
                                    status = status
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF79A7D8),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Save Changes",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.navigationBarsPadding()) {
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
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1,
    showLabel: Boolean = true
) {
    Column {
        if (showLabel && label.isNotBlank()) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF444444)
            )

            Spacer(modifier = Modifier.size(6.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}

@Composable
private fun DropdownField(
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            color = Color.Black,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Outlined.ArrowDropDown,
            contentDescription = "Open dropdown",
            tint = Color(0xFF666666)
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