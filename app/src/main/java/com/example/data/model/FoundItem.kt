package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "found_items")
data class FoundItem(
    @PrimaryKey val id: String,
    val finderId: String,
    val finderName: String,
    val finderAvatar: String? = null,
    val title: String,
    val description: String,
    val category: String, // from ItemCategory
    val approxLocation: String, // e.g. "Kakkanad, Kochi"
    val latitude: Double,
    val longitude: Double,
    val dateFound: Long,
    val photosString: String = "", // separated by |||
    val verificationQuestion: String? = null,
    val verificationAnswerNote: String? = null, // Finder's private note, never exposed to public
    val status: String = ItemStatus.AVAILABLE.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    val photoUrls: List<String>
        get() = if (photosString.isBlank()) emptyList() else photosString.split("|||").filter { it.isNotBlank() }

    val itemStatus: ItemStatus
        get() = ItemStatus.fromString(status)

    val itemCategory: ItemCategory
        get() = ItemCategory.fromString(category)
}
