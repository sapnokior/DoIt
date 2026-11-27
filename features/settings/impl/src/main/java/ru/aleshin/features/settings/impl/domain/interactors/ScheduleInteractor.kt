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
package ru.aleshin.features.settings.impl.domain.interactors

import kotlinx.coroutines.flow.first
import ru.aleshin.core.domain.entities.categories.MainCategory
import ru.aleshin.core.domain.entities.categories.SubCategory
import ru.aleshin.core.domain.entities.schedules.BaseDailySchedule
import ru.aleshin.core.domain.entities.schedules.Schedule
import ru.aleshin.core.domain.entities.schedules.fetchAllTimeTasks
import ru.aleshin.core.domain.entities.schedules.mapToBase
import ru.aleshin.core.domain.entities.tasks.TaskPriority
import ru.aleshin.core.domain.entities.tasks.TimeTask
import ru.aleshin.core.domain.entities.template.RepeatTime
import ru.aleshin.core.domain.entities.template.convertToTimeTask
import ru.aleshin.core.domain.repository.ScheduleRepository
import ru.aleshin.core.domain.repository.TemplatesRepository
import ru.aleshin.core.domain.repository.TimeTaskRepository
import ru.aleshin.core.utils.extensions.generateUniqueKey
import ru.aleshin.core.utils.functional.DomainResult
import ru.aleshin.core.utils.functional.TimeRange
import ru.aleshin.core.utils.functional.UnitDomainResult
import ru.aleshin.core.utils.functional.WeekDay
import ru.aleshin.core.utils.managers.DateManager
import ru.aleshin.features.settings.impl.domain.common.SettingsEitherWrapper
import ru.aleshin.features.settings.impl.domain.entities.SettingsFailures
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * @author Stanislav Aleshin on 09.06.2023.
 */
internal interface ScheduleInteractor {

    suspend fun addOrUpdateSchedules(schedules: List<Schedule>): UnitDomainResult<SettingsFailures>
    suspend fun fetchAllSchedules(): DomainResult<SettingsFailures, List<Schedule>>
    suspend fun deleteAllSchedules(): DomainResult<SettingsFailures, List<Schedule>>
    suspend fun fetchIsTodayOff(): DomainResult<SettingsFailures, Boolean>
    suspend fun applyTodaysOff(): DomainResult<SettingsFailures, Unit>
    suspend fun restoreTodaysWorkDay(): DomainResult<SettingsFailures, Unit>

    class Base @Inject constructor(
        private val scheduleRepository: ScheduleRepository,
        private val timeTaskRepository: TimeTaskRepository,
        private val templatesRepository: TemplatesRepository,
        private val dateManager: DateManager,
        private val eitherWrapper: SettingsEitherWrapper,
    ) : ScheduleInteractor {

        override suspend fun addOrUpdateSchedules(schedules: List<Schedule>) = eitherWrapper.wrap {
            scheduleRepository.addOrUpdateSchedules(schedules.map { it.mapToBase() })
            timeTaskRepository.addOrUpdateTimeTasks(schedules.flatMap { it.timeTasks })
        }

        override suspend fun fetchAllSchedules() = eitherWrapper.wrap {
            scheduleRepository.fetchSchedulesByRange(null).first()
        }

        override suspend fun deleteAllSchedules() = eitherWrapper.wrap {
            scheduleRepository.deleteAllSchedules()
        }

        override suspend fun fetchIsTodayOff(): DomainResult<SettingsFailures, Boolean> = eitherWrapper.wrap {
            val currentDate = dateManager.fetchBeginningCurrentDay()
            val schedule = scheduleRepository.fetchScheduleByDate(currentDate).first()
            val tasks = schedule?.allTimeTasks.orEmpty()
            val hasDayOffTasks = tasks.any { task ->
                val name = task.subCategory?.name ?: ""
                name.contains("Wash All Clothes") || name.contains("Nail Cutting") || name.contains("Bathroom & Toilet")
            }
            val hasWorkTasks = tasks.any { task ->
                val name = task.subCategory?.name ?: ""
                name.contains("Amazon Flex")
            }
            hasDayOffTasks && !hasWorkTasks
        }

        override suspend fun applyTodaysOff(): DomainResult<SettingsFailures, Unit> = eitherWrapper.wrap {
            val currentDate = dateManager.fetchBeginningCurrentDay()
            var schedule = scheduleRepository.fetchScheduleByDate(currentDate).first()
            if (schedule == null) {
                scheduleRepository.addOrUpdateSchedule(BaseDailySchedule(date = currentDate))
                schedule = scheduleRepository.fetchScheduleByDate(currentDate).first()
            }
            val allTasks = schedule?.allTimeTasks.orEmpty()

            val cal = Calendar.getInstance()
            val tasksToDelete = allTasks.filter { task ->
                cal.time = task.timeRange.from
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hour >= 8
            }
            if (tasksToDelete.isNotEmpty()) {
                timeTaskRepository.deleteTimeTasksByIds(tasksToDelete.map { it.key })
            }

            val templates = templatesRepository.fetchAllTemplates().first()
            val dayOffTemplates = templates.filter { tmpl ->
                tmpl.repeatTimes.any { it is RepeatTime.WeekDays && it.day == WeekDay.WEDNESDAY }
            }

            val newTasks = if (dayOffTemplates.isNotEmpty()) {
                dayOffTemplates
                    .filter { tmpl ->
                        cal.time = tmpl.startTime
                        cal.get(Calendar.HOUR_OF_DAY) >= 8
                    }
                    .map { tmpl ->
                        tmpl.convertToTimeTask(date = currentDate, createdAt = currentDate)
                    }
            } else {
                fallbackDayOffItems(currentDate)
            }

            if (newTasks.isNotEmpty()) {
                timeTaskRepository.addOrUpdateTimeTasks(newTasks)
            }
        }

        override suspend fun restoreTodaysWorkDay(): DomainResult<SettingsFailures, Unit> = eitherWrapper.wrap {
            val currentDate = dateManager.fetchBeginningCurrentDay()
            var schedule = scheduleRepository.fetchScheduleByDate(currentDate).first()
            if (schedule == null) {
                scheduleRepository.addOrUpdateSchedule(BaseDailySchedule(date = currentDate))
                schedule = scheduleRepository.fetchScheduleByDate(currentDate).first()
            }
            val allTasks = schedule?.allTimeTasks.orEmpty()

            val cal = Calendar.getInstance()
            val tasksToDelete = allTasks.filter { task ->
                cal.time = task.timeRange.from
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hour >= 8
            }
            if (tasksToDelete.isNotEmpty()) {
                timeTaskRepository.deleteTimeTasksByIds(tasksToDelete.map { it.key })
            }

            val templates = templatesRepository.fetchAllTemplates().first()
            val workDayTemplates = templates.filter { tmpl ->
                tmpl.repeatTimes.any { it is RepeatTime.WeekDays && it.day != WeekDay.WEDNESDAY }
            }

            val newTasks = if (workDayTemplates.isNotEmpty()) {
                workDayTemplates
                    .filter { tmpl ->
                        cal.time = tmpl.startTime
                        cal.get(Calendar.HOUR_OF_DAY) >= 8
                    }
                    .map { tmpl ->
                        tmpl.convertToTimeTask(date = currentDate, createdAt = currentDate)
                    }
            } else {
                fallbackWorkDayItems(currentDate)
            }

            if (newTasks.isNotEmpty()) {
                timeTaskRepository.addOrUpdateTimeTasks(newTasks)
            }
        }

        private fun fallbackDayOffItems(date: Date): List<TimeTask> {
            val items = listOf(
                ItemSpec("🧺 Wash All Clothes & Full Laundry Chore", 7, 8, 0, 9, 30, "- [ ] Separate delivery clothes, towels & darks\n- [ ] Add detergent and hand/machine wash\n- [ ] Rinse thoroughly\n- [ ] Hang outside in bright sunlight"),
                ItemSpec("✂️ Nail Cutting & Personal Grooming", 7, 9, 30, 10, 0, "- [ ] Trim fingernails neatly\n- [ ] Trim toenails\n- [ ] Smooth rough edges with filer\n- [ ] Apply moisturizer / oil"),
                ItemSpec("🚽 Bathroom & Toilet Cleaning Deep Reset", 7, 10, 0, 10, 45, "- [ ] Apply toilet bowl cleaner & let sit\n- [ ] Scrub toilet bowl & rim with brush\n- [ ] Scrub bathroom floor tiles\n- [ ] Clean washbasin, tap & mirror\n- [ ] Rinse and mop dry"),
                ItemSpec("🍳 Kitchen Deep Cleaning & Counter Reset", 7, 10, 45, 11, 30, "- [ ] Clean gas stove & burner top\n- [ ] Wipe kitchen counter & tiles\n- [ ] Wash accumulated pots & pans\n- [ ] Reorganize spice jars & wipe shelves\n- [ ] Empty kitchen trash bin"),
                ItemSpec("🏍️ CT 100 Motorcycle Maintenance & Clean", 7, 11, 30, 12, 15, "- [ ] Wipe motorcycle body, tank & headlight\n- [ ] Clean and lubricate drive chain\n- [ ] Check engine oil level (20W-50)\n- [ ] Check tyre pressures & brake play\n- [ ] Check horn, indicator & brake light"),
                ItemSpec("🎓 Deep BCA IGNOU Midday Sprint", 1, 12, 15, 13, 30, "- [ ] Focused reading on difficult topics\n- [ ] Write 2-3 TMA assignment questions\n- [ ] Run code practicals"),
                ItemSpec("🍲 Cook Wholesome Lunch & Kitchen Reset", 11, 13, 30, 14, 30, "- [ ] Cook delicious meal (parathas / pulao / sabji)\n- [ ] Eat peacefully\n- [ ] Clean cookware and wipe counter"),
                ItemSpec("🛋️ Guilt-Free Afternoon Nap & Deep Rest", 5, 14, 30, 16, 30, "- [ ] Darken room & put phone on DND\n- [ ] Full nervous system deep recovery"),
                ItemSpec("👕 Collect & Fold Fresh Dry Clothes", 7, 16, 30, 17, 0, "- [ ] Take dry clothes off clothesline\n- [ ] Fold delivery clothes neatly\n- [ ] Stack and store in wardrobe"),
                ItemSpec("☕ Evening Chai & Personal Time", 5, 17, 0, 18, 0, "- [ ] Brew hot cardamom chai\n- [ ] Call with Raja Ji\n- [ ] Listen to favorite tracks & unwind"),
                ItemSpec("💻 BCA IGNOU Second Focus Review (1.5 Hours)", 1, 18, 0, 19, 30, "- [ ] Review assignment write-ups\n- [ ] Double-check question numbers & covers\n- [ ] Plan upcoming study goals"),
                ItemSpec("🍽️ Light Dinner & Sink Clear", 11, 19, 30, 20, 30, "- [ ] Light meal\n- [ ] Clean plates & leave sink spotless"),
                ItemSpec("🎬 Movie / Gaming / Leisure Time", 9, 20, 30, 21, 30, "- [ ] Watch thriller/movie or play favorite game\n- [ ] Zero stress and total relaxation"),
                ItemSpec("🌙 Tomorrow Prep & Wind-down", 5, 21, 30, 22, 30, "- [ ] Pack clean delivery clothes for Thursday\n- [ ] Phone & power bank on charger\n- [ ] Wind down for 10:30 PM sleep"),
            )
            return items.map { it.toTimeTask(date) }
        }

        private fun fallbackWorkDayItems(date: Date): List<TimeTask> {
            val items = listOf(
                ItemSpec("Amazon Flex Delivery Shift (6h)", 3, 8, 0, 14, 0, "- [ ] Reach delivery station & check-in\n- [ ] Scan block packages & organize in bag\n- [ ] Deliver packages safely across route\n- [ ] Log customer delivery confirmations"),
                ItemSpec("Return to Station (BTS) & Check-out", 8, 14, 0, 14, 30, "- [ ] Return any undelivered packages\n- [ ] Hand over empty delivery totes\n- [ ] Check out in Amazon Flex app"),
                ItemSpec("Ride Home on CT 100 (~3 PM Arrival)", 8, 14, 30, 15, 0, "- [ ] Smooth ride back on CT 100\n- [ ] Park bike safely & lock"),
                ItemSpec("Post-Shift Decompression & Lunch Buffer", 5, 15, 0, 16, 30, "- [ ] Wash hands & face\n- [ ] Eat hearty lunch\n- [ ] Lie down & rest nervous system"),
                ItemSpec("Cook Fresh Dinner", 11, 19, 0, 19, 45, "- [ ] Chop fresh vegetables\n- [ ] Cook sabji with roti or egg pulao\n- [ ] Keep kitchen counters neat"),
                ItemSpec("Dinner & 2-Min Sink Reset", 11, 19, 45, 20, 0, "- [ ] Eat dinner mindfully\n- [ ] Wash dinner plates & utensils right away\n- [ ] Wipe sink clean"),
                ItemSpec("BCA IGNOU Evening Sprint (1.5 Hours)", 1, 20, 0, 21, 30, "- [ ] Practice Python / C / SQL practicals\n- [ ] Work on BCA project / assignments\n- [ ] Update study notes"),
                ItemSpec("Tomorrow Prep & Wind-down", 5, 21, 30, 22, 30, "- [ ] Lay out delivery clothes\n- [ ] Put phone & power bank to charge\n- [ ] Set alarm & peaceful sleep by 10:30 PM"),
            )
            return items.map { it.toTimeTask(date) }
        }

        private data class ItemSpec(
            val title: String,
            val categoryId: Long,
            val startH: Int,
            val startM: Int,
            val endH: Int,
            val endM: Int,
            val desc: String,
        ) {
            fun toTimeTask(date: Date): TimeTask {
                val cal = Calendar.getInstance().apply { time = date; set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                cal.set(Calendar.HOUR_OF_DAY, startH)
                cal.set(Calendar.MINUTE, startM)
                val from = cal.time
                cal.set(Calendar.HOUR_OF_DAY, endH)
                cal.set(Calendar.MINUTE, endM)
                val to = cal.time
                return TimeTask(
                    key = generateUniqueKey(),
                    date = date,
                    timeRange = TimeRange(from, to),
                    category = MainCategory(id = categoryId),
                    subCategory = SubCategory(id = 0, mainCategoryId = categoryId, name = title),
                    priority = TaskPriority.STANDARD,
                    isCompleted = false,
                    isEnableNotification = true,
                    isConsiderInStatistics = true,
                    note = desc,
                )
            }
        }
    }
}
