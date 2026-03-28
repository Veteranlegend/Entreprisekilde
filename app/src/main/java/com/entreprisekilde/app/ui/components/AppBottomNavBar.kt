package com.entreprisekilde.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomNavBar(
    selectedItem: BottomNavDestination,
    unreadNotificationCount: Int = 0,
    onHomeClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Home",
            icon = Icons.Outlined.Home,
            color = if (selectedItem == BottomNavDestination.HOME) Color.Black else Color(0xFF9F98AA),
            onClick = onHomeClick
        )

        BottomNavItem(
            label = "Message",
            icon = Icons.Outlined.ChatBubbleOutline,
            color = if (selectedItem == BottomNavDestination.MESSAGES) Color.Black else Color(0xFF9F98AA),
            onClick = onMessagesClick
        )

        NotificationBottomNavItem(
            label = "Notification",
            icon = Icons.Outlined.Inventory2,
            color = if (selectedItem == BottomNavDestination.NOTIFICATIONS) Color.Black else Color(0xFF9F98AA),
            unreadCount = unreadNotificationCount,
            onClick = onNotificationsClick
        )

        BottomNavItem(
            label = "Profile",
            icon = Icons.Outlined.PersonOutline,
            color = if (selectedItem == BottomNavDestination.PROFILE) Color.Black else Color(0xFF9F98AA),
            onClick = onProfileClick
        )
    }
}

enum class BottomNavDestination {
    HOME,
    MESSAGES,
    NOTIFICATIONS,
    PROFILE
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() }
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

@Composable
private fun NotificationBottomNavItem(
    label: String,
    icon: ImageVector,
    color: Color,
    unreadCount: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color
            )

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(Color(0xFFE35B5B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            color = color
        )
    }
}