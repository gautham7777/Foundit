package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BlockedUser
import com.example.data.model.ChatMessage
import com.example.data.model.Claim
import com.example.data.model.Conversation
import com.example.data.model.FoundItem
import com.example.data.model.LostWatch
import com.example.data.model.NotificationItem
import com.example.data.model.Report
import com.example.data.model.User

@Database(
    entities = [
        User::class,
        FoundItem::class,
        Claim::class,
        Conversation::class,
        ChatMessage::class,
        NotificationItem::class,
        Report::class,
        BlockedUser::class,
        LostWatch::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FoundItDatabase : RoomDatabase() {
    abstract fun foundItemDao(): FoundItemDao
    abstract fun userDao(): UserDao
    abstract fun claimDao(): ClaimDao
    abstract fun messageDao(): MessageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun reportDao(): ReportDao
    abstract fun lostWatchDao(): LostWatchDao

    companion object {
        @Volatile
        private var INSTANCE: FoundItDatabase? = null

        fun getDatabase(context: Context): FoundItDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoundItDatabase::class.java,
                    "foundit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
