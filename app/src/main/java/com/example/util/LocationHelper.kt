package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class UserLocation(
    val latitude: Double = 10.0159, // Default Kakkanad, Kochi
    val longitude: Double = 76.3419,
    val name: String = "Kakkanad, Kochi"
)

object LocationHelper {
    val PRESET_LOCATIONS = listOf(
        UserLocation(10.0159, 76.3419, "Kakkanad, Kochi"),
        UserLocation(10.0261, 76.3085, "Edappally, Kochi"),
        UserLocation(10.0400, 76.3300, "Thrikkakara, Kochi"),
        UserLocation(10.0284, 76.3079, "Lulu Mall, Kochi"),
        UserLocation(9.9816, 76.2753, "Marine Drive, Kochi"),
        UserLocation(9.9658, 76.2421, "Fort Kochi"),
        UserLocation(10.0104, 76.3630, "Infopark, Kakkanad")
    )

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 0.1) {
            "Nearby (<100m)"
        } else if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt()} m away"
        } else {
            String.format(Locale.US, "%.1f km away", distanceKm)
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(context: Context, onLocationResult: (UserLocation) -> Unit) {
        if (!hasLocationPermission(context)) {
            onLocationResult(PRESET_LOCATIONS.first())
            return
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        onLocationResult(
                            UserLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                name = "Current Device Location"
                            )
                        )
                    } else {
                        onLocationResult(PRESET_LOCATIONS.first())
                    }
                }
                .addOnFailureListener {
                    onLocationResult(PRESET_LOCATIONS.first())
                }
        } catch (e: Exception) {
            onLocationResult(PRESET_LOCATIONS.first())
        }
    }
}
