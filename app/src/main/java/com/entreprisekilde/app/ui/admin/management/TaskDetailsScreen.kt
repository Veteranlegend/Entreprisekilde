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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TaskDetailsScreen(
    task: TaskData,
    onBack: () -> Unit = {},
    onSaveEdit: (TaskData) -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }

    var customer by remember { mutableStateOf(task.customer) }
    var phoneNumber by remember { mutableStateOf(task.phoneNumber) }
    var address by remember { mutableStateOf(task.address) }
    var date by remember { mutableStateOf(task.date) }
    var assignTo by remember { mutableStateOf(task.assignTo) }
    var taskDetails by remember { mutableStateOf(task.taskDetails) }
    var fromTime by remember { mutableStateOf("09:30 AM") }
    var toTime by remember { mutableStateOf("12:30 PM") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(task) {
        customer = task.customer
        phoneNumber = task.phoneNumber
        address = task.address
        date = task.date
        assignTo = task.assignTo
        taskDetails = task.taskDetails
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

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFA8D6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        fontSize = 26.sp
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                Text(
                    text = assignTo,
                    fontSize = 16.sp,
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
                        imageVector = Icons.Outlined.GridView,
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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    DetailBlock("Customer", customer, isEditing, true) { customer = it }
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailBlock("Phone Number", phoneNumber, isEditing, true) { phoneNumber = it }
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailBlock("Address", address, isEditing, true) { address = it }
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailBlock("Date", prettyDate(date), false, true) {}
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailBlock("Task Details", taskDetails, isEditing, false) { taskDetails = it }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeField("From", fromTime, isEditing, { fromTime = it }, Modifier.weight(1f))
                    TimeField("To", toTime, isEditing, { toTime = it }, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Note",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = { Text("Add description.") },
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color.Transparent, RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = if (note.isBlank()) "Add description." else note,
                            color = Color(0xFF8A8A8A),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (isEditing) {
                            onSaveEdit(
                                task.copy(
                                    customer = customer,
                                    phoneNumber = phoneNumber,
                                    address = address,
                                    date = date,
                                    assignTo = assignTo,
                                    taskDetails = taskDetails
                                )
                            )
                        }
                        isEditing = !isEditing
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF55A8E3),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isEditing) "Save" else "Edit",
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
}

@Composable
private fun DetailBlock(
    title: String,
    value: String,
    editable: Boolean,
    singleLine: Boolean,
    onValueChange: (String) -> Unit
) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF3D3D3D)
    )

    Spacer(modifier = Modifier.height(4.dp))

    if (editable) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        )
    } else {
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color(0xFF3D3D3D)
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    editable: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = !editable,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF8A8A8A)
                )
            }
        )
    }
}

private fun prettyDate(date: String): String {
    return try {
        val parsed = LocalDate.parse(
            date,
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        )
        val dayName = parsed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val monthName = parsed.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
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