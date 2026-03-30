package com.entreprisekilde.app.ui.admin.dashboard

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SupervisorAccount
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
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination

@Composable
fun AdminDashboardScreen(
    unreadNotificationCount: Int = 0,
    onAllTasksClick: () -> Unit = {},
    onCreateTaskClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onTimesheetClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
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
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dashboard",
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
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = "Dashboard",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DashboardButton(
                text = "All Tasks",
                icon = Icons.Outlined.List,
                onClick = onAllTasksClick
            )

            DashboardButton(
                text = "Create Task",
                icon = Icons.Outlined.CheckCircle,
                onClick = onCreateTaskClick
            )

            DashboardButton(
                text = "Calendar",
                icon = Icons.Outlined.CalendarMonth,
                onClick = onCalendarClick
            )

            DashboardButton(
                text = "Messages",
                icon = Icons.Outlined.Message,
                onClick = onMessagesClick
            )

            DashboardButton(
                text = "Timesheet",
                icon = Icons.Outlined.Schedule,
                onClick = onTimesheetClick
            )

            DashboardButton(
                text = "Users",
                icon = Icons.Outlined.SupervisorAccount,
                onClick = onUsersClick
            )
        }

        Box(modifier = Modifier.navigationBarsPadding()) {
            AppBottomNavBar(
                selectedItem = BottomNavDestination.HOME,
                unreadNotificationCount = unreadNotificationCount,
                onHomeClick = {},
                onMessagesClick = onMessagesClick,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick
            )
        }
    }
}

@Composable
private fun DashboardButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF7FA8D6), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.Black,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.size(20.dp))

        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}