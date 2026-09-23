package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "claims")
data class Claim(
    @PrimaryKey val id: String,
    val itemId: String,
    val claimantId: String,
    val claimantName: String,
    val claimantAvatar: String? = null,
    val verificationAnswer: String,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)
