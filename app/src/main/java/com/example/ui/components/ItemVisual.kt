package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ItemCategory

@Composable
fun ItemVisual(
    photoKeyOrUri: String?,
    category: ItemCategory,
    modifier: Modifier = Modifier
) {
    if (!photoKeyOrUri.isNullOrBlank() && (photoKeyOrUri.startsWith("http") || photoKeyOrUri.startsWith("content://") || photoKeyOrUri.startsWith("file://"))) {
        AsyncImage(
            model = photoKeyOrUri,
            contentDescription = category.displayName,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // High quality artistic category badge with distinct palettes
        val (gradient, icon) = getCategoryVisuals(category, photoKeyOrUri)
        Box(
            modifier = modifier.background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category.displayName,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

private fun getCategoryVisuals(category: ItemCategory, photoKey: String?): Pair<Brush, ImageVector> {
    return when (category) {
        ItemCategory.WALLET -> Brush.linearGradient(
            listOf(Color(0xFF27272A), Color(0xFF18181B), Color(0xFF09090B))
        ) to Icons.Default.AccountBalanceWallet

        ItemCategory.PURSE -> Brush.linearGradient(
            listOf(Color(0xFFBE185D), Color(0xFF9D174D), Color(0xFF831843))
        ) to Icons.Default.ShoppingBag

        ItemCategory.PHONE -> Brush.linearGradient(
            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E40AF))
        ) to Icons.Default.PhoneAndroid

        ItemCategory.KEYS -> Brush.linearGradient(
            listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF92400E))
        ) to Icons.Default.Key

        ItemCategory.ID_CARD -> Brush.linearGradient(
            listOf(Color(0xFF0D9488), Color(0xFF0F766E), Color(0xFF115E59))
        ) to Icons.Default.Badge

        ItemCategory.BAG -> Brush.linearGradient(
            listOf(Color(0xFF4338CA), Color(0xFF3730A3), Color(0xFF312E81))
        ) to Icons.Default.LocalMall

        ItemCategory.ELECTRONICS -> Brush.linearGradient(
            listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF075985))
        ) to Icons.Default.Headphones

        ItemCategory.DOCUMENTS -> Brush.linearGradient(
            listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B))
        ) to Icons.Default.Description

        ItemCategory.OTHER -> Brush.linearGradient(
            listOf(Color(0xFF64748B), Color(0xFF475569), Color(0xFF334155))
        ) to Icons.Default.QuestionMark
    }
}
