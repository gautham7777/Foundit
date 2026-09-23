package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val itemId: String,
    val itemTitle: String,
    val itemCategory: String,
    val finderId: String,
    val finderName: String,
    val claimantId: String,
    val claimantName: String,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val verificationAnswerSnippet: String? = null
)
