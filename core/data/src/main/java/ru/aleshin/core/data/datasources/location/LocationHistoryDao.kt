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
package ru.aleshin.core.data.datasources.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.aleshin.core.data.models.location.LocationHistoryEntity

@Dao
interface LocationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LocationHistoryEntity)

    /** All entries for a date range, ordered oldest-first (for timeline display) */
    @Query("SELECT * FROM locationHistory WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun getRange(from: Long, to: Long): List<LocationHistoryEntity>

    /** Last N entries for the status pill / quick summary */
    @Query("SELECT * FROM locationHistory ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int = 50): List<LocationHistoryEntity>

    /** Delete entries older than a given timestamp (keep 30 days max) */
    @Query("DELETE FROM locationHistory WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    /** Most recent single entry */
    @Query("SELECT * FROM locationHistory ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEntry(): LocationHistoryEntity?
}
