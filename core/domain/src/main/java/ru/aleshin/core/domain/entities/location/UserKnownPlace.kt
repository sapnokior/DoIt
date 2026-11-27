/*
 * Copyright 2026 Stanislav Aleshin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.aleshin.core.domain.entities.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Known user locations with matching emoji badges.
 */
enum class UserKnownPlace(
    val title: String,
    val emoji: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = 350.0,
) {
    HOME(
        title = "Home",
        emoji = "🏠",
        latitude = 22.9867962,
        longitude = 72.6137236,
        radiusMeters = 600.0,
    ),
    WORK(
        title = "Work",
        emoji = "💼",
        latitude = 22.9666259,
        longitude = 72.5797171,
        radiusMeters = 500.0,
    ),
    HOSPITAL(
        title = "Hospital",
        emoji = "🏥",
        latitude = 23.0834116,
        longitude = 72.5249355,
        radiusMeters = 500.0,
    ),
    SERVICE_CENTRE(
        title = "Service Centre",
        emoji = "🧑‍🔧",
        latitude = 22.9916668,
        longitude = 72.6244202,
        radiusMeters = 450.0,
    ),
    UNKNOWN(
        title = "On the Move",
        emoji = "📍",
        latitude = 0.0,
        longitude = 0.0,
        radiusMeters = 0.0,
    );

    companion object {
        fun findMatchingPlace(latitude: Double, longitude: Double): UserKnownPlace {
            for (place in entries) {
                if (place == UNKNOWN) continue
                val distance = calculateDistanceMeters(latitude, longitude, place.latitude, place.longitude)
                if (distance <= place.radiusMeters) {
                    return place
                }
            }
            return UNKNOWN
        }

        /**
         * Haversine formula for calculating distance in meters between two lat/lng coordinates.
         * 100% on-device, zero API keys, zero network overhead.
         */
        fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}

/**
 * On-device detected movement activity (Sitting/Stationary vs Driving).
 */
enum class UserMovementActivity(
    val title: String,
    val emoji: String,
) {
    STATIONARY(title = "At Location", emoji = "🪑"),
    DRIVING(title = "Driving", emoji = "🏍️"),
    UNKNOWN(title = "Active", emoji = "⚡");
}

data class CurrentContextState(
    val place: UserKnownPlace = UserKnownPlace.HOME,
    val activity: UserMovementActivity = UserMovementActivity.STATIONARY,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
)
