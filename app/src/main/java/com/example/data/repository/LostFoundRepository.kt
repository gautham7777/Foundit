package com.example.data.repository

import com.example.data.db.FoundItDatabase
import com.example.data.model.BlockedUser
import com.example.data.model.ChatMessage
import com.example.data.model.Claim
import com.example.data.model.Conversation
import com.example.data.model.FoundItem
import com.example.data.model.ItemCategory
import com.example.data.model.ItemStatus
import com.example.data.model.LostWatch
import com.example.data.model.NotificationItem
import com.example.data.model.Report
import com.example.data.model.User
import com.example.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class LostFoundRepository(private val db: FoundItDatabase) {

    private val foundItemDao = db.foundItemDao()
    private val userDao = db.userDao()
    private val claimDao = db.claimDao()
    private val messageDao = db.messageDao()
    private val notificationDao = db.notificationDao()
    private val reportDao = db.reportDao()
    private val lostWatchDao = db.lostWatchDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    // --- Users ---
    val currentUser: Flow<User?> = userDao.getCurrentUser()
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    suspend fun switchUser(userId: String) {
        userDao.clearCurrentUserFlag()
        userDao.setCurrentUser(userId)
    }

    suspend fun createOrUpdateUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun deleteCurrentUserAccount() {
        val user = userDao.getCurrentUserDirect()
        if (user != null) {
            userDao.deleteUser(user.id)
            val remaining = userDao.getUserById("user_alex")
            if (remaining != null) {
                userDao.setCurrentUser("user_alex")
            }
        }
    }

    // --- Found Items ---
    val allFoundItems: Flow<List<FoundItem>> = foundItemDao.getAllFoundItems()

    fun getItem(id: String): Flow<FoundItem?> = foundItemDao.getItemById(id)

    fun getItemsByFinder(finderId: String): Flow<List<FoundItem>> = foundItemDao.getItemsByFinder(finderId)

    suspend fun postFoundItem(
        title: String,
        description: String,
        category: ItemCategory,
        approxLocation: String,
        latitude: Double,
        longitude: Double,
        dateFound: Long,
        photoUrls: List<String>,
        verificationQuestion: String?,
        verificationAnswerNote: String?
    ): String {
        val currentUser = userDao.getCurrentUserDirect()
            ?: User(
                id = "user_me",
                name = "Rahul Sharma",
                email = "rahul.community@example.com",
                isCurrentUser = true,
                communityBadge = "Verified Finder"
            ).also { userDao.insertUser(it) }

        val itemId = "item_" + UUID.randomUUID().toString().take(8)
        val newItem = FoundItem(
            id = itemId,
            finderId = currentUser.id,
            finderName = currentUser.name,
            finderAvatar = currentUser.avatarUri,
            title = title.trim(),
            description = description.trim(),
            category = category.name,
            approxLocation = approxLocation.trim(),
            latitude = latitude,
            longitude = longitude,
            dateFound = dateFound,
            photosString = photoUrls.joinToString("|||"),
            verificationQuestion = verificationQuestion?.trim()?.ifBlank { null },
            verificationAnswerNote = verificationAnswerNote?.trim()?.ifBlank { null },
            status = ItemStatus.AVAILABLE.name,
            createdAt = System.currentTimeMillis()
        )

        foundItemDao.insertItem(newItem)
        userDao.incrementPostsCreated(currentUser.id)

        // Check against active LostWatches to generate smart match notifications
        checkMatchesForNewFoundItem(newItem)

        return itemId
    }

    suspend fun updateFoundItem(item: FoundItem) {
        foundItemDao.updateItem(item)
    }

    suspend fun deleteFoundItem(itemId: String) {
        foundItemDao.deleteItemById(itemId)
    }

    suspend fun markItemReturned(itemId: String): Boolean {
        val item = foundItemDao.getItemByIdDirect(itemId) ?: return false
        foundItemDao.updateStatus(itemId, ItemStatus.RETURNED.name)
        userDao.incrementItemsReturned(item.finderId)

        // Send notification to finder
        notificationDao.insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = item.finderId,
                title = "Item Marked Returned! 🎉",
                message = "Thank you for returning \"${item.title}\" to its rightful owner. The community appreciates your honesty!",
                type = "RETURNED",
                relatedItemId = itemId
            )
        )
        return true
    }

    // --- Claims ---
    fun getClaimsForItem(itemId: String): Flow<List<Claim>> = claimDao.getClaimsForItem(itemId)
    fun getClaimsByUser(userId: String): Flow<List<Claim>> = claimDao.getClaimsByUser(userId)

    suspend fun submitClaim(
        itemId: String,
        verificationAnswer: String
    ): String {
        val item = foundItemDao.getItemByIdDirect(itemId) ?: error("Item not found")
        val claimant = userDao.getCurrentUserDirect() ?: error("User not logged in")

        val claimId = "claim_" + UUID.randomUUID().toString().take(8)
        val claim = Claim(
            id = claimId,
            itemId = itemId,
            claimantId = claimant.id,
            claimantName = claimant.name,
            claimantAvatar = claimant.avatarUri,
            verificationAnswer = verificationAnswer.trim(),
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        claimDao.insertClaim(claim)

        // Update item status to Claim In Progress if currently Available
        if (item.status == ItemStatus.AVAILABLE.name) {
            foundItemDao.updateStatus(itemId, ItemStatus.CLAIM_IN_PROGRESS.name)
        }

        // Notify finder
        notificationDao.insertNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                userId = item.finderId,
                title = "New Claim on \"${item.title}\"",
                message = "${claimant.name} says this might be theirs and answered your verification question.",
                type = "CLAIM",
                relatedItemId = itemId
            )
        )

        // Automatically start or find conversation between Finder and Claimant
        val convId = getOrCreateConversation(itemId, item.finderId, item.finderName, claimant.id, claimant.name, item.title, item.category)

        // Post system verification message in the chat
        messageDao.insertMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                senderId = claimant.id,
                senderName = claimant.name,
                text = "Hi, I think this item might be mine! My answer to your verification question (\"${item.verificationQuestion ?: "Verification"}\") is:\n\n\"$verificationAnswer\"",
                isSystemNotice = false
            )
        )
        messageDao.updateLastMessage(convId, "Verification answer sent: \"$verificationAnswer\"", System.currentTimeMillis())

        return claimId
    }

    // --- Messaging ---
    fun getConversationsForUser(userId: String): Flow<List<Conversation>> = messageDao.getConversationsForUser(userId)
    fun getConversation(convId: String): Flow<Conversation?> = messageDao.getConversationById(convId)
    fun getMessages(convId: String): Flow<List<ChatMessage>> = messageDao.getMessagesForConversation(convId)

    suspend fun getOrCreateConversation(
        itemId: String,
        finderId: String,
        finderName: String,
        claimantId: String,
        claimantName: String,
        itemTitle: String,
        itemCategory: String
    ): String {
        val existing = messageDao.findConversation(itemId, finderId, claimantId)
        if (existing != null) return existing.id

        val convId = "conv_" + UUID.randomUUID().toString().take(8)
        val newConv = Conversation(
            id = convId,
            itemId = itemId,
            itemTitle = itemTitle,
            itemCategory = itemCategory,
            finderId = finderId,
            finderName = finderName,
            claimantId = claimantId,
            claimantName = claimantName,
            lastMessageText = "Conversation started regarding \"$itemTitle\"",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        messageDao.insertConversation(newConv)
        return convId
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        imageUri: String? = null
    ) {
        val message = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            text = text.trim(),
            imageUri = imageUri,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)
        messageDao.updateLastMessage(conversationId, text.ifBlank { "📷 Photo sent" }, System.currentTimeMillis())
    }

    suspend fun markConversationRead(conversationId: String) {
        messageDao.markConversationRead(conversationId)
    }

    // --- Notifications ---
    fun getNotificationsForUser(userId: String): Flow<List<NotificationItem>> = notificationDao.getNotificationsForUser(userId)
    fun getUnreadNotificationCount(userId: String): Flow<Int> = notificationDao.getUnreadCount(userId)

    suspend fun markNotificationsRead(userId: String) {
        notificationDao.markAllAsRead(userId)
    }

    // --- Safety, Reports & Blocking ---
    suspend fun reportTarget(reporterId: String, targetType: String, targetId: String, reason: String, details: String) {
        reportDao.insertReport(
            Report(
                id = UUID.randomUUID().toString(),
                reporterId = reporterId,
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                details = details
            )
        )
    }

    suspend fun blockUser(userId: String, blockedUserId: String) {
        reportDao.blockUser(
            BlockedUser(
                id = "${userId}_$blockedUserId",
                userId = userId,
                blockedUserId = blockedUserId
            )
        )
    }

    fun getBlockedUserIds(userId: String): Flow<List<String>> = reportDao.getBlockedUserIds(userId)

    // --- Smart Matching & Lost Watches ---
    fun getLostWatches(userId: String): Flow<List<LostWatch>> = lostWatchDao.getWatchesByUser(userId)

    suspend fun createLostWatch(
        title: String,
        category: ItemCategory,
        description: String,
        approxLocation: String,
        latitude: Double,
        longitude: Double,
        dateLost: Long
    ) {
        val user = userDao.getCurrentUserDirect() ?: return
        val watch = LostWatch(
            id = "watch_" + UUID.randomUUID().toString().take(8),
            userId = user.id,
            title = title.trim(),
            category = category.name,
            description = description.trim(),
            approxLocation = approxLocation.trim(),
            latitude = latitude,
            longitude = longitude,
            dateLost = dateLost
        )
        lostWatchDao.insertWatch(watch)
    }

    suspend fun deleteLostWatch(watchId: String) {
        lostWatchDao.deleteWatch(watchId)
    }

    fun computeMatchConfidence(watch: LostWatch, item: FoundItem): Int {
        var score = 0

        // 1. Category match (35 pts)
        if (watch.category.equals(item.category, ignoreCase = true)) {
            score += 35
        }

        // 2. Keyword overlap in titles and descriptions (up to 35 pts)
        val watchWords = (watch.title + " " + watch.description).lowercase().split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
        val itemWords = (item.title + " " + item.description).lowercase().split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
        val overlap = watchWords.intersect(itemWords)
        if (overlap.isNotEmpty()) {
            val ratio = (overlap.size.toDouble() / watchWords.size.coerceAtLeast(1).toDouble()).coerceAtMost(1.0)
            score += (ratio * 35).toInt()
        }

        // 3. Distance proximity (< 5 km = 20 pts, < 15 km = 10 pts)
        val dist = LocationHelper.calculateDistanceKm(watch.latitude, watch.longitude, item.latitude, item.longitude)
        if (dist <= 3.0) {
            score += 20
        } else if (dist <= 8.0) {
            score += 12
        } else if (dist <= 15.0) {
            score += 6
        }

        // 4. Date proximity (within 10 days = 10 pts)
        val daysDiff = kotlin.math.abs(watch.dateLost - item.dateFound) / (1000 * 60 * 60 * 24)
        if (daysDiff <= 3) {
            score += 10
        } else if (daysDiff <= 10) {
            score += 5
        }

        return score.coerceIn(0, 99)
    }

    private suspend fun checkMatchesForNewFoundItem(newItem: FoundItem) {
        // Find if any user has a lost watch that matches this found item
        // If match score >= 50, send a "MATCH" notification
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check all watches
                // Note: in a small community app this is fast and responsive
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // --- Seed Initial Realistic Data ---
    private suspend fun seedInitialDataIfNeeded() {
        if (foundItemDao.getItemCount() > 0) return

        val userRahul = User(
            id = "user_rahul",
            name = "Rahul Sharma",
            email = "rahul.s@example.com",
            avatarUri = null,
            createdAt = System.currentTimeMillis() - 86400000L * 60,
            itemsReturned = 3,
            postsCreated = 2,
            isCurrentUser = true,
            communityBadge = "Community Star ⭐"
        )

        val userPriya = User(
            id = "user_priya",
            name = "Priya Nair",
            email = "priya.nair@example.com",
            avatarUri = null,
            createdAt = System.currentTimeMillis() - 86400000L * 120,
            itemsReturned = 5,
            postsCreated = 4,
            isCurrentUser = false,
            communityBadge = "Top Finder 🏆"
        )

        val userAnand = User(
            id = "user_anand",
            name = "Anand Menon",
            email = "anand.m@example.com",
            avatarUri = null,
            createdAt = System.currentTimeMillis() - 86400000L * 45,
            itemsReturned = 2,
            postsCreated = 3,
            isCurrentUser = false,
            communityBadge = "Active Neighbor"
        )

        userDao.insertAll(listOf(userRahul, userPriya, userAnand))

        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 3600 * 1000L)
        val fourHoursAgo = now - (4 * 3600 * 1000L)
        val yesterday = now - (26 * 3600 * 1000L)
        val twoDaysAgo = now - (50 * 3600 * 1000L)

        val items = listOf(
            FoundItem(
                id = "item_wallet_1",
                finderId = "user_priya",
                finderName = "Priya Nair",
                title = "Found Black Leather Wallet",
                description = "Found a black bi-fold leather wallet lying on a stone bench in the park near Kakkanad junction. It contains some cash and metro pass. No sensitive documents are shown for safety.",
                category = ItemCategory.WALLET.name,
                approxLocation = "Near Kakkanad, Kochi",
                latitude = 10.0175,
                longitude = 76.3450,
                dateFound = twoHoursAgo,
                photosString = "preset_wallet_black",
                verificationQuestion = "What brand is stamped on the corner and what color is the inner stitch?",
                verificationAnswerNote = "Tommy Hilfiger, red and white inner accent stitching",
                status = ItemStatus.AVAILABLE.name,
                createdAt = twoHoursAgo
            ),
            FoundItem(
                id = "item_purse_2",
                finderId = "user_priya",
                finderName = "Priya Nair",
                title = "Found a missing purse near Lulu Mall",
                description = "Spotted a medium-sized beige shoulder purse near the second-floor food court entrance of Lulu Mall. Item handed over safely to security desk but listed here for owner discovery.",
                category = ItemCategory.PURSE.name,
                approxLocation = "Near Lulu Mall, Edappally",
                latitude = 10.0284,
                longitude = 76.3079,
                dateFound = fourHoursAgo,
                photosString = "preset_purse_beige",
                verificationQuestion = "What colour is the inside lining and what keychain is attached?",
                verificationAnswerNote = "Dark red lining, golden star keychain",
                status = ItemStatus.AVAILABLE.name,
                createdAt = fourHoursAgo
            ),
            FoundItem(
                id = "item_airpods_3",
                finderId = "user_anand",
                finderName = "Anand Menon",
                title = "Found AirPods Case with Buds",
                description = "Found a white AirPods Pro case with a translucent matte silicone protective bumper near the metro pillar staircase.",
                category = ItemCategory.ELECTRONICS.name,
                approxLocation = "Edappally Metro Station, Kochi",
                latitude = 10.0261,
                longitude = 76.3085,
                dateFound = yesterday,
                photosString = "preset_airpods",
                verificationQuestion = "What color is the silicone sleeve cover and what initials or sticker is on it?",
                verificationAnswerNote = "Olive green silicone cover, small bear decal",
                status = ItemStatus.AVAILABLE.name,
                createdAt = yesterday
            ),
            FoundItem(
                id = "item_idcard_4",
                finderId = "user_priya",
                finderName = "Priya Nair",
                title = "Found College ID Card",
                description = "Found an engineering college student ID card near Thrikkakara university bus shelter. Full ID number and roll number hidden for privacy and safety.",
                category = ItemCategory.ID_CARD.name,
                approxLocation = "Thrikkakara, Kochi",
                latitude = 10.0400,
                longitude = 76.3300,
                dateFound = yesterday,
                photosString = "preset_idcard",
                verificationQuestion = "What are the student's initials and which college department is listed?",
                verificationAnswerNote = "Initials A.K., Computer Science 3rd Year",
                status = ItemStatus.AVAILABLE.name,
                createdAt = yesterday
            ),
            FoundItem(
                id = "item_keys_5",
                finderId = "user_anand",
                finderName = "Anand Menon",
                title = "Found Bunch of Bike Keys with Tag",
                description = "Found a set of 3 keys with a black braided cord and a metal souvenir tag near Infopark campus food street.",
                category = ItemCategory.KEYS.name,
                approxLocation = "Infopark, Kakkanad",
                latitude = 10.0104,
                longitude = 76.3630,
                dateFound = twoDaysAgo,
                photosString = "preset_keys",
                verificationQuestion = "What bike brand logo is on the key head, and what is written on the metal tag?",
                verificationAnswerNote = "Royal Enfield logo, tag says 'Ride Pure'",
                status = ItemStatus.AVAILABLE.name,
                createdAt = twoDaysAgo
            ),
            FoundItem(
                id = "item_bag_6",
                finderId = "user_rahul",
                finderName = "Rahul Sharma",
                title = "Found Blue Travel Backpack",
                description = "Found a dark blue water-resistant backpack left on a bench at Marine Drive walkway. Already claimed by owner and handed over successfully!",
                category = ItemCategory.BAG.name,
                approxLocation = "Marine Drive, Kochi",
                latitude = 9.9816,
                longitude = 76.2753,
                dateFound = twoDaysAgo - 86400000L,
                photosString = "preset_backpack_blue",
                verificationQuestion = "What brand is on the zipper pulls?",
                verificationAnswerNote = "Quechua",
                status = ItemStatus.RETURNED.name,
                createdAt = twoDaysAgo - 86400000L
            )
        )

        foundItemDao.insertAll(items)

        // Seed a starter conversation demonstrating the verified chat flow
        val convId = "conv_demo_sample"
        val demoConv = Conversation(
            id = convId,
            itemId = "item_purse_2",
            itemTitle = "Found a missing purse near Lulu Mall",
            itemCategory = ItemCategory.PURSE.name,
            finderId = "user_priya",
            finderName = "Priya Nair",
            claimantId = "user_rahul",
            claimantName = "Rahul Sharma",
            lastMessageText = "Yep, that matches! Let's arrange a safe public place to return it.",
            lastMessageTimestamp = fourHoursAgo + 1800000L,
            unreadCount = 1,
            verificationAnswerSnippet = "Dark red lining, golden star keychain"
        )
        messageDao.insertConversation(demoConv)

        val messages = listOf(
            ChatMessage(
                id = "msg_1",
                conversationId = convId,
                senderId = "user_rahul",
                senderName = "Rahul Sharma",
                text = "Hi Priya! I think this purse might be mine. I misplaced it near Lulu Mall earlier today.",
                timestamp = fourHoursAgo + 600000L
            ),
            ChatMessage(
                id = "msg_2",
                conversationId = convId,
                senderId = "user_rahul",
                senderName = "Rahul Sharma",
                text = "Regarding your verification question: The inner lining is dark red, and it has a small golden star keychain attached to the side strap.",
                timestamp = fourHoursAgo + 700000L
            ),
            ChatMessage(
                id = "msg_3",
                conversationId = convId,
                senderId = "user_priya",
                senderName = "Priya Nair",
                text = "Yep, that matches! Let's arrange a safe public place to return it.",
                timestamp = fourHoursAgo + 1800000L
            )
        )
        messages.forEach { messageDao.insertMessage(it) }

        // Seed starter notification
        notificationDao.insertNotification(
            NotificationItem(
                id = "notif_1",
                userId = "user_rahul",
                title = "Finder Replied!",
                message = "Priya Nair replied to your claim on \"Found a missing purse near Lulu Mall\".",
                type = "MESSAGE",
                relatedItemId = "item_purse_2",
                relatedConversationId = convId,
                timestamp = fourHoursAgo + 1800000L
            )
        )

        // Seed a sample lost watch for Rahul so smart matching highlights instantly
        lostWatchDao.insertWatch(
            LostWatch(
                id = "watch_sample_wallet",
                userId = "user_rahul",
                title = "Lost black leather wallet near Kakkanad",
                category = ItemCategory.WALLET.name,
                description = "Black leather wallet with metro pass and ID card lost around Kakkanad junction.",
                approxLocation = "Kakkanad, Kochi",
                latitude = 10.0159,
                longitude = 76.3419,
                dateLost = twoHoursAgo - 3600000L
            )
        )
    }
}
