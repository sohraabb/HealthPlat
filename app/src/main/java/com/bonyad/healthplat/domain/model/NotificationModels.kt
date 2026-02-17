package com.bonyad.healthplat.domain.model

data class NotificationItem(
    val id: Long,
    val title: String,
    val description: String,
    val date: String, // Persian date format like "۱۴۰۴/۰۶/۲۰"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)