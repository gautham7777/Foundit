package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = now - timestamp
        val diffMinutes = diffMillis / (1000 * 60)
        val diffHours = diffMillis / (1000 * 60 * 60)
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)

        val calNow = Calendar.getInstance()
        val calItem = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            calNow.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR) &&
                    calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR) -> "Found Today"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays == 1L -> "Found Yesterday"
            diffDays < 7 -> "Found ${diffDays} days ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
                sdf.format(Date(timestamp))
            }
        }
    }

    fun formatDetailedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatShortTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
