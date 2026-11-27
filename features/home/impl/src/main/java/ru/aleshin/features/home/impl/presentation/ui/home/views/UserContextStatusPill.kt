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
package ru.aleshin.features.home.impl.presentation.ui.home.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.aleshin.core.domain.entities.location.UserKnownPlace
import ru.aleshin.core.domain.entities.location.UserLocationStateManager
import ru.aleshin.core.domain.entities.location.UserMovementActivity

/**
 * Interactive pill showing current detected context (e.g. "🏠 Home", "💼 Work", "🏍️ Driving").
 * Tapping allows quickly selecting any location or switching activity.
 */
@Composable
fun UserContextStatusPill(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by UserLocationStateManager.currentState.collectAsState()
    var isMenuOpen by remember { mutableStateOf(false) }

    val displayEmoji = if (state.activity == UserMovementActivity.DRIVING) {
        state.activity.emoji
    } else {
        state.place.emoji
    }

    val displayTitle = if (state.activity == UserMovementActivity.DRIVING) {
        "Driving"
    } else {
        state.place.title
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                .clickable { isMenuOpen = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = displayEmoji, fontSize = 16.sp)
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }

        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            UserKnownPlace.entries.filter { it != UserKnownPlace.UNKNOWN }.forEach { place ->
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(place.emoji)
                            Text(place.title)
                        }
                    },
                    onClick = {
                        UserLocationStateManager.updateManualPlace(context, place)
                        isMenuOpen = false
                    },
                )
            }
        }
    }
}
