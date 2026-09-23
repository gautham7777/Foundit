package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val itemsReturned: Int = 0,
    val postsCreated: Int = 0,
    val isCurrentUser: Boolean = false,
    val communityBadge: String = "Community Member"
)
