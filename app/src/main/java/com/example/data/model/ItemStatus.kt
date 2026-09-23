package com.example.data.model

enum class ItemStatus(val label: String) {
    AVAILABLE("Available"),
    CLAIM_IN_PROGRESS("Claim in Progress"),
    RETURNED("Returned ✅");

    companion object {
        fun fromString(status: String?): ItemStatus {
            return entries.firstOrNull { it.name.equals(status, ignoreCase = true) } ?: AVAILABLE
        }
    }
}
