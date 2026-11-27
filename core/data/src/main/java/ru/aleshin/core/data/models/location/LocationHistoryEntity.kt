/*
 * Copyright 2026 Adarsh Kumar Singh
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
package ru.aleshin.core.data.models.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores a 10-minute location ping, like Google Timeline.
 * Each row = one snapshot of where Adarsh was at a given moment.
 */
@Entity(
    tableName = "locationHistory",
    indices = [
        Index(value = ["timestamp"]),
    ],
)
data class LocationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    /** Epoch milliseconds when this ping was recorded */
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    /** Latitude, e.g. 22.9868 */
    @ColumnInfo(name = "latitude") val latitude: Double,
    /** Longitude, e.g. 72.6137 */
    @ColumnInfo(name = "longitude") val longitude: Double,
    /** Matched known place name, e.g. "Home", "Work", or null if in-transit */
    @ColumnInfo(name = "place_name") val placeName: String?,
    /** Emoji for the place: 🏠 🏍️ 💼 🏥 🧑‍🔧 */
    @ColumnInfo(name = "place_emoji") val placeEmoji: String?,
    /** Accuracy in metres from GPS */
    @ColumnInfo(name = "accuracy_meters") val accuracyMeters: Float?,
)
