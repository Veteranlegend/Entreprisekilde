package com.entreprisekilde.app.ui.admin.tasks

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    var customer by remember(task.id) { mutableStateOf(task.customer) }
    var phoneNumber by remember(task.id) { mutableStateOf(task.phoneNumber) }
    var address by remember(task.id) { mutableStateOf(task.address) }
    var date by remember(task.id) { mutableStateOf(normalizeDateForDisplay(task.date)) }
    var assignTo by remember(task.id) { mutableStateOf(task.assignTo) }
    var taskDetails by remember(task.id) { mutableStateOf(task.taskDetails) }
    var assignToExpanded by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }
    var showMapsErrorDialog by remember { mutableStateOf(false) }

    val initialDate = parseToLocalDate(date) ?: LocalDate.now()

    val datePickerDialog = remember(initialDate) {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                date = selectedDate.format(displayFormatter)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).apply {
            try {
                datePicker.calendarViewShown = true
                datePicker.spinnersShown = false
            } catch (_: Exception) {
            }
        }
    }

    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = {
                showSavedDialog = false
            },
            title = {
                Text("Changes saved")
            },
            text = {
                Text("The task details were updated successfully.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSavedDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showMapsErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showMapsErrorDialog = false
            },
            title = {
                Text("Unable to open maps")
            },
            text = {
                Text("No maps application was found on this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMapsErrorDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
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
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Task Details",
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
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Task details",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallField(
                label = "Customer",
                value = customer,
                onValueChange = { customer = it }
            )

            SmallField(
                label = "Phone Number",
                value = phoneNumber,
                onValueChange = { phoneNumber = it }
            )

            SmallField(
                label = "Address",
                value = address,
                onValueChange = { address = it }
            )

            Button(
                onClick = {
                    val cleanAddress = address.trim()

                    if (cleanAddress.isBlank()) return@Button

                    val encodedAddress = Uri.encode(cleanAddress)

                    val mapsIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=$encodedAddress")
                    )

                    try {
                        context.startActivity(mapsIntent)
                    } catch (_: ActivityNotFoundException) {
                        try {
                            val browserFallbackIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
                            )
                            context.startActivity(browserFallbackIntent)
                        } catch (_: Exception) {
                            showMapsErrorDialog = true
                        }
                    }

                },
                enabled = address.trim().isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE6EEF8),
                    contentColor = Color(0xFF214C7A),
                    disabledContainerColor = Color(0xFFEAEAEA),
                    disabledContentColor = Color(0xFF9E9E9E)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Open in Maps",
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Open in Maps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            DateSelectorField(
                label = "Date",
                value = date,
                onClick = { datePickerDialog.show() }
            )

            AssignToSelectorField(
                label = "Assign To",
                value = assignTo,
                options = assignedUserOptions,
                expanded = assignToExpanded,
                onExpand = { assignToExpanded = true },
                onDismiss = { assignToExpanded = false },
                onSelect = { selectedUser ->
                    assignTo = selectedUser
                    assignToExpanded = false
                }
            )

            SmallField(
                label = "Task Details",
                value = taskDetails,
                onValueChange = { taskDetails = it },
                singleLine = false,
                minLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSaveEdit(
                        task.copy(
                            customer = customer.trim(),
                            phoneNumber = phoneNumber.trim(),
                            address = address.trim(),
                            date = normalizeDateForDisplay(date),
                            assignTo = assignTo.trim(),
                            taskDetails = taskDetails.trim()
                        )
                    )
                    showSavedDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCFE0D0),
                    contentColor = Color(0xFF3F6E48)
                )
            ) {
                Text(
                    text = "Save",
                    fontSize = 18.sp,
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
}

@Composable
private fun SmallField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = Color(0xFF444444),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF9AA0A6),
                unfocusedBorderColor = Color(0xFF9AA0A6),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
    }
}

@Composable
private fun DateSelectorField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF444444),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Select date",
                        tint = Color(0xFF666666)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF9AA0A6),
                    unfocusedBorderColor = Color(0xFF9AA0A6),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
            )
        }
    }
}

@Composable
private fun AssignToSelectorField(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF444444),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Open employee list",
                        tint = Color(0xFF666666)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF9AA0A6),
                    unfocusedBorderColor = Color(0xFF9AA0A6),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onExpand() }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss
            ) {
                options.forEach { userName ->
                    DropdownMenuItem(
                        text = { Text(userName) },
                        onClick = { onSelect(userName) }
                    )
                }
            }
        }
    }
}

private fun parseToLocalDate(date: String): LocalDate? {
    val inputFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    for (formatter in inputFormats) {
        try {
            return LocalDate.parse(date, formatter)
        } catch (_: Exception) {
        }
    }

    return null
}

private fun normalizeDateForDisplay(date: String): String {
    val parsed = parseToLocalDate(date) ?: return date
    return parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}