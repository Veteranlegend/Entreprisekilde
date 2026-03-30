package com.entreprisekilde.app.ui.admin.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.messages.ChatMessage
import com.entreprisekilde.app.data.model.messages.MessageThread

@Composable
fun ChatScreen(
    thread: MessageThread,
    messages: SnapshotStateList<ChatMessage>,
    loggedInUserId: String,
    onBack: () -> Unit = {},
    onSendMessage: (String) -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {

        // 🔹 TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
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

            Text(
                text = thread.recipientName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // 🔹 MESSAGES LIST
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isFromMe = message.senderId == loggedInUserId
                )
            }
        }

        // 🔹 INPUT FIELD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
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
    isFromMe: Boolean
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
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isFromMe) Color.White else Color(0xFF333333),
                fontSize = 16.sp
            )
        }

        if (message.time.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.time,
                fontSize = 11.sp,
                color = Color(0xFF8A8A8A)
            )
        }
    }
}