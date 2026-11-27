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
package ru.aleshin.core.data.datasources.schedules

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.aleshin.core.data.datasources.categories.MainCategoryDao
import ru.aleshin.core.data.datasources.goals.GoalDao
import ru.aleshin.core.data.datasources.goals.GoalHistoryDao
import ru.aleshin.core.data.datasources.location.LocationHistoryDao
import ru.aleshin.core.data.datasources.subcategories.SubCategoryDao
import ru.aleshin.core.data.datasources.tasks.TimeTaskDao
import ru.aleshin.core.data.datasources.tasks.UndefinedTaskDao
import ru.aleshin.core.data.datasources.templates.TemplateDao
import ru.aleshin.core.data.models.categories.MainCategoryEntity
import ru.aleshin.core.data.models.categories.SubCategoryEntity
import ru.aleshin.core.data.models.goals.GoalEntity
import ru.aleshin.core.data.models.goals.GoalHistoryEntity
import ru.aleshin.core.data.models.location.LocationHistoryEntity
import ru.aleshin.core.data.models.schedules.DailyScheduleEntity
import ru.aleshin.core.data.models.tasks.TimeTaskEntity
import ru.aleshin.core.data.models.tasks.UndefinedTaskEntity
import ru.aleshin.core.data.models.template.RepeatTimeEntity
import ru.aleshin.core.data.models.template.TemplateEntity

/**
 * @author Stanislav Aleshin on 25.02.2023.
 */
@Database(
    version = 20,
    entities = [
        GoalEntity::class,
        GoalHistoryEntity::class,
        TemplateEntity::class,
        RepeatTimeEntity::class,
        DailyScheduleEntity::class,
        TimeTaskEntity::class,
        UndefinedTaskEntity::class,
        MainCategoryEntity::class,
        SubCategoryEntity::class,
        LocationHistoryEntity::class,
    ],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
    ],
)
abstract class SchedulesDataBase : RoomDatabase() {

    abstract fun fetchScheduleDao(): ScheduleDao
    abstract fun fetchTimeTaskDao(): TimeTaskDao
    abstract fun fetchMainCategoryDao(): MainCategoryDao
    abstract fun fetchSubCategoryDao(): SubCategoryDao
    abstract fun fetchTemplateDao(): TemplateDao
    abstract fun fetchUndefinedTaskDao(): UndefinedTaskDao
    abstract fun fetchGoalDao(): GoalDao
    abstract fun fetchGoalHistoryDao(): GoalHistoryDao
    abstract fun fetchLocationHistoryDao(): LocationHistoryDao

    companion object {

        const val NAME = "SchedulesDataBase.db"

        val MIGRATE_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    copyAndModifiedTable(database)
                    rebaseTable(database)
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }

            private fun copyAndModifiedTable(database: SupportSQLiteDatabase) {
                val values = ContentValues()
                database.execSQL(
                    "CREATE TEMPORARY TABLE IF NOT EXISTS `mainCategories_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`custom_name` TEXT, " +
                            "`default_category_type` TEXT)",
                )
                database.query("SELECT * FROM mainCategories").apply {
                    while (moveToNext()) {
                        values.clear()
                        val id = getInt(getColumnIndexOrThrow("id"))
                        val nameCategoryName =
                            getString(getColumnIndexOrThrow("main_category_name"))
                        val engCategoryName =
                            getString(getColumnIndexOrThrow("main_category_name_eng"))
                        val type = getString(getColumnIndexOrThrow("main_icon"))
                        values.put("id", id)
                        values.put("custom_name", nameCategoryName ?: engCategoryName ?: null)
                        values.put("default_category_type", type)
                        database.insert("mainCategories_new", CONFLICT_REPLACE, values)
                    }
                    close()
                }
            }

            private fun rebaseTable(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE mainCategories")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mainCategories` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`custom_name` TEXT, " +
                            "`default_category_type` TEXT)",
                )
                database.execSQL(
                    "INSERT INTO mainCategories " +
                            "SELECT id, custom_name, default_category_type " +
                            "FROM mainCategories_new",
                )
                database.execSQL("DROP TABLE mainCategories_new")
            }
        }
        val MIGRATE_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    val healthValues = ContentValues().apply {
                        put("id", -1)
                        putNull("custom_name")
                        put("default_category_type", "HEALTH")
                    }
                    val shoppingValues = ContentValues().apply {
                        put("id", -2)
                        putNull("custom_name")
                        put("default_category_type", "SHOPPING")
                    }
                    database.insert("mainCategories", CONFLICT_REPLACE, healthValues)
                    database.insert("mainCategories", CONFLICT_REPLACE, shoppingValues)
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }
        val MIGRATE_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    database.execSQL(
                        "ALTER TABLE timeTaskTemplates " +
                                "ADD COLUMN repeat_enabled INTEGER NOT NULL DEFAULT 0",
                    )
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }
        val MIGRATE_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    database.execSQL("ALTER TABLE timeTasks ADD COLUMN created_at INTEGER")
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }
        val MIGRATE_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    migrateTemplatesTable(database)
                    migrateTimeTasksTable(database)
                    migrateUndefinedTasksTable(database)
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }

            private fun migrateTemplatesTable(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `repeatTimes_backup`")
                database.execSQL(
                    "CREATE TEMPORARY TABLE `repeatTimes_backup` AS " +
                            "SELECT * FROM `repeatTimes`",
                )
                database.execSQL("DROP TABLE IF EXISTS `timeTaskTemplates_new`")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timeTaskTemplates_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`start_time` INTEGER NOT NULL, " +
                            "`end_time` INTEGER NOT NULL, " +
                            "`main_category_id` INTEGER NOT NULL, " +
                            "`sub_category_id` INTEGER, " +
                            "`priority` TEXT NOT NULL, " +
                            "`is_enable_notification` INTEGER NOT NULL, " +
                            "`is_consider_in_statistics` INTEGER NOT NULL, " +
                            "`repeat_enabled` INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY(`main_category_id`) REFERENCES `mainCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`sub_category_id`) REFERENCES `subCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL)",
                )
                database.execSQL(
                    "INSERT INTO `timeTaskTemplates_new` (" +
                            "`id`, `start_time`, `end_time`, `main_category_id`, `sub_category_id`, " +
                            "`priority`, `is_enable_notification`, `is_consider_in_statistics`, `repeat_enabled`) " +
                            "SELECT `id`, `start_time`, `end_time`, `main_category_id`, `sub_category_id`, " +
                            "CASE " +
                            "WHEN `is_important` = 1 THEN 'MAX' " +
                            "WHEN `is_medium_important` = 1 THEN 'MEDIUM' " +
                            "ELSE 'STANDARD' " +
                            "END, " +
                            "`is_enable_notification`, `is_consider_in_statistics`, `repeat_enabled` " +
                            "FROM `timeTaskTemplates`",
                )
                database.execSQL("DROP TABLE `timeTaskTemplates`")
                database.execSQL("ALTER TABLE `timeTaskTemplates_new` RENAME TO `timeTaskTemplates`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timeTaskTemplates_main_category_id` " +
                            "ON `timeTaskTemplates` (`main_category_id`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timeTaskTemplates_sub_category_id` " +
                            "ON `timeTaskTemplates` (`sub_category_id`)",
                )
                database.execSQL(
                    "INSERT OR REPLACE INTO `repeatTimes` " +
                            "SELECT * FROM `repeatTimes_backup`",
                )
                database.execSQL("DROP TABLE `repeatTimes_backup`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_repeatTimes_template_id` " +
                            "ON `repeatTimes` (`template_id`)",
                )
            }

            private fun migrateTimeTasksTable(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `timeTasks_new`")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timeTasks_new` (" +
                            "`key` INTEGER NOT NULL, " +
                            "`daily_schedule_date` INTEGER NOT NULL, " +
                            "`next_schedule_date` INTEGER DEFAULT null, " +
                            "`start_time` INTEGER NOT NULL, " +
                            "`end_time` INTEGER NOT NULL, " +
                            "`created_at` INTEGER, " +
                            "`main_category_id` INTEGER NOT NULL, " +
                            "`sub_category_id` INTEGER, " +
                            "`linked_template_id` INTEGER, " +
                            "`is_completed` INTEGER NOT NULL DEFAULT 1, " +
                            "`priority` TEXT NOT NULL, " +
                            "`is_enable_notification` INTEGER NOT NULL, " +
                            "`fifteen_minutes_before_notify` INTEGER NOT NULL DEFAULT 0, " +
                            "`one_hour_before_notify` INTEGER NOT NULL DEFAULT 0, " +
                            "`three_hour_before_notify` INTEGER NOT NULL DEFAULT 0, " +
                            "`one_day_before_notify` INTEGER NOT NULL DEFAULT 0, " +
                            "`one_week_before_notify` INTEGER NOT NULL DEFAULT 0, " +
                            "`before_end_notify` INTEGER NOT NULL DEFAULT 0, " +
                            "`is_consider_in_statistics` INTEGER NOT NULL, " +
                            "`note` TEXT, " +
                            "PRIMARY KEY(`key`), " +
                            "FOREIGN KEY(`daily_schedule_date`) REFERENCES `dailySchedules`(`date`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`main_category_id`) REFERENCES `mainCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`sub_category_id`) REFERENCES `subCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                            "FOREIGN KEY(`linked_template_id`) REFERENCES `timeTaskTemplates`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL)",
                )
                database.execSQL(
                    "INSERT INTO `timeTasks_new` (" +
                            "`key`, `daily_schedule_date`, `next_schedule_date`, `start_time`, `end_time`, " +
                            "`created_at`, `main_category_id`, `sub_category_id`, `linked_template_id`, " +
                            "`is_completed`, `priority`, " +
                            "`is_enable_notification`, `fifteen_minutes_before_notify`, " +
                            "`one_hour_before_notify`, `three_hour_before_notify`, `one_day_before_notify`, " +
                            "`one_week_before_notify`, `before_end_notify`, `is_consider_in_statistics`, `note`) " +
                            "SELECT `key`, `daily_schedule_date`, `next_schedule_date`, `start_time`, `end_time`, " +
                            "`created_at`, `main_category_id`, `sub_category_id`, NULL, `is_completed`, " +
                            "CASE " +
                            "WHEN `is_important` = 1 THEN 'MAX' " +
                            "WHEN `is_medium_important` = 1 THEN 'MEDIUM' " +
                            "ELSE 'STANDARD' " +
                            "END, " +
                            "`is_enable_notification`, `fifteen_minutes_before_notify`, " +
                            "`one_hour_before_notify`, `three_hour_before_notify`, `one_day_before_notify`, " +
                            "`one_week_before_notify`, `before_end_notify`, `is_consider_in_statistics`, `note` " +
                            "FROM `timeTasks`",
                )
                database.execSQL("DROP TABLE `timeTasks`")
                database.execSQL("ALTER TABLE `timeTasks_new` RENAME TO `timeTasks`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timeTasks_daily_schedule_date` " +
                            "ON `timeTasks` (`daily_schedule_date`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timeTasks_next_schedule_date` " +
                            "ON `timeTasks` (`next_schedule_date`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timeTasks_main_category_id` " +
                            "ON `timeTasks` (`main_category_id`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timeTasks_sub_category_id` " +
                            "ON `timeTasks` (`sub_category_id`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timeTasks_linked_template_id` " +
                            "ON `timeTasks` (`linked_template_id`)",
                )
                database.execSQL(
                    "UPDATE `timeTasks` SET `linked_template_id` = (" +
                            "SELECT `timeTaskTemplates`.`id` FROM `timeTaskTemplates` " +
                            "WHERE `timeTaskTemplates`.`main_category_id` = `timeTasks`.`main_category_id` " +
                            "AND (`timeTaskTemplates`.`sub_category_id` IS `timeTasks`.`sub_category_id`) " +
                            "AND `timeTaskTemplates`.`priority` = `timeTasks`.`priority` " +
                            "AND `timeTaskTemplates`.`is_enable_notification` = `timeTasks`.`is_enable_notification` " +
                            "AND `timeTaskTemplates`.`is_consider_in_statistics` = `timeTasks`.`is_consider_in_statistics` " +
                            "AND strftime('%H:%M', `timeTaskTemplates`.`start_time` / 1000, 'unixepoch', 'localtime') = " +
                            "strftime('%H:%M', `timeTasks`.`start_time` / 1000, 'unixepoch', 'localtime') " +
                            "AND strftime('%H:%M', `timeTaskTemplates`.`end_time` / 1000, 'unixepoch', 'localtime') = " +
                            "strftime('%H:%M', `timeTasks`.`end_time` / 1000, 'unixepoch', 'localtime') " +
                            "AND `timeTasks`.`note` IS NULL " +
                            "ORDER BY `timeTaskTemplates`.`id` LIMIT 1" +
                            ") WHERE `linked_template_id` IS NULL"
                )
            }

            private fun migrateUndefinedTasksTable(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `undefinedTasks_new`")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `undefinedTasks_new` (" +
                            "`key` INTEGER NOT NULL, " +
                            "`created_at` INTEGER, " +
                            "`deadline` INTEGER, " +
                            "`main_category_id` INTEGER NOT NULL, " +
                            "`sub_category_id` INTEGER, " +
                            "`priority` TEXT NOT NULL, " +
                            "`note` TEXT, " +
                            "PRIMARY KEY(`key`), " +
                            "FOREIGN KEY(`main_category_id`) REFERENCES `mainCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`sub_category_id`) REFERENCES `subCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL)",
                )
                database.execSQL(
                    "INSERT INTO `undefinedTasks_new` (" +
                            "`key`, `created_at`, `deadline`, `main_category_id`, " +
                            "`sub_category_id`, `priority`, `note`) " +
                            "SELECT `key`, `created_at`, `deadline`, `main_category_id`, " +
                            "`sub_category_id`, " +
                            "CASE " +
                            "WHEN `is_important` = 1 THEN 'MAX' " +
                            "WHEN `is_medium_important` = 1 THEN 'MEDIUM' " +
                            "ELSE 'STANDARD' " +
                            "END, " +
                            "`note` " +
                            "FROM `undefinedTasks`",
                )
                database.execSQL("DROP TABLE `undefinedTasks`")
                database.execSQL("ALTER TABLE `undefinedTasks_new` RENAME TO `undefinedTasks`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_undefinedTasks_main_category_id` " +
                            "ON `undefinedTasks` (`main_category_id`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_undefinedTasks_sub_category_id` " +
                            "ON `undefinedTasks` (`sub_category_id`)",
                )
            }
        }
        val MIGRATE_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `goals` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`scope_type` TEXT NOT NULL, " +
                            "`main_category_id` INTEGER, " +
                            "`sub_category_id` INTEGER, " +
                            "`metric` TEXT NOT NULL, " +
                            "`direction` TEXT NOT NULL, " +
                            "`target_value` INTEGER NOT NULL, " +
                            "`period` TEXT NOT NULL, " +
                            "`start_date` INTEGER NOT NULL, " +
                            "`created_at` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`main_category_id`) REFERENCES `mainCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                            "FOREIGN KEY(`sub_category_id`) REFERENCES `subCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goals_main_category_id` " +
                            "ON `goals` (`main_category_id`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goals_sub_category_id` " +
                            "ON `goals` (`sub_category_id`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goals_created_at` " +
                            "ON `goals` (`created_at`)",
                    )
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `goalPeriods` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`goal_id` INTEGER NOT NULL, " +
                            "`goal_title` TEXT NOT NULL, " +
                            "`metric` TEXT NOT NULL, " +
                            "`direction` TEXT NOT NULL, " +
                            "`target_value` INTEGER NOT NULL, " +
                            "`actual_value` INTEGER NOT NULL, " +
                            "`period` TEXT NOT NULL, " +
                            "`period_start` INTEGER NOT NULL, " +
                            "`period_end` INTEGER NOT NULL, " +
                            "`is_achieved` INTEGER NOT NULL, " +
                            "`created_at` INTEGER NOT NULL)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goalPeriods_goal_id` " +
                            "ON `goalPeriods` (`goal_id`)",
                    )
                    database.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_goalPeriods_goal_id_period_start_period_end` " +
                            "ON `goalPeriods` (`goal_id`, `period_start`, `period_end`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goalPeriods_period_end` " +
                            "ON `goalPeriods` (`period_end`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goalPeriods_is_achieved_period_end` " +
                            "ON `goalPeriods` (`is_achieved`, `period_end`)",
                    )
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }
        val MIGRATE_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `goals_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`scope_type` TEXT NOT NULL, " +
                            "`main_category_id` INTEGER, " +
                            "`sub_category_id` INTEGER, " +
                            "`metric` TEXT NOT NULL, " +
                            "`direction` TEXT NOT NULL, " +
                            "`target_value` INTEGER NOT NULL, " +
                            "`created_at` INTEGER NOT NULL, " +
                            "`deadline` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`main_category_id`) REFERENCES `mainCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                            "FOREIGN KEY(`sub_category_id`) REFERENCES `subCategories`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL)",
                    )
                    database.execSQL(
                        "INSERT INTO `goals_new` (" +
                            "`id`, `title`, `scope_type`, `main_category_id`, `sub_category_id`, " +
                            "`metric`, `direction`, `target_value`, `created_at`, `deadline`) " +
                            "SELECT `id`, `title`, `scope_type`, `main_category_id`, " +
                            "`sub_category_id`, `metric`, `direction`, `target_value`, " +
                            "`created_at`, `start_date` + CASE `period` " +
                            "WHEN 'MONTH' THEN 2505600000 ELSE 518400000 END " +
                            "FROM `goals`",
                    )
                    database.execSQL("DROP TABLE `goals`")
                    database.execSQL("ALTER TABLE `goals_new` RENAME TO `goals`")
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goals_main_category_id` " +
                            "ON `goals` (`main_category_id`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goals_sub_category_id` " +
                            "ON `goals` (`sub_category_id`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goals_created_at` " +
                            "ON `goals` (`created_at`)",
                    )
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `goalHistory` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`goal_id` INTEGER NOT NULL, " +
                            "`goal_title` TEXT NOT NULL, " +
                            "`metric` TEXT NOT NULL, " +
                            "`direction` TEXT NOT NULL, " +
                            "`target_value` INTEGER NOT NULL, " +
                            "`actual_value` INTEGER NOT NULL, " +
                            "`period_start` INTEGER NOT NULL, " +
                            "`period_end` INTEGER NOT NULL, " +
                            "`is_achieved` INTEGER NOT NULL, " +
                            "`created_at` INTEGER NOT NULL)",
                    )
                    database.execSQL(
                        "INSERT INTO `goalHistory` (" +
                            "`id`, `goal_id`, `goal_title`, `metric`, `direction`, " +
                            "`target_value`, `actual_value`, `period_start`, `period_end`, " +
                            "`is_achieved`, `created_at`) " +
                            "SELECT `id`, `goal_id`, `goal_title`, `metric`, `direction`, " +
                            "`target_value`, `actual_value`, `period_start`, `period_end`, " +
                            "`is_achieved`, `created_at` FROM `goalPeriods`",
                    )
                    database.execSQL("DROP TABLE `goalPeriods`")
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goalHistory_goal_id` " +
                            "ON `goalHistory` (`goal_id`)",
                    )
                    database.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_goalHistory_goal_id_period_start_period_end` " +
                            "ON `goalHistory` (`goal_id`, `period_start`, `period_end`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goalHistory_period_end` " +
                            "ON `goalHistory` (`period_end`)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_goalHistory_is_achieved_period_end` " +
                            "ON `goalHistory` (`is_achieved`, `period_end`)",
                    )
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }

        val MIGRATE_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    database.execSQL(
                        "UPDATE `timeTasks` SET `next_schedule_date` = NULL " +
                            "WHERE `next_schedule_date` IS NOT NULL " +
                            "AND `end_time` <= `next_schedule_date`",
                    )
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }

        /** Migration 19→20: Add locationHistory table for 10-min GPS timeline log */
        val MIGRATE_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.beginTransaction()
                try {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `locationHistory` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`timestamp` INTEGER NOT NULL, " +
                            "`latitude` REAL NOT NULL, " +
                            "`longitude` REAL NOT NULL, " +
                            "`place_name` TEXT, " +
                            "`place_emoji` TEXT, " +
                            "`accuracy_meters` REAL)",
                    )
                    database.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_locationHistory_timestamp` " +
                            "ON `locationHistory` (`timestamp`)",
                    )
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            }
        }

        private val PDD_PREPOPULATE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    val cursor = db.query("SELECT COUNT(*) FROM subCategories WHERE sub_category_name LIKE '%Sleep%'")
                    val hasSleep = if (cursor.moveToFirst()) cursor.getInt(0) > 0 else false
                    cursor.close()
                    if (!hasSleep) {
                        populateSleepRoutinesAndTasks(db)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SchedulesDataBase", "Sleep routine population error", e)
                }
            }

            private fun populateSleepRoutinesAndTasks(db: SupportSQLiteDatabase) {
                // 1. Insert Sleep subCategory
                val subValues = ContentValues().apply {
                    put("main_category_id", 2L)
                    put("sub_category_name", "😴 Deep Sleep (Phone on Charge)")
                    put("sub_description", "- [ ] Put phone on charging\n- [ ] 7 hours restorative deep sleep\n- [ ] Wake refreshed at 5:30 AM")
                }
                val sleepSubId = db.insert("subCategories", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, subValues)

                // 2. Insert sleep tasks into all existing dailySchedules
                val cursor = db.query("SELECT date FROM dailySchedules")
                val dates = mutableListOf<Long>()
                while (cursor.moveToNext()) {
                    dates.add(cursor.getLong(0))
                }
                cursor.close()

                dates.forEach { dateMillis ->
                    // Night block (22:30 -> 23:59)
                    val nightTask = ContentValues().apply {
                        put("key", dateMillis + 2230L)
                        put("daily_schedule_date", dateMillis)
                        put("start_time", dateMillis + 22L * 3600000L + 30L * 60000L)
                        put("end_time", dateMillis + 23L * 3600000L + 59L * 60000L)
                        put("main_category_id", 2L)
                        put("sub_category_id", sleepSubId)
                        put("is_completed", 0)
                        put("priority", "STANDARD")
                        put("is_enable_notification", 1)
                        put("is_consider_in_statistics", 1)
                        put("note", "- [ ] Put phone on charging\n- [ ] Restorative night sleep\n- [ ] Wake refreshed at 5:30 AM")
                    }
                    db.insert("timeTasks", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, nightTask)

                    // Morning block (00:00 -> 05:30)
                    val morningTask = ContentValues().apply {
                        put("key", dateMillis + 530L)
                        put("daily_schedule_date", dateMillis)
                        put("start_time", dateMillis)
                        put("end_time", dateMillis + 5L * 3600000L + 30L * 60000L)
                        put("main_category_id", 2L)
                        put("sub_category_id", sleepSubId)
                        put("is_completed", 0)
                        put("priority", "STANDARD")
                        put("is_enable_notification", 1)
                        put("is_consider_in_statistics", 1)
                        put("note", "- [ ] Deep sleep\n- [ ] Wake up at 5:30 AM feeling refreshed")
                    }
                    db.insert("timeTasks", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, morningTask)
                }
                android.util.Log.d("SchedulesDataBase", "Sleep blocks populated for ${dates.size} days")
            }

            private fun prepopulateWeeklyRoutines(db: SupportSQLiteDatabase) {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }

                fun timeMillis(hour: Int, min: Int): Long {
                    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                    cal.set(java.util.Calendar.MINUTE, min)
                    return cal.timeInMillis
                }

                data class RoutineItem(
                    val title: String,
                    val categoryId: Long,
                    val startHour: Int,
                    val startMin: Int,
                    val endHour: Int,
                    val endMin: Int,
                    val desc: String = "",
                    val repeatDays: List<String>,
                )

                val workDays = listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
                val dayOff = listOf("WEDNESDAY")

                val workDayItems = listOf(
                    RoutineItem("Wake Up, Toilet & Fresh Up", 10, 5, 30, 5, 45, "- [ ] Gentle full-body stretch\n- [ ] Use restroom & wash hands\n- [ ] Splash cold water on face", workDays),
                    RoutineItem("Brush Teeth", 10, 5, 45, 5, 50, "- [ ] Apply toothpaste\n- [ ] Brush thoroughly for 2 minutes\n- [ ] Tongue scrape & rinse", workDays),
                    RoutineItem("Drink 500ml Water", 10, 5, 50, 5, 55, "- [ ] Pour 500ml room-temp water\n- [ ] Sip slowly while sitting", workDays),
                    RoutineItem("Morning Tea & Toast", 11, 5, 55, 6, 10, "- [ ] Brew fresh tea\n- [ ] Toast 2 slices of bread\n- [ ] Mindful breakfast", workDays),
                    RoutineItem("Brisk Walk (35 Mins)", 4, 6, 10, 6, 45, "- [ ] Put on walking shoes\n- [ ] 35 min walk in fresh morning air\n- [ ] Deep breathing", workDays),
                    RoutineItem("BCA IGNOU Morning Sprint (1 Hour)", 1, 6, 45, 7, 45, "- [ ] Open BCA-IGNOU folder on PC\n- [ ] Review syllabus unit or concept\n- [ ] Solve 1 TMA assignment question", workDays),
                    RoutineItem("Bath, Gear & CT 100 Bike Check", 7, 7, 45, 8, 0, "- [ ] Refreshing bath\n- [ ] Put on delivery gear & helmet\n- [ ] Check CT 100 tyre pressure & petrol", workDays),
                    RoutineItem("Amazon Flex Delivery Shift (6h)", 3, 8, 0, 14, 0, "- [ ] Reach delivery station & check-in\n- [ ] Scan block packages & organize in bag\n- [ ] Deliver packages safely across route\n- [ ] Log customer delivery confirmations", workDays),
                    RoutineItem("Return to Station (BTS) & Check-out", 8, 14, 0, 14, 30, "- [ ] Return any undelivered packages\n- [ ] Hand over empty delivery totes\n- [ ] Check out in Amazon Flex app", workDays),
                    RoutineItem("Ride Home on CT 100 (~3 PM Arrival)", 8, 14, 30, 15, 0, "- [ ] Smooth ride back on CT 100\n- [ ] Park bike safely & lock", workDays),
                    RoutineItem("Post-Shift Decompression & Lunch Buffer", 5, 15, 0, 16, 30, "- [ ] Wash hands & face\n- [ ] Eat hearty lunch\n- [ ] Lie down & rest nervous system", workDays),
                    RoutineItem("Cook Fresh Dinner", 11, 19, 0, 19, 45, "- [ ] Chop fresh vegetables\n- [ ] Cook sabji with roti or egg pulao\n- [ ] Keep kitchen counters neat", workDays),
                    RoutineItem("Dinner & 2-Min Sink Reset", 11, 19, 45, 20, 0, "- [ ] Eat dinner mindfully\n- [ ] Wash dinner plates & utensils right away\n- [ ] Wipe sink clean", workDays),
                    RoutineItem("BCA IGNOU Evening Sprint (1.5 Hours)", 1, 20, 0, 21, 30, "- [ ] Practice Python / C / SQL practicals\n- [ ] Work on BCA project / assignments\n- [ ] Update study notes", workDays),
                    RoutineItem("Tomorrow Prep & Wind-down", 5, 21, 30, 22, 30, "- [ ] Lay out delivery clothes\n- [ ] Put phone & power bank to charge\n- [ ] Set alarm & peaceful sleep by 10:30 PM", workDays),
                    RoutineItem("😴 Deep Sleep (Phone on Charge)", 2, 22, 30, 23, 59, "- [ ] Put phone on charging\n- [ ] 7 hours restorative deep sleep\n- [ ] Wake refreshed at 5:30 AM", workDays),
                    RoutineItem("😴 Restful Sleep & Morning Wake-Up", 2, 0, 0, 5, 30, "- [ ] Deep sleep\n- [ ] Gentle wake up at 5:30 AM", workDays),
                )

                val dayOffItems = listOf(
                    RoutineItem("Wake Up, Toilet & Fresh Up", 10, 5, 30, 5, 45, "- [ ] Gentle full-body stretch\n- [ ] Use restroom & wash hands\n- [ ] Splash cold water on face", dayOff),
                    RoutineItem("Brush Teeth", 10, 5, 45, 5, 50, "- [ ] Apply toothpaste\n- [ ] Brush thoroughly for 2 minutes\n- [ ] Tongue scrape & rinse", dayOff),
                    RoutineItem("Drink 500ml Water", 10, 5, 50, 5, 55, "- [ ] Pour 500ml room-temp water\n- [ ] Sip slowly while sitting", dayOff),
                    RoutineItem("Morning Tea & Toast", 11, 5, 55, 6, 10, "- [ ] Brew fresh tea\n- [ ] Toast 2 slices of bread\n- [ ] Mindful breakfast", dayOff),
                    RoutineItem("Brisk Walk (35 Mins)", 4, 6, 10, 6, 45, "- [ ] Put on walking shoes\n- [ ] 35 min walk in fresh morning air\n- [ ] Deep breathing", dayOff),
                    RoutineItem("BCA IGNOU Morning Sprint (1 Hour)", 1, 6, 45, 7, 45, "- [ ] Open BCA-IGNOU folder on PC\n- [ ] Review syllabus unit or concept\n- [ ] Solve 1 TMA assignment question", dayOff),
                    RoutineItem("Bath & Morning Prep", 7, 7, 45, 8, 0, "- [ ] Refreshing bath\n- [ ] Feel clean and fresh for the day", dayOff),
                    RoutineItem("🧺 Wash All Clothes & Full Laundry Chore", 7, 8, 0, 9, 30, "- [ ] Separate delivery clothes, towels & darks\n- [ ] Add detergent and hand/machine wash\n- [ ] Rinse thoroughly\n- [ ] Hang outside in bright sunlight", dayOff),
                    RoutineItem("✂️ Nail Cutting & Personal Grooming", 7, 9, 30, 10, 0, "- [ ] Trim fingernails neatly\n- [ ] Trim toenails\n- [ ] Smooth rough edges with filer\n- [ ] Apply moisturizer / oil", dayOff),
                    RoutineItem("🚽 Bathroom & Toilet Cleaning Deep Reset", 7, 10, 0, 10, 45, "- [ ] Apply toilet bowl cleaner & let sit\n- [ ] Scrub toilet bowl & rim with brush\n- [ ] Scrub bathroom floor tiles\n- [ ] Clean washbasin, tap & mirror\n- [ ] Rinse and mop dry", dayOff),
                    RoutineItem("🍳 Kitchen Deep Cleaning & Counter Reset", 7, 10, 45, 11, 30, "- [ ] Clean gas stove & burner top\n- [ ] Wipe kitchen counter & tiles\n- [ ] Wash accumulated pots & pans\n- [ ] Reorganize spice jars & wipe shelves\n- [ ] Empty kitchen trash bin", dayOff),
                    RoutineItem("🏍️ CT 100 Motorcycle Maintenance & Clean", 7, 11, 30, 12, 15, "- [ ] Wipe motorcycle body, tank & headlight\n- [ ] Clean and lubricate drive chain\n- [ ] Check engine oil level (20W-50)\n- [ ] Check tyre pressures & brake play\n- [ ] Check horn, indicator & brake light", dayOff),
                    RoutineItem("🎓 Deep BCA IGNOU Midday Sprint", 1, 12, 15, 13, 30, "- [ ] Focused reading on difficult topics\n- [ ] Write 2-3 TMA assignment questions\n- [ ] Run code practicals", dayOff),
                    RoutineItem("🍲 Cook Wholesome Lunch & Kitchen Reset", 11, 13, 30, 14, 30, "- [ ] Cook delicious meal (parathas / pulao / sabji)\n- [ ] Eat peacefully\n- [ ] Clean cookware and wipe counter", dayOff),
                    RoutineItem("🛋️ Guilt-Free Afternoon Nap & Deep Rest", 5, 14, 30, 16, 30, "- [ ] Darken room & put phone on DND\n- [ ] Full nervous system deep recovery", dayOff),
                    RoutineItem("👕 Collect & Fold Fresh Dry Clothes", 7, 16, 30, 17, 0, "- [ ] Take dry clothes off clothesline\n- [ ] Fold delivery clothes neatly\n- [ ] Stack and store in wardrobe", dayOff),
                    RoutineItem("☕ Evening Chai & Personal Time", 5, 17, 0, 18, 0, "- [ ] Brew hot cardamom chai\n- [ ] Call with Raja Ji\n- [ ] Listen to favorite tracks & unwind", dayOff),
                    RoutineItem("💻 BCA IGNOU Second Focus Review (1.5 Hours)", 1, 18, 0, 19, 30, "- [ ] Review assignment write-ups\n- [ ] Double-check question numbers & covers\n- [ ] Plan upcoming study goals", dayOff),
                    RoutineItem("🍽️ Light Dinner & Sink Clear", 11, 19, 30, 20, 30, "- [ ] Light meal\n- [ ] Clean plates & leave sink spotless", dayOff),
                    RoutineItem("🎬 Movie / Gaming / Leisure Time", 9, 20, 30, 21, 30, "- [ ] Watch thriller/movie or play favorite game\n- [ ] Zero stress and total relaxation", dayOff),
                    RoutineItem("🌙 Tomorrow Prep & Wind-down", 5, 21, 30, 22, 30, "- [ ] Pack clean delivery clothes for Thursday\n- [ ] Phone & power bank on charger\n- [ ] Wind down for 10:30 PM sleep", dayOff),
                    RoutineItem("😴 Deep Sleep (Phone on Charge)", 2, 22, 30, 23, 59, "- [ ] Put phone on charging\n- [ ] 7 hours restorative deep sleep\n- [ ] Wake refreshed at 5:30 AM", dayOff),
                    RoutineItem("😴 Restful Sleep & Morning Wake-Up", 2, 0, 0, 5, 30, "- [ ] Deep sleep\n- [ ] Gentle wake up at 5:30 AM", dayOff),
                )

                val allRoutines = workDayItems + dayOffItems

                db.execSQL("DELETE FROM repeatTimes")
                db.execSQL("DELETE FROM timeTaskTemplates")
                db.execSQL("DELETE FROM timeTasks WHERE linked_template_id IS NOT NULL")

                allRoutines.forEach { item ->
                    val subValues = ContentValues().apply {
                        put("main_category_id", item.categoryId)
                        put("sub_category_name", item.title)
                        put("sub_description", item.desc)
                    }
                    val subId = db.insert("subCategories", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, subValues)
                    val actualSubId = if (subId > 0) subId else {
                        val cur = db.query("SELECT id FROM subCategories WHERE main_category_id = ? AND sub_category_name = ?", arrayOf<String>(item.categoryId.toString(), item.title))
                        val id = if (cur.moveToFirst()) cur.getLong(0) else null
                        cur.close()
                        id
                    }

                    val tmplValues = ContentValues().apply {
                        put("start_time", timeMillis(item.startHour, item.startMin))
                        put("end_time", timeMillis(item.endHour, item.endMin))
                        put("main_category_id", item.categoryId)
                        put("sub_category_id", actualSubId)
                        put("priority", "STANDARD")
                        put("is_enable_notification", 1)
                        put("is_consider_in_statistics", 1)
                        put("repeat_enabled", 1)
                    }
                    val templateId = db.insert("timeTaskTemplates", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, tmplValues)

                    item.repeatDays.forEach { dayName ->
                        val repValues = ContentValues().apply {
                            put("template_id", templateId)
                            put("type", "WEEK_DAY")
                            put("day", dayName)
                        }
                        db.insert("repeatTimes", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, repValues)
                    }
                }
            }
        }

        fun create(context: Context) = Room.databaseBuilder(
            context = context,
            klass = SchedulesDataBase::class.java,
            name = NAME,
        ).createFromAsset("database/categories_prepopulate.db")
            .addCallback(PDD_PREPOPULATE_CALLBACK)
            .addMigrations(MIGRATE_2_3)
            .addMigrations(MIGRATE_4_5)
            .addMigrations(MIGRATE_5_6)
            .addMigrations(MIGRATE_7_8)
            .addMigrations(MIGRATE_15_16)
            .addMigrations(MIGRATE_16_17)
            .addMigrations(MIGRATE_17_18)
            .addMigrations(MIGRATE_18_19)
            .addMigrations(MIGRATE_19_20)
            .build()
    }
}
