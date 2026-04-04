package com.entreprisekilde.app.ui.admin.tasks

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskStatus
import com.entreprisekilde.app.data.model.users.User
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Screen for creating a new task.
 *
 * This screen handles:
 * - collecting task input from the admin
 * - validating required fields
 * - selecting a date
 * - selecting an assigned employee
 * - selecting one or more images
 * - creating the final [TaskData] object
 *
 * The actual save/upload logic is not done here directly.
 * Instead, we pass the created task + selected image URIs back through [onCreateTask].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTaskScreen(
    unreadNotificationCount: Int = 0,
    onBack: () -> Unit = {},
    onCreateTask: (TaskData, List<Uri>) -> Unit = { _, _ -> },
    assignedUserOptions: List<User> = emptyList(),
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Form state
    var customer by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var assignTo by remember { mutableStateOf("") }
    var assignedUserId by remember { mutableStateOf("") }
    var taskDetails by remember { mutableStateOf("") }

    // UI state
    var assignToExpanded by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Holds the selected local image URIs before they are uploaded/saved.
    val selectedImageUris = remember { mutableStateListOf<Uri>() }

    // Validation error messages for each field.
    // Null means "no error".
    var customerError by remember { mutableStateOf<String?>(null) }
    var phoneNumberError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var assignToError by remember { mutableStateOf<String?>(null) }
    var taskDetailsError by remember { mutableStateOf<String?>(null) }

    // Small style constants used across the screen.
    val headerColor = Color(0xFFE0A673)
    val buttonColor = Color(0xFFCFE0D0)
    val buttonTextColor = Color(0xFF3F6E48)

    val calendar = remember { Calendar.getInstance() }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    /**
     * Android photo picker launcher.
     *
     * We allow multiple image selection.
     * Current behavior: when the user picks new images, we replace the old selection
     * rather than append to it.
     */
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)
        }
    }

    /**
     * Native Android date picker dialog.
     *
     * When a date is selected:
     * - we format it as dd/MM/yyyy
     * - store it in the form
     * - clear any existing date validation error
     */
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                date = selectedDate.format(displayFormatter)
                dateError = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            try {
                // Prefer calendar view UI when supported by the device/theme.
                datePicker.calendarViewShown = true
                datePicker.spinnersShown = false
            } catch (_: Exception) {
                // Some devices / theme combinations may not support these settings.
                // If that happens, we silently keep the default picker behavior.
            }
        }
    }

    /**
     * Validates all form fields and sets field-specific error messages.
     *
     * Returns true only when the entire form is valid.
     */
    fun validateFields(): Boolean {
        val digitsOnlyPhoneNumber = phoneNumber.filter { it.isDigit() }

        customerError = if (customer.trim().isBlank()) {
            "Customer is required"
        } else {
            null
        }

        phoneNumberError = when {
            digitsOnlyPhoneNumber.isBlank() -> "Phone number is required"
            digitsOnlyPhoneNumber.length < 8 -> "Phone number must be at least 8 digits"
            else -> null
        }

        addressError = if (address.trim().isBlank()) {
            "Address is required"
        } else {
            null
        }

        dateError = if (date.trim().isBlank()) {
            "Date is required"
        } else {
            null
        }

        assignToError = if (assignTo.trim().isBlank() || assignedUserId.trim().isBlank()) {
            "Please select an employee"
        } else {
            null
        }

        taskDetailsError = if (taskDetails.trim().isBlank()) {
            "Task details are required"
        } else {
            null
        }

        return customerError == null &&
                phoneNumberError == null &&
                addressError == null &&
                dateError == null &&
                assignToError == null &&
                taskDetailsError == null
    }

    // Success dialog shown after a task is created successfully.
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
            },
            title = {
                Text("Task created")
            },
            text = {
                Text("The task was created successfully.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
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
        // Top header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
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
                text = "Create Task",
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
                    imageVector = Icons.Outlined.AddTask,
                    contentDescription = "Create Task",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Main form content
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
                onValueChange = {
                    customer = it

                    // Clear the error as soon as the field becomes valid.
                    if (it.trim().isNotBlank()) customerError = null
                },
                errorMessage = customerError
            )

            SmallField(
                label = "Phone Number",
                value = phoneNumber,
                onValueChange = {
                    // Keep only digits in this field.
                    phoneNumber = it.filter { character -> character.isDigit() }

                    if (phoneNumber.length >= 8) {
                        phoneNumberError = null
                    }
                },
                errorMessage = phoneNumberError,
                keyboardType = KeyboardType.Number
            )

            SmallField(
                label = "Address",
                value = address,
                onValueChange = {
                    address = it
                    if (it.trim().isNotBlank()) addressError = null
                },
                errorMessage = addressError
            )

            DateSelectorField(
                label = "Date",
                value = date,
                onClick = { datePickerDialog.show() },
                errorMessage = dateError
            )

            AssignToSelectorField(
                label = "Assign To",
                value = assignTo,
                options = assignedUserOptions,
                expanded = assignToExpanded,
                onExpand = { assignToExpanded = true },
                onDismiss = { assignToExpanded = false },
                onSelect = { selectedUser ->
                    // Save both the display name and the actual user ID.
                    // This is useful because the UI shows a name, but the backend
                    // usually needs a stable user ID.
                    assignTo = selectedUser.fullName
                    assignedUserId = selectedUser.id
                    assignToExpanded = false
                    assignToError = null
                },
                errorMessage = assignToError
            )

            SmallField(
                label = "Task Details",
                value = taskDetails,
                onValueChange = {
                    taskDetails = it
                    if (it.trim().isNotBlank()) taskDetailsError = null
                },
                singleLine = false,
                minLines = 4,
                errorMessage = taskDetailsError
            )

            TaskImagesField(
                imageCount = selectedImageUris.size,
                onPickImages = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearImages = {
                    selectedImageUris.clear()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!validateFields()) return@Button

                    // Build the task object using the current form data.
                    // ID is left blank here because the repository/backend usually
                    // generates it during save.
                    onCreateTask(
                        TaskData(
                            id = "",
                            customer = customer.trim(),
                            phoneNumber = phoneNumber.trim(),
                            address = address.trim(),
                            date = date,
                            assignTo = assignTo.trim(),
                            assignedUserId = assignedUserId.trim(),
                            taskDetails = taskDetails.trim(),
                            status = TaskStatus.PENDING,
                            imageUrls = emptyList()
                        ),
                        selectedImageUris.toList()
                    )

                    // Reset form after successful create.
                    customer = ""
                    phoneNumber = ""
                    address = ""
                    date = ""
                    assignTo = ""
                    assignedUserId = ""
                    taskDetails = ""
                    assignToExpanded = false
                    selectedImageUris.clear()

                    // Clear any old validation state.
                    customerError = null
                    phoneNumberError = null
                    addressError = null
                    dateError = null
                    assignToError = null
                    taskDetailsError = null

                    showSuccessDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = buttonTextColor
                )
            ) {
                Text(
                    text = "Create Task",
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

/**
 * Reusable text input field used across the create-task form.
 *
 * Supports:
 * - normal single-line text
 * - multi-line text
 * - keyboard type customization
 * - inline error display
 */
@Composable
private fun SmallField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
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
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = if (errorMessage != null) Color(0xFFC62828) else Color(0xFF9AA0A6),
                unfocusedBorderColor = if (errorMessage != null) Color(0xFFC62828) else Color(0xFF9AA0A6),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFC62828),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Read-only field that opens a date picker when tapped.
 *
 * We use a transparent clickable overlay instead of making the text field
 * itself editable, so the user can only select valid dates through the picker.
 */
@Composable
private fun DateSelectorField(
    label: String,
    value: String,
    onClick: () -> Unit,
    errorMessage: String? = null
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
                placeholder = { Text("Select date") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Select date",
                        tint = Color(0xFF666666)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = if (errorMessage != null) Color(0xFFC62828) else Color(0xFF9AA0A6),
                    unfocusedBorderColor = if (errorMessage != null) Color(0xFFC62828) else Color(0xFF9AA0A6),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            // Invisible overlay that captures taps without showing ripple/field editing.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFC62828),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Read-only dropdown field for selecting which employee the task should be assigned to.
 *
 * Stores both:
 * - the visible name in the field
 * - the real selected [User] object through [onSelect]
 */
@Composable
private fun AssignToSelectorField(
    label: String,
    value: String,
    options: List<User>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (User) -> Unit,
    errorMessage: String? = null
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
                placeholder = { Text("Select employee") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Open employee list",
                        tint = Color(0xFF666666)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = if (errorMessage != null) Color(0xFFC62828) else Color(0xFF9AA0A6),
                    unfocusedBorderColor = if (errorMessage != null) Color(0xFFC62828) else Color(0xFF9AA0A6),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            // Transparent overlay so the whole field opens the menu when tapped.
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
                options.forEach { user ->
                    DropdownMenuItem(
                        text = { Text(user.fullName.ifBlank { user.username }) },
                        onClick = { onSelect(user) }
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFC62828),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Image selection field for task photos.
 *
 * Shows:
 * - a button to open the image picker
 * - number of selected images
 * - a small clear button when images are selected
 *
 * This component only manages display + callbacks.
 * The actual list of URIs is owned by the parent screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskImagesField(
    imageCount: Int,
    onPickImages: () -> Unit,
    onClearImages: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Task Images",
            color = Color(0xFF444444),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onPickImages,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.AddPhotoAlternate,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (imageCount == 0) {
                    "Add pictures"
                } else {
                    "$imageCount picture(s) selected"
                }
            )
        }

        if (imageCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$imageCount selected",
                    color = Color(0xFF666666),
                    fontSize = 13.sp
                )

                IconButton(
                    onClick = onClearImages,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear images",
                        tint = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}