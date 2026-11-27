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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.aleshin.core.domain.entities.tasks.TaskPriority
import ru.aleshin.core.domain.entities.tasks.TaskStepsParser
import ru.aleshin.features.home.impl.presentation.theme.HomeThemeRes
import ru.aleshin.timeplanner.core.ui.mappers.mapToMonogram
import ru.aleshin.timeplanner.core.ui.views.CategoryIconMonogram
import ru.aleshin.timeplanner.core.ui.views.CategoryTextMonogram
import ru.aleshin.timeplanner.core.ui.views.ExpandedIcon

@Composable
private fun StepCheckbox(
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedColor: Color = MaterialTheme.colorScheme.outline,
    checkmarkColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .border(
                width = 1.5.dp,
                color = if (isCompleted) checkedColor else uncheckedColor,
                shape = CircleShape,
            )
            .background(
                color = if (isCompleted) checkedColor else Color.Transparent,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkmarkColor,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * @author Stanislav Aleshin on 22.02.2023.
 */
@Composable
internal fun PlannedTimeTask(
    modifier: Modifier = Modifier,
    onEditClicked: () -> Unit,
    onToggleStep: (Int) -> Unit = {},
    taskTitle: String,
    taskSubTitle: String?,
    taskDurationTitle: String,
    categoryIcon: Painter?,
    priority: TaskPriority,
    enabledNotifications: Boolean,
    note: String?,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val steps = remember(note) { TaskStepsParser.parseSteps(note) }
    val generalNote = remember(note) { TaskStepsParser.parseGeneralNote(note) }

    Surface(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier,
        enabled = true,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = if (isExpanded) 0.dp else 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.align(Alignment.Top)) {
                    if (categoryIcon != null) {
                        CategoryIconMonogram(
                            icon = categoryIcon,
                            iconDescription = taskTitle,
                            iconColor = MaterialTheme.colorScheme.primary,
                            priority = priority.mapToMonogram(),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    } else {
                        CategoryTextMonogram(
                            text = taskTitle.first().toString(),
                            textColor = MaterialTheme.colorScheme.primary,
                            priority = priority.mapToMonogram(),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                }
                TimeTaskTitles(
                    modifier = Modifier.weight(1f),
                    title = taskTitle,
                    titleColor = MaterialTheme.colorScheme.onSurface,
                    subTitle = taskSubTitle,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TimeTaskDurationTitle(
                        title = taskDurationTitle,
                        enabledNotifications = enabledNotifications,
                    )
                    ExpandedIcon(
                        isExpanded = isExpanded,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        description = "Expand task steps",
                    )
                }
            }

            if (!isExpanded && steps.isNotEmpty()) {
                val doneCount = steps.count { it.isCompleted }
                Text(
                    text = "📋 $doneCount of ${steps.size} steps done",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 68.dp, bottom = 12.dp),
                )
            }

            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                if (generalNote.isNotEmpty()) {
                    Text(
                        text = generalNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (steps.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleStep(index) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StepCheckbox(
                                    isCompleted = step.isCompleted,
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (step.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (step.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                } else if (generalNote.isEmpty()) {
                    Text(
                        text = "No steps yet. Tap ✏️ to add steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (steps.isNotEmpty()) {
                        val doneCount = steps.count { it.isCompleted }
                        Text(
                            text = "$doneCount/${steps.size} steps done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEditClicked() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RunningTimeTask(
    modifier: Modifier = Modifier,
    onEditClicked: () -> Unit,
    onIncreaseTime: () -> Unit,
    onReduceTime: () -> Unit,
    onToggleStep: (Int) -> Unit = {},
    taskTitle: String,
    taskSubTitle: String?,
    categoryIcon: Painter?,
    priority: TaskPriority,
    note: String?,
) {
    var expandedTask by rememberSaveable { mutableStateOf(false) }
    val steps = remember(note) { TaskStepsParser.parseSteps(note) }
    val generalNote = remember(note) { TaskStepsParser.parseGeneralNote(note) }

    Surface(
        onClick = { expandedTask = !expandedTask },
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = if (expandedTask) 0.dp else 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.align(Alignment.Top)) {
                    if (categoryIcon != null) {
                        CategoryIconMonogram(
                            icon = categoryIcon,
                            iconDescription = taskTitle,
                            iconColor = MaterialTheme.colorScheme.onPrimary,
                            priority = priority.mapToMonogram(),
                            backgroundColor = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        CategoryTextMonogram(
                            text = taskTitle.first().toString(),
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            priority = priority.mapToMonogram(),
                        )
                    }
                }
                TimeTaskTitles(
                    modifier = Modifier.weight(1f),
                    title = taskTitle,
                    titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    subTitle = taskSubTitle,
                )
                Box(
                    modifier = Modifier.size(36.dp).animateContentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpandedIcon(
                        isExpanded = expandedTask,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        description = "Expand task",
                    )
                }
            }

            if (!expandedTask && steps.isNotEmpty()) {
                val doneCount = steps.count { it.isCompleted }
                Text(
                    text = "📋 $doneCount of ${steps.size} steps done",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 68.dp, bottom = 12.dp),
                )
            }

            if (expandedTask) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                )

                if (generalNote.isNotEmpty()) {
                    Text(
                        text = generalNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (steps.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleStep(index) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StepCheckbox(
                                    isCompleted = step.isCompleted,
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (step.isCompleted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    textDecoration = if (step.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                } else if (generalNote.isEmpty()) {
                    Text(
                        text = "No steps yet. Tap ✏️ to add steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onIncreaseTime) {
                        Icon(
                            painter = painterResource(HomeThemeRes.icons.add),
                            contentDescription = HomeThemeRes.strings.timeTaskAddIconDesc,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = HomeThemeRes.strings.timeTaskIncreaseTimeTitle,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    TextButton(onClick = onReduceTime) {
                        Icon(
                            painter = painterResource(HomeThemeRes.icons.remove),
                            contentDescription = HomeThemeRes.strings.timeTaskRemoveIconDesc,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = HomeThemeRes.strings.timeTaskReduceTimeTitle,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEditClicked() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompletedTimeTask(
    modifier: Modifier = Modifier,
    onEditClicked: () -> Unit,
    onDoneChange: () -> Unit,
    onToggleStep: (Int) -> Unit = {},
    taskTitle: String,
    taskSubTitle: String?,
    categoryIcon: Painter?,
    isCompleted: Boolean,
    note: String?,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val steps = remember(note) { TaskStepsParser.parseSteps(note) }
    val generalNote = remember(note) { TaskStepsParser.parseGeneralNote(note) }

    Surface(
        modifier = modifier,
        onClick = { isExpanded = !isExpanded },
        enabled = true,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = if (isExpanded) 0.dp else 16.dp, start = 16.dp, end = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.align(Alignment.Top)) {
                    if (categoryIcon != null) {
                        CategoryIconMonogram(
                            icon = categoryIcon,
                            iconDescription = taskTitle,
                            iconColor = MaterialTheme.colorScheme.onTertiary,
                            backgroundColor = MaterialTheme.colorScheme.tertiary,
                        )
                    } else {
                        CategoryTextMonogram(
                            text = taskTitle.first().toString(),
                            textColor = MaterialTheme.colorScheme.onTertiary,
                            backgroundColor = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                TimeTaskTitles(
                    modifier = Modifier.weight(1f),
                    title = taskTitle,
                    titleColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    subTitle = taskSubTitle,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        modifier = Modifier.size(36.dp),
                        onClick = onDoneChange,
                    ) {
                        if (isCompleted) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(HomeThemeRes.icons.check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        } else {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(HomeThemeRes.icons.cancel),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    ExpandedIcon(
                        isExpanded = isExpanded,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        description = "Expand task",
                    )
                }
            }

            if (!isExpanded && steps.isNotEmpty()) {
                val doneCount = steps.count { it.isCompleted }
                Text(
                    text = "📋 $doneCount of ${steps.size} steps done",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(start = 68.dp, bottom = 12.dp),
                )
            }

            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                )

                if (generalNote.isNotEmpty()) {
                    Text(
                        text = generalNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (steps.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleStep(index) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StepCheckbox(
                                    isCompleted = step.isCompleted,
                                    checkedColor = MaterialTheme.colorScheme.tertiary,
                                    uncheckedColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                                    checkmarkColor = MaterialTheme.colorScheme.onTertiary,
                                )
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (step.isCompleted) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onTertiaryContainer,
                                    textDecoration = if (step.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                } else if (generalNote.isEmpty()) {
                    Text(
                        text = "No steps yet. Tap ✏️ to add steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (steps.isNotEmpty()) {
                        val doneCount = steps.count { it.isCompleted }
                        Text(
                            text = "$doneCount/${steps.size} steps done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEditClicked() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task",
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddTimeTaskView(
    modifier: Modifier = Modifier,
    showAddIconForFreeTime: Boolean = true,
    isFreeTime: Boolean,
    remainingTimeTitle: String,
    onViewClicked: () -> Unit,
) {
    Surface(
        onClick = onViewClicked,
        modifier = modifier.height(46.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showAddIconForFreeTime) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Filled.Add,
                    contentDescription = HomeThemeRes.strings.timeTaskIncreaseTimeTitle,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = if (isFreeTime) HomeThemeRes.strings.addFreeTimeTaskTitle else HomeThemeRes.strings.addTaskTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isFreeTime) {
                Text(
                    text = remainingTimeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun TimeTaskTitles(
    modifier: Modifier = Modifier,
    title: String,
    titleColor: Color,
    subTitle: String?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.titleMedium,
        )
        if (subTitle != null) {
            Text(
                text = subTitle,
                modifier = Modifier.padding(top = 2.dp),
                color = titleColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TimeTaskDurationTitle(
    modifier: Modifier = Modifier,
    title: String,
    enabledNotifications: Boolean,
) {
    Row(
        modifier = modifier, 
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!enabledNotifications) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = HomeThemeRes.icons.offNotifications),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TimeTaskNoteView(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    expanded: Boolean,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    var isOverflow by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && isOverflow || enabled && expanded,
        shape = MaterialTheme.shapes.medium,
        color = container,
    ) {
        AnimatedContent(
            targetState = expanded,
            label = "Note",
        ) { isExpanded ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(id = HomeThemeRes.icons.notes),
                    contentDescription = HomeThemeRes.strings.noteTitle,
                    tint = content,
                )
                Text(
                    text = text,
                    color = content,
                    maxLines = when (isExpanded) {
                        true -> 4
                        false -> 1
                    },
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result -> isOverflow = result.didOverflowHeight },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/* ----------------------- Release Preview -----------------------
@Preview
@Composable
private fun PlannedTimeTaskView_Preview() {
    TimePlannerTheme(
        dynamicColor = false,
        themeColorsType = ThemeColorsUiType.LIGHT,
        language = LanguageUiType.RU,
    ) {
        HomeTheme {
            PlannedTimeTask(
                modifier = Modifier.width(330.dp),
                onViewClicked = {},
                taskTitle = "Дела",
                taskSubTitle = null,
                taskDurationTitle = "4ч",
                categoryIcon = painterResource(id = HomeThemeRes.icons.settingsIcon),
                categoryIconDescription = "",
            )
        }
    }
}

@Preview
@Composable
private fun RunningTimeTaskView_Preview() {
    TimePlannerTheme(
        dynamicColor = false,
        themeColorsType = ThemeColorsUiType.LIGHT,
        language = LanguageUiType.RU,
    ) {
        HomeTheme {
            RunningTimeTask(
                modifier = Modifier.width(330.dp),
                taskTitle = "Дела",
                taskSubTitle = "Выполнить домашнее задание",
                categoryIcon = painterResource(id = HomeThemeRes.icons.settingsIcon),
                categoryIconDescription = "",
                onMoreButtonClick = {},
                onIncreaseTime = {},
                onReduceTime = {},
            )
        }
    }
}

@Preview
@Composable
private fun CompletedTimeTaskView_Preview() {
    TimePlannerTheme(
        dynamicColor = false,
        themeColorsType = ThemeColorsUiType.LIGHT,
        language = LanguageUiType.RU,
    ) {
        HomeTheme {
            CompletedTimeTask(
                modifier = Modifier.width(330.dp),
                onViewClicked = {},
                taskTitle = "Дела",
                taskSubTitle = "Выполнить домашнее задание",
                categoryIcon = painterResource(id = HomeThemeRes.icons.settingsIcon),
                categoryIconDescription = "",
            )
        }
    }
}
*/
