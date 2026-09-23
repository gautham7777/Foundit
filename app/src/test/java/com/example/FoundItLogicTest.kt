package com.example

import com.example.data.model.FoundItem
import com.example.data.model.ItemCategory
import com.example.data.model.ItemStatus
import com.example.data.model.LostWatch
import com.example.util.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundItLogicTest {

    @Test
    fun distanceCalculation_isAccurate() {
        // Kakkanad (10.0159, 76.3419) to Lulu Mall Edappally (10.0275, 76.3081) is approx 3.9 - 4.0 km
        val distance = LocationHelper.calculateDistanceKm(
            10.0159, 76.3419,
            10.0275, 76.3081
        )
        assertTrue("Distance should be around 3.9km", distance in 3.5..4.5)
        assertTrue("Formatted distance should end in km away", LocationHelper.formatDistance(distance).endsWith("km away"))
    }

    @Test
    fun distanceFormatting_showsMetersWhenUnder1Km() {
        val formatted = LocationHelper.formatDistance(0.45)
        assertEquals("450 m away", formatted)
    }
}
