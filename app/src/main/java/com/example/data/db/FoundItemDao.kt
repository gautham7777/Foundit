package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FoundItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoundItemDao {
    @Query("SELECT * FROM found_items ORDER BY createdAt DESC")
    fun getAllFoundItems(): Flow<List<FoundItem>>

    @Query("SELECT * FROM found_items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<FoundItem?>

    @Query("SELECT * FROM found_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirect(id: String): FoundItem?

    @Query("SELECT * FROM found_items WHERE finderId = :finderId ORDER BY createdAt DESC")
    fun getItemsByFinder(finderId: String): Flow<List<FoundItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FoundItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoundItem>)

    @Update
    suspend fun updateItem(item: FoundItem)

    @Query("UPDATE found_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM found_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("SELECT COUNT(*) FROM found_items")
    suspend fun getItemCount(): Int
}
