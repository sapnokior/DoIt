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
package ru.aleshin.core.domain.entities.tasks

object TaskStepsParser {

    private val STEP_REGEX = Regex("""^\s*[-*•]?\s*\[([ xX])\]\s*(.*)$""")

    fun parseSteps(note: String?): List<TaskStep> {
        if (note.isNullOrBlank()) return emptyList()
        val steps = mutableListOf<TaskStep>()
        note.lines().forEach { line ->
            val trimmed = line.trim()
            val match = STEP_REGEX.find(trimmed)
            if (match != null) {
                val mark = match.groupValues[1]
                val text = match.groupValues[2].trim()
                if (text.isNotEmpty()) {
                    steps.add(
                        TaskStep(
                            title = text,
                            isCompleted = mark.equals("x", ignoreCase = true),
                        )
                    )
                }
            }
        }
        return steps
    }

    fun parseGeneralNote(note: String?): String {
        if (note.isNullOrBlank()) return ""
        val nonStepLines = note.lines().filter { line ->
            !STEP_REGEX.matches(line.trim())
        }
        return nonStepLines.joinToString("\n").trim()
    }

    fun formatNote(generalNote: String?, steps: List<TaskStep>): String {
        val cleanGeneral = generalNote?.trim().orEmpty()
        val stepLines = steps.map { step ->
            val mark = if (step.isCompleted) "x" else " "
            "- [$mark] ${step.title.trim()}"
        }
        return when {
            cleanGeneral.isNotEmpty() && stepLines.isNotEmpty() -> "$cleanGeneral\n" + stepLines.joinToString("\n")
            stepLines.isNotEmpty() -> stepLines.joinToString("\n")
            cleanGeneral.isNotEmpty() -> cleanGeneral
            else -> ""
        }
    }

    fun toggleStep(note: String?, stepIndex: Int): String {
        val steps = parseSteps(note).toMutableList()
        if (stepIndex in steps.indices) {
            steps[stepIndex] = steps[stepIndex].copy(isCompleted = !steps[stepIndex].isCompleted)
        }
        val general = parseGeneralNote(note)
        return formatNote(general, steps)
    }
}
