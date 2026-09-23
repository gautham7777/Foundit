package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.LocationHelper
import com.example.util.UserLocation

@Composable
fun RadiusSelector(
    currentRadiusKm: Double,
    currentLocation: UserLocation,
    onRadiusSelected: (Double) -> Unit,
    onLocationChanged: (UserLocation) -> Unit,
    onRequestGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLocationDialog by remember { mutableStateOf(false) }
    val radii = listOf(1.0, 3.0, 5.0, 10.0, 25.0, 50.0)

    Column(modifier = modifier.fillMaxWidth()) {
        // Location row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Current Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentLocation.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            AssistChip(
                onClick = { showLocationDialog = true },
                label = { Text("Change") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.EditLocation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("change_location_button")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Radius chips row
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Radius:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            radii.forEach { radius ->
                val isSelected = currentRadiusKm == radius
                val label = if (radius >= 1.0) "${radius.toInt()} km" else "${(radius * 1000).toInt()} m"
                FilterChip(
                    selected = isSelected,
                    onClick = { onRadiusSelected(radius) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("radius_chip_${radius.toInt()}km")
                )
            }
        }
    }

    if (showLocationDialog) {
        LocationSelectionDialog(
            currentLocation = currentLocation,
            onDismiss = { showLocationDialog = false },
            onSelectLocation = {
                onLocationChanged(it)
                showLocationDialog = false
            },
            onUseGps = {
                onRequestGps()
                showLocationDialog = false
            }
        )
    }
}

@Composable
fun LocationSelectionDialog(
    currentLocation: UserLocation,
    onDismiss: () -> Unit,
    onSelectLocation: (UserLocation) -> Unit,
    onUseGps: () -> Unit
) {
    var customLocationName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Search Location") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Items are discovered based on proximity to your area. Choose a neighborhood or enter a custom location:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Popular Areas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LocationHelper.PRESET_LOCATIONS.forEach { loc ->
                    TextButton(
                        onClick = { onSelectLocation(loc) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = loc.name,
                                fontWeight = if (loc.name == currentLocation.name) FontWeight.Bold else FontWeight.Normal,
                                color = if (loc.name == currentLocation.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customLocationName,
                    onValueChange = { customLocationName = it },
                    label = { Text("Or manual location name") },
                    placeholder = { Text("e.g. MG Road, Kochi") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_location_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (customLocationName.isNotBlank()) {
                        // approximate center for custom query
                        onSelectLocation(UserLocation(10.0159, 76.3419, customLocationName.trim()))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
