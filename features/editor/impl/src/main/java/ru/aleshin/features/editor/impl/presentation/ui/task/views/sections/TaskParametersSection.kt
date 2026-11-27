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
package ru.aleshin.features.editor.impl.presentation.ui.task.views.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.aleshin.core.utils.functional.WeekDay
import ru.aleshin.features.editor.impl.presentation.mappers.convertToItem
import ru.aleshin.features.editor.impl.presentation.mappers.convertToModel
import ru.aleshin.features.editor.impl.presentation.models.tasks.TaskPriorityItemUi
import ru.aleshin.features.editor.impl.presentation.models.tasks.TaskRepeatMode
import ru.aleshin.features.editor.impl.presentation.models.tasks.TimeTaskEditParametersUi
import ru.aleshin.features.editor.impl.presentation.theme.EditorThemeRes
import ru.aleshin.features.editor.impl.presentation.ui.task.views.ParameterChooser
import ru.aleshin.features.editor.impl.presentation.ui.task.views.SegmentedParametersChooser
import ru.aleshin.features.editor.impl.presentation.ui.task.views.TaskNotificationsMenu
import ru.aleshin.timeplanner.core.ui.theme.TimePlannerRes
import ru.aleshin.timeplanner.core.ui.views.SegmentedButtonItem

internal enum class RepeatModeItemUi(val mode: TaskRepeatMode) : SegmentedButtonItem {
    ONCE(TaskRepeatMode.ONCE) {
        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = "Once"
    },
    DAILY(TaskRepeatMode.DAILY) {
        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = "Daily"
    },
    SPECIFIC_DAYS(TaskRepeatMode.SPECIFIC_DAYS) {
        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = "Specific Days"
    };

    companion object {
        fun fromMode(mode: TaskRepeatMode) = when (mode) {
            TaskRepeatMode.ONCE -> ONCE
            TaskRepeatMode.DAILY -> DAILY
            TaskRepeatMode.SPECIFIC_DAYS -> SPECIFIC_DAYS
        }
    }
}

/**
 * @author Stanislav Aleshin on 30.07.2026.
 */
@Composable
internal fun TaskParametersSection(
    modifier: Modifier = Modifier,
    parameters: TimeTaskEditParametersUi,
    repeatMode: TaskRepeatMode = TaskRepeatMode.ONCE,
    repeatDays: Set<WeekDay> = WeekDay.entries.toSet(),
    isEnabled: Boolean = true,
    onParametersChange: (TimeTaskEditParametersUi) -> Unit,
    onRepeatModeChange: (TaskRepeatMode) -> Unit = {},
    onRepeatDayToggle: (WeekDay) -> Unit = {},
) {
    var isNotificationsMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SegmentedParametersChooser(
            enabled = isEnabled,
            parameters = RepeatModeItemUi.entries.toTypedArray(),
            selected = RepeatModeItemUi.fromMode(repeatMode),
            leadingIcon = painterResource(TimePlannerRes.icons.templateTab),
            title = "Recurrence",
            onChangeSelected = { item ->
                onRepeatModeChange(item.mode)
            },
        )

        if (repeatMode == TaskRepeatMode.SPECIFIC_DAYS) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Repeat on specific days:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val allWeekDays = listOf(
                            WeekDay.MONDAY to "Mon",
                            WeekDay.TUESDAY to "Tue",
                            WeekDay.WEDNESDAY to "Wed",
                            WeekDay.THURSDAY to "Thu",
                            WeekDay.FRIDAY to "Fri",
                            WeekDay.SATURDAY to "Sat",
                            WeekDay.SUNDAY to "Sun",
                        )
                        allWeekDays.forEach { (day, label) ->
                            val isSelected = repeatDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onRepeatDayToggle(day) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        }

        SegmentedParametersChooser(
            enabled = isEnabled,
            parameters = TaskPriorityItemUi.entries.toTypedArray(),
            selected = parameters.priority.convertToItem(),
            leadingIcon = painterResource(EditorThemeRes.icons.priority),
            title = EditorThemeRes.strings.priorityParameterTitle,
            onChangeSelected = { priority ->
                val updatedParameters = parameters.copy(priority = priority.convertToModel())
                onParametersChange(updatedParameters)
            },
        )
        ParameterChooser(
            enabled = isEnabled,
            selected = parameters.isEnableNotification,
            leadingIcon = painterResource(EditorThemeRes.icons.notifications),
            title = EditorThemeRes.strings.notifyParameterTitle,
            description = EditorThemeRes.strings.notifyParameterDesc,
            optionsButton = if (parameters.isEnableNotification) {
                {
                    Box {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            enabled = isEnabled,
                            onClick = { isNotificationsMenuOpen = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                            )
                        }
                        TaskNotificationsMenu(
                            isExpanded = isNotificationsMenuOpen,
                            taskNotification = parameters.taskNotifications,
                            onDismiss = { isNotificationsMenuOpen = false },
                            onUpdate = { notifications ->
                                val updatedParameters = parameters.copy(
                                    taskNotifications = notifications,
                                )
                                onParametersChange(updatedParameters)
                            },
                        )
                    }
                }
            } else {
                null
            },
            onChangeSelected = { isNotificationEnabled ->
                val updatedParameters = parameters.copy(
                    isEnableNotification = isNotificationEnabled,
                )
                onParametersChange(updatedParameters)
            },
        )
        ParameterChooser(
            enabled = isEnabled,
            selected = parameters.isConsiderInStatistics,
            leadingIcon = painterResource(EditorThemeRes.icons.statistics),
            title = EditorThemeRes.strings.statisticsParameterTitle,
            description = EditorThemeRes.strings.statisticsParameterDesc,
            onChangeSelected = { isStatisticsEnabled ->
                val updatedParameters = parameters.copy(
                    isConsiderInStatistics = isStatisticsEnabled,
                )
                onParametersChange(updatedParameters)
            },
        )
    }
}
