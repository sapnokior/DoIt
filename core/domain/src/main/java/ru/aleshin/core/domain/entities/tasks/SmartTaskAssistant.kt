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

import ru.aleshin.core.utils.extensions.setHoursAndMinutes
import ru.aleshin.core.utils.extensions.shiftDay
import ru.aleshin.core.utils.extensions.startThisDay
import ru.aleshin.core.utils.functional.TimeRange
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Natural language parser & smart task assistant.
 * Extracts task title, priority, date, and time range from natural language input.
 * Examples:
 * - "Gym tomorrow at 6pm"
 * - "urgent Meeting with Bob at 14:00"
 * - "Read book tonight at 21:00 for 45m"
 */
data class SmartTaskParsedResult(
    val title: String,
    val date: Date,
    val timeRange: TimeRange,
    val priority: TaskPriority = TaskPriority.STANDARD,
)

object SmartTaskAssistant {

    private val TIME_REGEX = Regex("""(?i)\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""")
    private val DURATION_REGEX = Regex("""(?i)\b(?:for\s+)?(\d+)\s*(m|min|mins|minutes|h|hr|hrs|hours)\b""")
    private val PRIORITY_URGENT_REGEX = Regex("""(?i)\b(urgent|p1|high\s*priority|important|asap)\b""")
    private val PRIORITY_LOW_REGEX = Regex("""(?i)\b(low\s*priority|p3|optional)\b""")

    fun parse(input: String, baseDate: Date = Date()): SmartTaskParsedResult {
        var text = input.trim()

        // 1. Detect priority
        var priority = TaskPriority.STANDARD
        if (PRIORITY_URGENT_REGEX.containsMatchIn(text)) {
            priority = TaskPriority.MAX
            text = text.replace(PRIORITY_URGENT_REGEX, "").trim()
        } else if (PRIORITY_LOW_REGEX.containsMatchIn(text)) {
            priority = TaskPriority.STANDARD
            text = text.replace(PRIORITY_LOW_REGEX, "").trim()
        }

        // 2. Detect target date (today, tomorrow, day after tomorrow)
        var targetDate = baseDate.startThisDay()
        val lowerText = text.lowercase(Locale.ROOT)
        when {
            lowerText.contains("tomorrow") -> {
                targetDate = targetDate.shiftDay(1)
                text = text.replace(Regex("""(?i)\btomorrow\b"""), "").trim()
            }
            lowerText.contains("day after tomorrow") -> {
                targetDate = targetDate.shiftDay(2)
                text = text.replace(Regex("""(?i)\bday after tomorrow\b"""), "").trim()
            }
            lowerText.contains("today") -> {
                text = text.replace(Regex("""(?i)\btoday\b"""), "").trim()
            }
            lowerText.contains("tonight") -> {
                text = text.replace(Regex("""(?i)\btonight\b"""), "").trim()
            }
        }

        // 3. Detect duration (defaults to 30 mins)
        var durationMinutes = 30
        val durationMatch = DURATION_REGEX.find(text)
        if (durationMatch != null) {
            val amount = durationMatch.groupValues[1].toIntOrNull() ?: 30
            val unit = durationMatch.groupValues[2].lowercase(Locale.ROOT)
            durationMinutes = if (unit.startsWith("h")) amount * 60 else amount
            text = text.removeRange(durationMatch.range).trim()
        }

        // 4. Detect start time
        val timeMatch = TIME_REGEX.find(text)
        val calendar = Calendar.getInstance().apply { time = targetDate }

        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toIntOrNull() ?: 12
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val amPm = timeMatch.groupValues[3].lowercase(Locale.ROOT)

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            text = text.removeRange(timeMatch.range).trim()
        } else {
            // Default time: if baseDate is today and later than now, round up to next hour
            val nowCal = Calendar.getInstance().apply { time = baseDate }
            val nextHour = nowCal.get(Calendar.HOUR_OF_DAY) + 1
            calendar.set(Calendar.HOUR_OF_DAY, if (nextHour < 24) nextHour else 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.time
        val endCal = Calendar.getInstance().apply {
            time = startTime
            add(Calendar.MINUTE, durationMinutes)
        }
        val endTime = endCal.time

        // Clean up remaining filler words from title
        val cleanedTitle = text
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '-', ':')
            .ifEmpty { "New Task" }

        return SmartTaskParsedResult(
            title = cleanedTitle,
            date = targetDate,
            timeRange = TimeRange(from = startTime, to = endTime),
            priority = priority,
        )
    }
}
