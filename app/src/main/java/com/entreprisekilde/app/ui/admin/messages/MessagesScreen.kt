package com.entreprisekilde.app.ui.admin.messages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.messages.MessageThread
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import kotlin.math.roundToInt

/**
 * Admin messages overview screen.
 *
 * This screen shows:
 * - a searchable list of message threads
 * - unread indicators
 * - swipe-to-delete interaction for each thread
 * - a floating action button for starting a new chat
 *
 * The actual message thread content is handled elsewhere. This screen is mainly
 * the "inbox" / conversation list view.
 */
@Composable
fun MessagesScreen(
    threads: List<MessageThread>,
    unreadNotificationCount: Int = 0,
    onThreadClick: (MessageThread) -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onDeleteThread: (MessageThread) -> Unit = {},
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Local search state used to filter the visible thread list.
    var searchText by remember { mutableStateOf("") }

    // Filter threads by recipient name or last message text.
    // Search is case-insensitive for a more forgiving UI experience.
    val filteredThreads = threads.filter {
        searchText.isBlank() ||
                it.recipientName.contains(searchText, ignoreCase = true) ||
                it.lastMessage.contains(searchText, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top app bar area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0A673))
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Message",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = "Messages",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Search field for narrowing down the visible threads.
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
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Thread list.
                // Each item supports swipe-to-reveal delete action.
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredThreads, key = { it.id }) { thread ->
                        SwipeToRevealThreadCard(
                            thread = thread,
                            onClick = { onThreadClick(thread) },
                            onDeleteConfirmed = { onDeleteThread(thread) }
                        )
                    }
                }
            }

            // Bottom navigation shared with the rest of the app.
            AppBottomNavBar(
                selectedItem = BottomNavDestination.MESSAGES,
                unreadNotificationCount = unreadNotificationCount,
                onHomeClick = onHomeClick,
                // Already on the messages screen, so we keep this as a no-op.
                onMessagesClick = {},
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick
            )
        }

        // Floating "new chat" button positioned above the bottom nav bar.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 100.dp)
                .size(60.dp)
                .background(Color(0xFF5FA8F5), CircleShape)
                .clickable { onNewChatClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Chat",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

/**
 * Message thread card with swipe-to-reveal delete action.
 *
 * Interaction behavior:
 * - swipe left to reveal delete area
 * - tap delete area to open confirmation dialog
 * - tap card while closed to open thread
 * - tap card while open to close it again
 *
 * Each card remembers its own swipe state based on [thread.id].
 */
@Composable
private fun SwipeToRevealThreadCard(
    thread: MessageThread,
    onClick: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    // Max distance the card can slide left to reveal the delete action.
    val maxRevealPx = 160f

    // Current raw drag position for this card.
    var dragOffset by remember(thread.id) { mutableFloatStateOf(0f) }

    // Controls the confirmation dialog visibility.
    var showDeleteDialog by remember(thread.id) { mutableStateOf(false) }

    // Animate the offset so opening/closing feels smoother.
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        label = "thread_swipe_offset"
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete chat?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete the chat with \"${thread.recipientName}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirmed()
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
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        // Background delete action that becomes visible when the card slides left.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(92.dp)
                    .background(Color(0xFFE58C8C), RoundedCornerShape(18.dp))
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Foreground card content that slides horizontally on drag.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .background(Color.White, RoundedCornerShape(18.dp))
                .then(
                    // Unread threads get a border so they stand out more in the list.
                    if (thread.unreadCount > 0) {
                        Modifier.border(1.dp, Color.Black, RoundedCornerShape(18.dp))
                    } else {
                        Modifier
                    }
                )
                .pointerInput(thread.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            // Allow swipe only within the supported range:
                            // 0f = fully closed, -maxRevealPx = fully revealed.
                            dragOffset = (dragOffset + dragAmount).coerceIn(-maxRevealPx, 0f)
                        },
                        onDragEnd = {
                            // Snap open or closed depending on how far the user dragged.
                            dragOffset = if (dragOffset < -maxRevealPx / 2f) {
                                -maxRevealPx
                            } else {
                                0f
                            }
                        }
                    )
                }
                .clickable {
                    if (dragOffset == 0f) {
                        // Normal click behavior when card is closed.
                        onClick()
                    } else {
                        // If currently swiped open, a tap closes it instead of opening the chat.
                        dragOffset = 0f
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar / user icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFFB7DDFC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SupervisorAccount,
                    contentDescription = null,
                    tint = Color(0xFF49A7EE),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = thread.recipientName,
                    fontSize = 16.sp,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Human-friendly relative timestamp for last activity in the thread.
                Text(
                    text = formatTimeAgo(thread.updatedAt),
                    fontSize = 14.sp,
                    color = Color(0xFF777777)
                )
            }

            // Unread counter badge shown only when there are unread messages.
            if (thread.unreadCount > 0) {
                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF55A8E3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = thread.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Converts a timestamp into a short relative time label.
 *
 * Examples:
 * - "Just now"
 * - "5 min ago"
 * - "3 h ago"
 * - "2 d ago"
 * - "1 w ago"
 *
 * Returns an empty string for invalid/non-positive timestamps.
 */
private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return ""

    val now = System.currentTimeMillis()
    val diffMillis = (now - timestamp).coerceAtLeast(0L)

    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    val week = 7 * day

    return when {
        diffMillis < minute -> "Just now"
        diffMillis < hour -> "${diffMillis / minute} min ago"
        diffMillis < day -> "${diffMillis / hour} h ago"
        diffMillis < week -> "${diffMillis / day} d ago"
        else -> "${diffMillis / week} w ago"
    }
}