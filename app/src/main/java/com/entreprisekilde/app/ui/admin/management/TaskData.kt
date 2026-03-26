package com.entreprisekilde.app.ui.admin.management

data class TaskData(
    val customer: String,
    val phoneNumber: String,
    val address: String,
    val date: String,
    val assignTo: String,
    val taskDetails: String,
    var status: String = "Pending"
)