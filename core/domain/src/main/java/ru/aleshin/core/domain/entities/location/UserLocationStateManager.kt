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

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state holder for the user's current detected place and movement state.
 */
object UserLocationStateManager {

    private const val PREFS_NAME = "doit_location_state_prefs"
    private const val KEY_PLACE = "key_current_place"
    private const val KEY_ACTIVITY = "key_current_activity"
    private const val KEY_TIMESTAMP = "key_last_updated"

    private val _currentState = MutableStateFlow(CurrentContextState())
    val currentState: StateFlow<CurrentContextState> = _currentState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val placeName = prefs.getString(KEY_PLACE, UserKnownPlace.HOME.name) ?: UserKnownPlace.HOME.name
        val activityName = prefs.getString(KEY_ACTIVITY, UserMovementActivity.STATIONARY.name) ?: UserMovementActivity.STATIONARY.name
        val timestamp = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

        val place = try { UserKnownPlace.valueOf(placeName) } catch (e: Exception) { UserKnownPlace.HOME }
        val activity = try { UserMovementActivity.valueOf(activityName) } catch (e: Exception) { UserMovementActivity.STATIONARY }

        _currentState.value = CurrentContextState(
            place = place,
            activity = activity,
            lastUpdatedMillis = timestamp,
        )
    }

    fun updateManualPlace(context: Context, place: UserKnownPlace) {
        val newState = _currentState.value.copy(
            place = place,
            lastUpdatedMillis = System.currentTimeMillis(),
        )
        saveState(context, newState)
    }

    fun updateFromLocation(context: Context, location: Location) {
        val matchedPlace = UserKnownPlace.findMatchingPlace(location.latitude, location.longitude)
        
        // Speed in m/s (3.6 km/h = 1 m/s; 15 km/h ≈ 4.16 m/s)
        val isDriving = location.hasSpeed() && (location.speed * 3.6f) > 12.0f
        val activity = if (isDriving) {
            UserMovementActivity.DRIVING
        } else {
            UserMovementActivity.STATIONARY
        }

        val newState = CurrentContextState(
            place = matchedPlace,
            activity = activity,
            lastUpdatedMillis = System.currentTimeMillis(),
        )
        saveState(context, newState)
    }

    private fun saveState(context: Context, state: CurrentContextState) {
        _currentState.value = state
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PLACE, state.place.name)
            .putString(KEY_ACTIVITY, state.activity.name)
            .putLong(KEY_TIMESTAMP, state.lastUpdatedMillis)
            .apply()
    }
}
