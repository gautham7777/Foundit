package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LostWatch
import kotlinx.coroutines.flow.Flow

@Dao
interface LostWatchDao {
    @Query("SELECT * FROM lost_watches WHERE userId = :userId ORDER BY createdAt DESC")
    fun getWatchesByUser(userId: String): Flow<List<LostWatch>>

    @Query("SELECT * FROM lost_watches ORDER BY createdAt DESC")
    fun getAllWatches(): Flow<List<LostWatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatch(watch: LostWatch)

    @Query("DELETE FROM lost_watches WHERE id = :id")
    suspend fun deleteWatch(id: String)
}
