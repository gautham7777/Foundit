package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FoundItem
import com.example.data.model.ItemCategory
import com.example.data.model.ItemStatus
import com.example.ui.LostFoundViewModel
import com.example.ui.components.ClaimDialog
import com.example.ui.components.ItemVisual
import com.example.ui.components.ReportDialog
import com.example.ui.components.ReturnedCelebrationDialog
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnEmeraldContainer
import com.example.util.DateUtils
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    itemId: String,
    viewModel: LostFoundViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val itemFlow = remember(itemId) { viewModel.getItem(itemId) }
    val item by itemFlow.collectAsStateWithLifecycle(initialValue = null)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()

    var showClaimDialog by remember { mutableStateOf(false) }
    var showReturnedDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (item == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Item Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
                Text("Item not found or has been removed.")
            }
        }
        return
    }

    val foundItem = item!!
    val isFinder = currentUser?.id == foundItem.finderId
    val distanceKm = LocationHelper.calculateDistanceKm(
        currentLocation.latitude,
        currentLocation.longitude,
        foundItem.latitude,
        foundItem.longitude
    )
    val distanceFormatted = LocationHelper.formatDistance(distanceKm)
    val formattedDate = DateUtils.formatDetailedDate(foundItem.dateFound)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Found Item Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report Listing") },
                            leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            }
                        )
                        if (!isFinder) {
                            DropdownMenuItem(
                                text = { Text("Block Finder") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.blockUser(foundItem.finderId) {
                                        Toast.makeText(context, "Finder blocked", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            )
                        }
                        if (isFinder) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = ErrorRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deletePost(foundItem.id) {
                                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isFinder) {
                        // Finder controls
                        if (foundItem.itemStatus != ItemStatus.RETURNED) {
                            Button(
                                onClick = { showReturnedDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mark_returned_button")
                            ) {
                                Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark Item Returned ✅", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Returned to Owner", fontWeight = FontWeight.Bold, color = OnEmeraldContainer)
                                }
                            }
                        }
                    } else {
                        // Claimant / Finder actions
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val convId = viewModel.repository.getOrCreateConversation(
                                        itemId = foundItem.id,
                                        finderId = foundItem.finderId,
                                        finderName = foundItem.finderName,
                                        claimantId = currentUser?.id ?: "user_me",
                                        claimantName = currentUser?.name ?: "Neighbor",
                                        itemTitle = foundItem.title,
                                        itemCategory = foundItem.category
                                    )
                                    onNavigateToChat(convId)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_finder_button")
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message Finder")
                        }

                        if (foundItem.itemStatus != ItemStatus.RETURNED) {
                            Button(
                                onClick = { showClaimDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("this_might_be_mine_button")
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("This Might Be Mine")
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Main visual photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                ItemVisual(
                    photoKeyOrUri = foundItem.photoUrls.firstOrNull(),
                    category = foundItem.itemCategory,
                    modifier = Modifier.fillMaxSize()
                )

                // Status Badge
                val (statusBg, statusFg, statusText) = when (foundItem.itemStatus) {
                    ItemStatus.AVAILABLE -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Available")
                    ItemStatus.CLAIM_IN_PROGRESS -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Claim in Progress")
                    ItemStatus.RETURNED -> Triple(EmeraldContainer, OnEmeraldContainer, "Returned ✅")
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Sensitive Information Privacy Warning for ID Cards and Documents
            if (foundItem.itemCategory == ItemCategory.ID_CARD || foundItem.itemCategory == ItemCategory.DOCUMENTS) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sensitive data is strictly protected. Exact identity numbers and full address are hidden for safety.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Details Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Category Tag
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = foundItem.itemCategory.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = foundItem.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Location info card with privacy disclosure
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = foundItem.approxLocation,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• $distanceFormatted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Approximate neighborhood area. Exact meetup spot is arranged in private chat.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Found
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Found on $formattedDate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = foundItem.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                // Verification Question Notice
                if (!foundItem.verificationQuestion.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Finder's Verification Question",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${foundItem.verificationQuestion}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'This Might Be Mine' below to answer this question and verify ownership.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Finder Profile Section
                Text(
                    text = "Found & Listed By",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = foundItem.finderName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFinder) "You (Current Profile)" else "Active Community Finder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Claim Dialog
    if (showClaimDialog) {
        ClaimDialog(
            item = foundItem,
            onDismiss = { showClaimDialog = false },
            onSubmitClaim = { answer ->
                viewModel.submitClaim(foundItem.id, answer) { claimId ->
                    showClaimDialog = false
                    Toast.makeText(context, "Claim submitted! Opening chat with finder...", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        val convId = viewModel.repository.getOrCreateConversation(
                            itemId = foundItem.id,
                            finderId = foundItem.finderId,
                            finderName = foundItem.finderName,
                            claimantId = currentUser?.id ?: "user_me",
                            claimantName = currentUser?.name ?: "Neighbor",
                            itemTitle = foundItem.title,
                            itemCategory = foundItem.category
                        )
                        onNavigateToChat(convId)
                    }
                }
            }
        )
    }

    // Returned Confirmation Dialog
    if (showReturnedDialog) {
        ReturnedCelebrationDialog(
            itemTitle = foundItem.title,
            onDismiss = { showReturnedDialog = false },
            onConfirmReturned = {
                viewModel.markItemReturned(foundItem.id) {
                    showReturnedDialog = false
                    Toast.makeText(context, "Item marked as returned! 🎉", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Report Dialog
    if (showReportDialog) {
        ReportDialog(
            targetTitle = foundItem.title,
            isUserReport = false,
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason, details ->
                viewModel.reportItem(foundItem.id, reason, details) {
                    showReportDialog = false
                    Toast.makeText(context, "Report submitted. Thank you for keeping the community safe.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
