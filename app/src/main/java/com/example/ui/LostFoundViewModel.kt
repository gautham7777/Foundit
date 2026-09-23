package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FoundItDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Claim
import com.example.data.model.Conversation
import com.example.data.model.FoundItem
import com.example.data.model.ItemCategory
import com.example.data.model.ItemStatus
import com.example.data.model.LostWatch
import com.example.data.model.NotificationItem
import com.example.data.model.User
import com.example.data.repository.LostFoundRepository
import com.example.util.LocationHelper
import com.example.util.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SmartMatchResult(
    val watch: LostWatch,
    val item: FoundItem,
    val confidencePercent: Int
)

class LostFoundViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FoundItDatabase.getDatabase(application)
    val repository = LostFoundRepository(db)

    // Current User
    val currentUser: StateFlow<User?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Location State
    private val _currentLocation = MutableStateFlow(LocationHelper.PRESET_LOCATIONS.first())
    val currentLocation: StateFlow<UserLocation> = _currentLocation.asStateFlow()

    private val _searchRadiusKm = MutableStateFlow(5.0)
    val searchRadiusKm: StateFlow<Double> = _searchRadiusKm.asStateFlow()

    // Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
    val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ItemStatus?>(null)
    val selectedStatusFilter: StateFlow<ItemStatus?> = _selectedStatusFilter.asStateFlow()

    private data class FilterCriteria(
        val query: String,
        val category: ItemCategory?,
        val status: ItemStatus?,
        val radiusKm: Double,
        val location: UserLocation
    )

    private val filterCriteriaFlow = combine(
        _searchQuery,
        _selectedCategory,
        _selectedStatusFilter,
        _searchRadiusKm
    ) { query, category, status, radius ->
        object {
            val q = query
            val c = category
            val s = status
            val r = radius
        }
    }.combine(_currentLocation) { base, loc ->
        FilterCriteria(
            query = base.q,
            category = base.c,
            status = base.s,
            radiusKm = base.r,
            location = loc
        )
    }

    // Filtered Items (for Home and Search)
    val displayedItems: StateFlow<List<FoundItem>> = combine(
        repository.allFoundItems,
        filterCriteriaFlow
    ) { items, filters ->
        items.filter { item ->
            // Category check
            val matchesCategory = filters.category == null || item.category.equals(filters.category.name, ignoreCase = true)

            // Status check
            val matchesStatus = filters.status == null || item.status.equals(filters.status.name, ignoreCase = true)

            // Search query matching title, category, description, approxLocation
            val matchesQuery = filters.query.isBlank() ||
                    item.title.contains(filters.query, ignoreCase = true) ||
                    item.description.contains(filters.query, ignoreCase = true) ||
                    item.category.contains(filters.query, ignoreCase = true) ||
                    item.approxLocation.contains(filters.query, ignoreCase = true)

            // Distance filter
            val dist = LocationHelper.calculateDistanceKm(
                filters.location.latitude,
                filters.location.longitude,
                item.latitude,
                item.longitude
            )
            val matchesDistance = dist <= filters.radiusKm

            matchesCategory && matchesStatus && matchesQuery && matchesDistance
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Items with calculated distance
    val allItems: StateFlow<List<FoundItem>> = repository.allFoundItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Conversations
    val conversations: StateFlow<List<Conversation>> = currentUser.combine(repository.allFoundItems) { user, _ ->
        user?.id ?: ""
    }.combine(repository.allFoundItems) { userId, _ ->
        userId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        .let {
            repository.getConversationsForUser("user_rahul") // dynamic fallback
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unread Notifications
    val unreadNotifsCount: StateFlow<Int> = repository.getUnreadNotificationCount("user_rahul")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // User's Lost Watches
    val userWatches: StateFlow<List<LostWatch>> = repository.getLostWatches("user_rahul")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Matches
    val smartMatches: StateFlow<List<SmartMatchResult>> = combine(
        userWatches,
        repository.allFoundItems
    ) { watches, items ->
        val results = mutableListOf<SmartMatchResult>()
        for (watch in watches) {
            for (item in items) {
                if (item.status != ItemStatus.RETURNED.name) {
                    val score = repository.computeMatchConfidence(watch, item)
                    if (score >= 50) {
                        results.add(SmartMatchResult(watch, item, score))
                    }
                }
            }
        }
        results.sortedByDescending { it.confidencePercent }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: ItemCategory?) {
        _selectedCategory.value = category
    }

    fun setSelectedStatusFilter(status: ItemStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setSearchRadius(radiusKm: Double) {
        _searchRadiusKm.value = radiusKm
    }

    fun setUserLocation(location: UserLocation) {
        _currentLocation.value = location
    }

    fun requestGpsLocation(context: Context) {
        LocationHelper.fetchCurrentLocation(context) { loc ->
            _currentLocation.value = loc
        }
    }

    fun postFoundItem(
        title: String,
        description: String,
        category: ItemCategory,
        approxLocation: String,
        latitude: Double,
        longitude: Double,
        dateFound: Long,
        photos: List<String>,
        question: String?,
        answerNote: String?,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.postFoundItem(
                title = title,
                description = description,
                category = category,
                approxLocation = approxLocation,
                latitude = latitude,
                longitude = longitude,
                dateFound = dateFound,
                photoUrls = photos,
                verificationQuestion = question,
                verificationAnswerNote = answerNote
            )
            onSuccess(id)
        }
    }

    fun submitClaim(
        itemId: String,
        answer: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val claimId = repository.submitClaim(itemId, answer)
                onSuccess(claimId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun markItemReturned(itemId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.markItemReturned(itemId)
            onComplete()
        }
    }

    fun deletePost(itemId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteFoundItem(itemId)
            onComplete()
        }
    }

    fun switchUser(userId: String) {
        viewModelScope.launch {
            repository.switchUser(userId)
        }
    }

    fun reportItem(itemId: String, reason: String, details: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            repository.reportTarget(user?.id ?: "anon", "POST", itemId, reason, details)
            onDone()
        }
    }

    fun reportUser(targetUserId: String, reason: String, details: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            repository.reportTarget(user?.id ?: "anon", "USER", targetUserId, reason, details)
            onDone()
        }
    }

    fun blockUser(targetUserId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                repository.blockUser(user.id, targetUserId)
            }
            onDone()
        }
    }

    fun createLostWatch(
        title: String,
        category: ItemCategory,
        description: String,
        approxLocation: String,
        latitude: Double,
        longitude: Double,
        dateLost: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createLostWatch(title, category, description, approxLocation, latitude, longitude, dateLost)
            onDone()
        }
    }

    fun deleteLostWatch(watchId: String) {
        viewModelScope.launch {
            repository.deleteLostWatch(watchId)
        }
    }

    fun getItem(itemId: String): Flow<FoundItem?> = repository.getItem(itemId)
    fun getClaimsForItem(itemId: String): Flow<List<Claim>> = repository.getClaimsForItem(itemId)
    fun getConversation(convId: String): Flow<Conversation?> = repository.getConversation(convId)
    fun getMessages(convId: String): Flow<List<ChatMessage>> = repository.getMessages(convId)
    fun getNotifications(): Flow<List<NotificationItem>> {
        val user = currentUser.value?.id ?: "user_rahul"
        return repository.getNotificationsForUser(user)
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            val user = currentUser.value?.id ?: "user_rahul"
            repository.markNotificationsRead(user)
        }
    }

    fun sendMessage(convId: String, text: String, imageUri: String? = null) {
        viewModelScope.launch {
            val user = currentUser.value
            repository.sendMessage(
                conversationId = convId,
                senderId = user?.id ?: "user_current",
                senderName = user?.name ?: "Me",
                text = text,
                imageUri = imageUri
            )
        }
    }
}
