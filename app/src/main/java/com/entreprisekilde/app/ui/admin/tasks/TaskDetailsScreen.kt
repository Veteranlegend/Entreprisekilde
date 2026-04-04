package com.entreprisekilde.app.ui.admin.tasks

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.entreprisekilde.app.data.model.task.TaskData
import com.entreprisekilde.app.data.model.task.TaskImageData
import com.entreprisekilde.app.data.model.task.TaskImageSource
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Task details screen used to view and edit a task.
 *
 * This screen lets the user:
 * - edit the main task information
 * - open the task address in maps
 * - change the task date
 * - reassign the task
 * - review images added at creation time
 * - add and review extra images uploaded later
 * - save the edited task
 *
 * The composable is mostly UI/state handling. Actual persistence is delegated
 * upward through [onSaveEdit] and [onAddImages].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailsScreen(
    task: TaskData,
    onBack: () -> Unit = {},
    onSaveEdit: (TaskData) -> Unit = {},
    onAddImages: (TaskData, List<Uri>) -> Unit = { _, _ -> },
    assignedUserOptions: List<String> = emptyList(),
    unreadNotificationCount: Int = 0,
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // We keep one formatter around for consistent dd/MM/yyyy display formatting.
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    /**
     * Local editable state for each task field.
     *
     * Using remember(task.id) means the form resets correctly when a different
     * task is opened, while still preserving local edits during recomposition.
     */
    var customer by remember(task.id) { mutableStateOf(task.customer) }
    var phoneNumber by remember(task.id) { mutableStateOf(task.phoneNumber) }
    var address by remember(task.id) { mutableStateOf(task.address) }
    var date by remember(task.id) { mutableStateOf(normalizeDateForDisplay(task.date)) }
    var assignTo by remember(task.id) { mutableStateOf(task.assignTo) }
    var taskDetails by remember(task.id) { mutableStateOf(task.taskDetails) }

    // Controls dropdown visibility for the "Assign To" selector.
    var assignToExpanded by remember { mutableStateOf(false) }

    // Dialog state for save confirmation.
    var showSavedDialog by remember { mutableStateOf(false) }

    // Dialog state shown when no maps app/browser fallback can be opened.
    var showMapsErrorDialog by remember { mutableStateOf(false) }

    /**
     * Keeps track of any newly selected image Uris.
     *
     * This list is not currently rendered directly, but it can still be useful
     * for future preview/temporary state needs. Right now the actual add action
     * is immediately passed upward through [onAddImages].
     */
    val selectedNewImageUris = remember { mutableStateListOf<Uri>() }

    // When non-null, we show an image preview dialog for the selected image URL.
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    /**
     * Split images into two groups so the UI can present them more clearly:
     * - images attached when the task was originally created
     * - images uploaded later from the details screen
     */
    val createdImages = remember(task.images) {
        task.images.filter { it.source == TaskImageSource.CREATED }
    }

    val uploadedLaterImages = remember(task.images) {
        task.images.filter { it.source == TaskImageSource.DETAILS }
    }

    /**
     * Image picker launcher for adding more detail/progress images.
     *
     * We allow multiple image selection here since users may want to upload
     * several progress photos in one action.
     */
    val addImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedNewImageUris.clear()
            selectedNewImageUris.addAll(uris)
            onAddImages(task, uris)
        }
    }

    /**
     * Figure out which date the date picker should open on.
     *
     * If the current value cannot be parsed for any reason, we fall back to today
     * instead of crashing or showing a broken dialog.
     */
    val initialDate = parseToLocalDate(date) ?: LocalDate.now()

    /**
     * Android platform date picker dialog.
     *
     * We wrap it in remember(initialDate) so it is recreated when the current
     * editable date meaningfully changes.
     */
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
                // Prefer the calendar-style picker over spinners when possible.
                datePicker.calendarViewShown = true
                datePicker.spinnersShown = false
            } catch (_: Exception) {
                // Some device implementations may not support these tweaks.
            }
        }
    }

    /**
     * Simple success dialog shown after saving task changes.
     */
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

    /**
     * Error dialog shown if we fail to open maps and also fail the browser fallback.
     */
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

    /**
     * Full-screen-ish image preview dialog for whichever task image was tapped.
     *
     * We also expose a simple "Download / Open" button which hands the URL off
     * to the system so the user can view/download it externally.
     */
    selectedImageUrl?.let { imageUrl ->
        AlertDialog(
            onDismissRequest = { selectedImageUrl = null },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedImageUrl = null }) {
                    Text("Close")
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Download / Open")
                    }
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
        /**
         * Header row for the screen.
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

        /**
         * Main scrollable form content.
         */
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

            /**
             * Opens the current address in a maps app.
             *
             * Strategy:
             * 1. Try a geo: intent first (best for installed maps apps)
             * 2. Fall back to Google Maps in the browser
             * 3. If both fail, show an error dialog
             */
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

            /**
             * Read-only section for images originally attached during task creation.
             */
            TaskImagesSection(
                title = "Created Images",
                subtitle = "Images added when the task was created",
                images = createdImages,
                emptyText = "No created images",
                onImageClick = { imageUrl ->
                    selectedImageUrl = imageUrl
                }
            )

            /**
             * Section for images uploaded later from the task details workflow.
             *
             * This one exposes an "Add" button because these images are expected
             * to be updated over time.
             */
            TaskImagesSection(
                title = "Uploaded Later",
                subtitle = "Images added from task details",
                images = uploadedLaterImages,
                emptyText = "No later uploads yet",
                showAddButton = true,
                onAddImagesClick = {
                    addImagesLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onImageClick = { imageUrl ->
                    selectedImageUrl = imageUrl
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            /**
             * Save button.
             *
             * We trim all editable text values before saving so we do not
             * accidentally persist leading/trailing whitespace.
             */
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

/**
 * Reusable labeled text field used throughout the task form.
 *
 * This keeps the form layout consistent and avoids repeating the same styling
 * for every field.
 */
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

/**
 * Read-only field that behaves like a date picker trigger.
 *
 * We render it as an OutlinedTextField for visual consistency with the rest of
 * the form, then place a transparent clickable overlay on top so the whole field
 * behaves like a picker button.
 */
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

            // Invisible overlay so the whole field is tappable without showing
            // text-field editing behavior/cursor.
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

/**
 * Read-only dropdown selector for assigning the task to a user.
 *
 * Like [DateSelectorField], it uses a text-field look for visual consistency
 * while behaving like a picker.
 */
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

            // Invisible overlay to make the whole field open the dropdown.
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

/**
 * Reusable image section used for both:
 * - created images
 * - images uploaded later
 *
 * It supports:
 * - title/subtitle
 * - empty state
 * - optional "Add" button
 * - clickable image thumbnails
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskImagesSection(
    title: String,
    subtitle: String,
    images: List<TaskImageData>,
    emptyText: String,
    showAddButton: Boolean = false,
    onAddImagesClick: () -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color(0xFF444444),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    color = Color(0xFF777777),
                    fontSize = 12.sp
                )
            }

            if (showAddButton) {
                TextButton(onClick = onAddImagesClick) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text("Add")
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (images.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF888888)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = emptyText,
                    color = Color(0xFF777777),
                    fontSize = 14.sp
                )
            }
        } else {
            /**
             * FlowRow works well here because it creates a neat wrapping thumbnail
             * grid without forcing us into a fixed-column layout.
             */
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                images.forEach { image ->
                    Column(
                        modifier = Modifier.width(110.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(image.imageUrl),
                            contentDescription = title,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .clickable {
                                    onImageClick(image.imageUrl)
                                },
                            contentScale = ContentScale.Crop
                        )

                        // For later-uploaded images, show who uploaded the image
                        // when that information is available.
                        if (image.source == TaskImageSource.DETAILS && image.uploadedByName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By ${image.uploadedByName}",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses a raw date string into [LocalDate].
 *
 * Supported formats:
 * - dd/MM/yyyy
 * - yyyy-MM-dd
 *
 * Returning null instead of throwing keeps this helper safe to use anywhere in
 * the UI layer.
 */
private fun parseToLocalDate(date: String): LocalDate? {
    val inputFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    for (formatter in inputFormats) {
        try {
            return LocalDate.parse(date, formatter)
        } catch (_: Exception) {
            // Try the next known format.
        }
    }

    return null
}

/**
 * Normalizes a date string to the app's preferred display format: dd/MM/yyyy.
 *
 * If parsing fails, we return the original value unchanged rather than risking
 * data loss or a crash.
 */
private fun normalizeDateForDisplay(date: String): String {
    val parsed = parseToLocalDate(date) ?: return date
    return parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}