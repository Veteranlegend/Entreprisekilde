package com.entreprisekilde.app.ui.admin.messages

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread
import java.io.File

@Composable
fun ChatScreen(
    thread: MessageThread,
    messages: SnapshotStateList<ChatMessage>,
    loggedInUserId: String,
    onBack: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onSendImage: (Uri) -> Unit = {},
    onMessageTextChanged: (String) -> Unit = {},
    onMarkAsRead: () -> Unit = {},
    onStopTyping: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    var showImageOptions by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        showImageOptions = false
        if (uri != null) {
            onSendImage(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        showImageOptions = false
        if (success && uri != null) {
            onSendImage(uri)
        }
    }

    val isOtherUserTyping = thread.typingUserIds.any { it != loggedInUserId }

    LaunchedEffect(messages.size, thread.id) {
        onMarkAsRead()
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopTyping()
        }
    }

    val lastMyMessageId = messages
        .lastOrNull { it.senderId == loggedInUserId }
        ?.id

    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = {
                Text(
                    text = "Send image",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F7FB), RoundedCornerShape(14.dp))
                            .clickable {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color(0xFF5F6B7A)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Choose from gallery",
                            color = Color.Black
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F7FB), RoundedCornerShape(14.dp))
                            .clickable {
                                val uri = createImageUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF5F6B7A)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Take photo",
                            color = Color.Black
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showImageOptions = false }
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onStopTyping()
                    onBack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFB7DDFC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SupervisorAccount,
                    contentDescription = null,
                    tint = Color(0xFF49A7EE)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = thread.recipientName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                if (isOtherUserTyping) {
                    Text(
                        text = "Typing...",
                        fontSize = 12.sp,
                        color = Color(0xFF3B6E57)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = messages,
                key = { _, message -> message.id }
            ) { _, message ->
                val isFromMe = message.senderId == loggedInUserId
                val isLastMyMessage = message.id == lastMyMessageId
                val isSeenByOtherUser = thread.recipientId in message.readByUserIds

                MessageBubble(
                    message = message,
                    isFromMe = isFromMe,
                    showSeenStatus = isFromMe && isLastMyMessage,
                    isSeen = isSeenByOtherUser
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE9ECF2), CircleShape)
                    .clickable {
                        showImageOptions = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "Choose image",
                    tint = Color(0xFF5F6B7A)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                    onMessageTextChanged(it)
                },
                placeholder = { Text("Write a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = if (messageText.isNotBlank()) Color(0xFF6F92F6)
                        else Color(0xFFD9DCE3),
                        shape = CircleShape
                    )
                    .clickable {
                        val trimmed = messageText.trim()
                        if (trimmed.isNotBlank()) {
                            onSendMessage(trimmed)
                            messageText = ""
                            onMessageTextChanged("")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    showSeenStatus: Boolean,
    isSeen: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isFromMe) Color(0xFF7F9DF7)
                    else Color(0xFFE8E8E8),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(10.dp)
        ) {
            if (message.messageType == ChatMessage.MESSAGE_TYPE_IMAGE && message.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(message.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = message.text,
                    color = if (isFromMe) Color.White else Color(0xFF333333),
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (message.time.isNotBlank()) {
                Text(
                    text = message.time,
                    fontSize = 11.sp,
                    color = Color(0xFF8A8A8A)
                )
            }

            if (showSeenStatus) {
                Text(
                    text = if (isSeen) "Seen" else "Sent",
                    fontSize = 11.sp,
                    color = Color(0xFF8A8A8A)
                )
            }
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "chat_camera_images").apply {
        if (!exists()) mkdirs()
    }

    val imageFile = File.createTempFile(
        "chat_photo_${System.currentTimeMillis()}",
        ".jpg",
        imagesDir
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}