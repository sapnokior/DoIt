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
package ru.aleshin.features.settings.impl.presentation.ui.settings.views.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.aleshin.features.settings.impl.presentation.theme.SettingsThemeRes
import ru.aleshin.features.settings.impl.presentation.ui.common.SettingsItemIcon
import ru.aleshin.features.settings.impl.presentation.ui.common.SettingsSection

/**
 * Section for managing daily schedule overrides such as "Today's Off".
 */
@Composable
internal fun ScheduleSection(
    modifier: Modifier = Modifier,
    isTodayOff: Boolean,
    onApplyTodayOff: () -> Unit,
    onRestoreWorkDay: () -> Unit,
) {
    SettingsSection(
        modifier = modifier,
        title = "Schedule",
    ) {
        TodayOffView(
            isTodayOff = isTodayOff,
            onApplyTodayOff = onApplyTodayOff,
            onRestoreWorkDay = onRestoreWorkDay,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayOffView(
    modifier: Modifier = Modifier,
    isTodayOff: Boolean,
    onApplyTodayOff: () -> Unit,
    onRestoreWorkDay: () -> Unit,
) {
    var isOpenDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        onClick = { isOpenDialog = true },
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsItemIcon(
                icon = SettingsThemeRes.icons.calendar,
                contentDescription = null,
            )
            Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp).weight(1f)) {
                Text(
                    text = "Today's Off",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (isTodayOff) {
                        "Off Day Active: Amazon shift removed, chores & rest scheduled"
                    } else {
                        "Override workday with off-day chores (Laundry, Nail Cutting, Toilet & Kitchen Cleaning)"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = isTodayOff,
                onCheckedChange = { isOpenDialog = true },
            )
        }
    }

    if (isOpenDialog) {
        BasicAlertDialog(onDismissRequest = { isOpenDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = if (isTodayOff) "Switch Back to Work Day?" else "Set Today as Off Day?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isTodayOff) {
                            "This will restore your 6-hour Amazon Flex delivery shift (08:00 AM – 02:00 PM), commute, and evening routine for today."
                        } else {
                            "This will keep your wake-up time at 05:30 AM and sleep at 10:30 PM, remove today's 6-hour Amazon Flex shift, and schedule clothes washing, nail cutting, bathroom & kitchen cleaning, motorcycle check, and afternoon rest."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = { isOpenDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Button(
                            onClick = {
                                isOpenDialog = false
                                if (isTodayOff) onRestoreWorkDay() else onApplyTodayOff()
                            },
                        ) {
                            Text(if (isTodayOff) "Restore Work Day" else "Today's Off")
                        }
                    }
                }
            }
        }
    }
}
