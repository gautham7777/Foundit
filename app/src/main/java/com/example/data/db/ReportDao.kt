package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockedUser
import com.example.data.model.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockUser(blockedUser: BlockedUser)

    @Query("SELECT blockedUserId FROM blocked_users WHERE userId = :userId")
    fun getBlockedUserIds(userId: String): Flow<List<String>>

    @Query("DELETE FROM blocked_users WHERE userId = :userId AND blockedUserId = :targetUserId")
    suspend fun unblockUser(userId: String, targetUserId: String)
}
