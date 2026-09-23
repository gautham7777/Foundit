package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val id: String,
    val reporterId: String,
    val targetType: String, // "POST" or "USER"
    val targetId: String,
    val reason: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users")
data class BlockedUser(
    @PrimaryKey val id: String, // userId_blockedUserId
    val userId: String,
    val blockedUserId: String,
    val timestamp: Long = System.currentTimeMillis()
)
