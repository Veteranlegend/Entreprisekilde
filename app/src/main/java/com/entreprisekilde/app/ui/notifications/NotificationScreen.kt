package com.entreprisekilde.app.ui.notifications

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.notifications.AppNotification
import com.entreprisekilde.app.data.model.notifications.NotificationType
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination

/**
 * Main notifications screen.
 *
 * This screen is responsible for:
 * - showing the current list of notifications
 * - displaying unread status
 * - letting the user mark everything as read
 * - opening or deleting individual notifications
 * - keeping the bottom navigation in sync with the rest of the app
 *
 * The actual business logic is intentionally passed in through callbacks,
 * which keeps this composable focused on UI only.
 */
@Composable
fun NotificationScreen(
    notifications: SnapshotStateList<AppNotification>,
    unreadCount: Int,
    onBack: () -> Unit = {},
    onScreenOpened: () -> Unit = {},
    onMarkAllAsRead: () -> Unit = {},
    onNotificationClick: (AppNotification) -> Unit = {},
    onDeleteNotification: (AppNotification) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onScreenClosed: () -> Unit = {}
) {
    /**
     * Notify the parent when this screen is opened or when the notification
     * list changes size. That can be useful for refreshing state, analytics,
     * or marking that the user has visited the screen.
     */
    LaunchedEffect(notifications.size) {
        onScreenOpened()
    }

    /**
     * Runs cleanup logic when the composable leaves the composition.
     * Useful for lifecycle-style tracking without putting that logic directly in the UI.
     */
    DisposableEffect(Unit) {
        onDispose {
            onScreenClosed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            // Applies safe area padding so content does not overlap status/system bars.
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        /**
         * Header section for the screen.
         *
         * Uses a warm background color and a circular icon container to make
         * the notifications page feel visually distinct from the main content area.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
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
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = "Notifications",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        /**
         * Summary row shown only when there are notifications in the list.
         *
         * It gives the user quick feedback on unread items and shows a
         * "mark all as read" action only when that action is actually relevant.
         */
        if (notifications.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                if (unreadCount > 0) {
                    Button(
                        onClick = onMarkAllAsRead,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7FA8D6),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Mark all as read")
                    }
                }
            }
        }

        /**
         * Empty state shown when there are no notifications yet.
         *
         * This is more user-friendly than leaving the screen blank and helps
         * explain what kind of content will eventually appear here.
         */
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF9F98AA),
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    Text(
                        text = "No notifications yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.size(6.dp))

                    Text(
                        text = "Messages and assigned tasks will appear here.",
                        fontSize = 14.sp,
                        color = Color(0xFF777777),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            /**
             * Main notifications list.
             *
             * LazyColumn is used here so the UI remains efficient even if the
             * number of notifications grows over time.
             *
             * We use each notification's id as the key so Compose can keep
             * item identity stable during recomposition.
             */
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = { onNotificationClick(notification) },
                        onDelete = { onDeleteNotification(notification) }
                    )
                }
            }
        }

        /**
         * Bottom navigation stays visible at the bottom of the screen.
         *
         * Notifications is marked as the active destination, and we intentionally
         * pass an empty lambda for onNotificationsClick because the user is already here.
         */
        AppBottomNavBar(
            selectedItem = BottomNavDestination.NOTIFICATIONS,
            unreadNotificationCount = unreadCount,
            onHomeClick = onHomeClick,
            onMessagesClick = onMessagesClick,
            onNotificationsClick = {},
            onProfileClick = onProfileClick
        )
    }
}

/**
 * Single notification row/card.
 *
 * This card changes its background color depending on whether the notification
 * has been read. It also changes the leading icon and icon background color
 * based on the notification type.
 */
@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                // Unread notifications get a subtle blue tint so they stand out a bit more.
                color = if (notification.isRead) Color.White else Color(0xFFEAF3FF),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    // Message and task-like notifications get different accent colors
                    // so the user can quickly distinguish them visually.
                    color = if (notification.type == NotificationType.MESSAGE) {
                        Color(0xFFB7DDFC)
                    } else {
                        Color(0xFFD8EACF)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (notification.type == NotificationType.MESSAGE) {
                    Icons.Outlined.MailOutline
                } else {
                    Icons.Outlined.AssignmentTurnedIn
                },
                contentDescription = null,
                tint = Color.Black
            )
        }

        Spacer(modifier = Modifier.size(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = notification.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = notification.message,
                fontSize = 14.sp,
                color = Color(0xFF555555)
            )

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = formatTimeAgo(notification.createdAt),
                fontSize = 12.sp,
                color = Color(0xFF8A8A8A)
            )
        }

        /**
         * Inline delete action.
         *
         * This gives the user a quick way to remove a notification without
         * needing to open it first.
         */
        Text(
            text = "Delete",
            color = Color(0xFFB06D6D),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onDelete() }
        )
    }
}

/**
 * Converts a timestamp into a short "time ago" label.
 *
 * Examples:
 * - Just now
 * - 5 min ago
 * - 3 h ago
 * - 2 d ago
 * - 1 w ago
 *
 * The output is intentionally compact because it is used inside a small card layout.
 */
private fun formatTimeAgo(createdAt: Long): String {
    val now = System.currentTimeMillis()

    // Guard against negative values in case createdAt is in the future for some reason.
    val diffMillis = (now - createdAt).coerceAtLeast(0L)

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