package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lost_watches")
data class LostWatch(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val description: String,
    val approxLocation: String,
    val latitude: Double,
    val longitude: Double,
    val dateLost: Long,
    val createdAt: Long = System.currentTimeMillis()
)
