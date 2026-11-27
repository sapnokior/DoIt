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
package ru.aleshin.timeplanner.presentation.work

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.aleshin.core.data.datasources.schedules.SchedulesDataBase
import ru.aleshin.core.data.models.location.LocationHistoryEntity
import ru.aleshin.core.domain.entities.location.UserKnownPlace
import ru.aleshin.core.domain.entities.location.UserLocationStateManager
import java.util.concurrent.TimeUnit

/**
 * Runs every 15 minutes (Android WorkManager minimum interval).
 * On each run:
 *   1. Reads best available location from GPS/Network/Passive.
 *   2. Updates the in-memory UserLocationStateManager state (shown in the pill).
 *   3. Inserts a LocationHistoryEntity row into Room — Adarsh's personal 10-min GPS timeline.
 *   4. Prunes entries older than 30 days.
 */
class LocationTrackerWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val hasFine = ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return Result.success()

        val locationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return Result.success()

        val lastGps = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) { null }
        val lastNetwork = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) { null }
        val lastPassive = try {
            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) { null }

        val bestLocation = listOfNotNull(lastGps, lastNetwork, lastPassive)
            .maxByOrNull { it.time }

        if (bestLocation != null) {
            // 1. Update live status pill
            UserLocationStateManager.updateFromLocation(appContext, bestLocation)

            // 2. Determine matched place for history label
            val (placeName, placeEmoji) = resolvePlace(bestLocation)

            // 3. Log to Room history table
            try {
                val db = SchedulesDataBase.create(appContext)
                val dao = db.fetchLocationHistoryDao()
                dao.insert(
                    LocationHistoryEntity(
                        timestamp = System.currentTimeMillis(),
                        latitude = bestLocation.latitude,
                        longitude = bestLocation.longitude,
                        placeName = placeName,
                        placeEmoji = placeEmoji,
                        accuracyMeters = if (bestLocation.hasAccuracy()) bestLocation.accuracy else null,
                    ),
                )
                // 4. Prune >30 days old
                val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                dao.deleteOlderThan(thirtyDaysAgo)
            } catch (e: Exception) {
                android.util.Log.e("LocationTrackerWorker", "DB insert failed", e)
            }
        }

        return Result.success()
    }

    /** Matches the given GPS fix to one of Adarsh's known places (Haversine ≤ radius). */
    private fun resolvePlace(loc: Location): Pair<String?, String?> {
        val matched = UserKnownPlace.findMatchingPlace(loc.latitude, loc.longitude)
        return if (matched != UserKnownPlace.UNKNOWN) {
            matched.title to matched.emoji
        } else {
            null to "🏍️" // in-transit / unknown
        }
    }

    companion object {
        private const val WORK_NAME = "DoItPeriodicLocationWorker"

        fun schedulePeriodic(context: Context) {
            // 15 min is WorkManager's hard floor; actual GPS pings feel like ~10 min in practice
            val request = PeriodicWorkRequestBuilder<LocationTrackerWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun runOnce(context: Context) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val lastGps = try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (e: SecurityException) { null }
            val lastNetwork = try {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) { null }
            val best = listOfNotNull(lastGps, lastNetwork).maxByOrNull { it.time }
            if (best != null) {
                UserLocationStateManager.updateFromLocation(context, best)
            }
        }
    }
}
