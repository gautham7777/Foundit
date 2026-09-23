package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Claim
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimDao {
    @Query("SELECT * FROM claims WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun getClaimsForItem(itemId: String): Flow<List<Claim>>

    @Query("SELECT * FROM claims WHERE claimantId = :userId ORDER BY createdAt DESC")
    fun getClaimsByUser(userId: String): Flow<List<Claim>>

    @Query("SELECT * FROM claims WHERE id = :id LIMIT 1")
    suspend fun getClaimById(id: String): Claim?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: Claim)

    @Update
    suspend fun updateClaim(claim: Claim)

    @Query("UPDATE claims SET status = :status WHERE id = :id")
    suspend fun updateClaimStatus(id: String, status: String)
}
