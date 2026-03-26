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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun CreateTaskScreen(
    onBack: () -> Unit = {},
    onCreateTask: (TaskData) -> Unit = {}
) {
    var customer by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var assignTo by remember { mutableStateOf("") }
    var taskDetails by remember { mutableStateOf("") }

    val pageBackground = Color(0xFFD8CBCB)
    val whitePanel = Color(0xFFF5F5F5)
    val headerColor = Color(0xFFE0A673)
    val buttonColor = Color(0xFFCFE0D0)
    val buttonTextColor = Color(0xFF3F6E48)
    val inactiveColor = Color(0xFF9F98AA)
    val borderBlue = Color(0xFF2196F3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(whitePanel)
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
                    .background(headerColor)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Create Task",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .background(Color.Transparent, RoundedCornerShape(6.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(whitePanel)
                                .padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Transparent)
                                        .padding(0.dp)
                                ) {
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Transparent)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Transparent)
                                                .padding(0.dp)
                                        ) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(horizontal = 0.dp, vertical = 0.dp)
                ) {


                    SmallField(
                        label = "Customer:",
                        value = customer,
                        onValueChange = { customer = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SmallField(
                        label = "Phone Number:",
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SmallField(
                        label = "Address:",
                        value = address,
                        onValueChange = { address = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SmallField(
                        label = "Date",
                        value = date,
                        onValueChange = { date = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SmallField(
                        label = "Assign to",
                        value = assignTo,
                        onValueChange = { assignTo = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SmallField(
                        label = "Task Details",
                        value = taskDetails,
                        onValueChange = { taskDetails = it },
                        singleLine = false,
                        minLines = 3
                    )
                }
            }

            Button(
                onClick = {
                    onCreateTask(
                        TaskData(
                            customer = customer,
                            phoneNumber = phoneNumber,
                            address = address,
                            date = date,
                            assignTo = assignTo,
                            taskDetails = taskDetails,
                            status = "Pending"
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp)
                    .width(230.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem("Home", Icons.Outlined.Home, Color.Black)
                BottomNavItem("Message", Icons.Outlined.ChatBubbleOutline, inactiveColor)
                BottomNavItem("Notification", Icons.Outlined.Inventory2, inactiveColor)
                BottomNavItem("Profile", Icons.Outlined.PersonOutline, inactiveColor)
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF444444),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (singleLine) 58.dp else 110.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
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