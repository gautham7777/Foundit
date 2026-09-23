package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM conversations WHERE finderId = :userId OR claimantId = :userId ORDER BY lastMessageTimestamp DESC")
    fun getConversationsForUser(userId: String): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: String): Flow<Conversation?>

    @Query("SELECT * FROM conversations WHERE itemId = :itemId AND ((finderId = :u1 AND claimantId = :u2) OR (finderId = :u2 AND claimantId = :u1)) LIMIT 1")
    suspend fun findConversation(itemId: String, u1: String, u2: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: Conversation)

    @Update
    suspend fun updateConversation(conv: Conversation)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessage)

    @Query("UPDATE conversations SET lastMessageText = :text, lastMessageTimestamp = :time WHERE id = :convId")
    suspend fun updateLastMessage(convId: String, text: String, time: Long)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :convId")
    suspend fun markConversationRead(convId: String)
}
