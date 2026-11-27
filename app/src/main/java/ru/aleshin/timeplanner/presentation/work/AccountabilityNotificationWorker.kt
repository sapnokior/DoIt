/*
 * Copyright 2026 Adarsh Kumar Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ru.aleshin.timeplanner.presentation.work

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import ru.aleshin.core.data.datasources.schedules.SchedulesDataBase
import ru.aleshin.core.utils.extensions.startThisDay
import ru.aleshin.timeplanner.presentation.receivers.TaskCheckInReceiver
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * PDD Accountability Worker — runs every 15 minutes (WorkManager minimum).
 *
 * On each run it checks today's schedule for tasks whose end_time just passed
 * (within the last 20 minutes) and whose is_completed is still FALSE.
 *
 * For each such task it fires an interactive check-in notification:
 *   "Adarsh, क्या आपने [Task] कर लिया? ✅ हाँ / ❌ नहीं"
 *
 * If neither button is tapped within [TIMEOUT_MS], an AlarmManager fires a
 * TaskCheckInReceiver.ACTION_TIMEOUT that updates the notification with ❓.
 */
class AccountabilityNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        ensureChannel()

        val now = System.currentTimeMillis()
        val windowStart = now - WINDOW_MS          // 20 min back
        val todayStart = Date().startThisDay().time

        return try {
            val db = SchedulesDataBase.create(appContext)
            val tasks = db.fetchTimeTaskDao()
                .fetchAllTimeTasksDetailsByDate(todayStart)
                .firstOrNull() ?: emptyList()

            tasks.forEach { detail ->
                val task = detail.timeTask
                // Fire check-in notification for tasks that just ended and are still pending
                if (!task.isCompleted &&
                    task.endTime in windowStart..now
                ) {
                    val subName = detail.subCategory?.subCategoryName
                    val mainCatName = detail.mainCategory.mainCategory.customName
                        ?: detail.mainCategory.mainCategory.defaultType?.name
                        ?: "Task"
                    val label = if (!subName.isNullOrBlank()) subName else mainCatName

                    sendAccountabilityNotification(
                        taskKey = task.key,
                        taskLabel = label,
                        notifId = (task.key % Int.MAX_VALUE).toInt(),
                    )
                }
            }

            // Morning nudge at 5:30 AM if tasks exist
            sendMorningNudgeIfNeeded(now, tasks.size)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AccountabilityWorker", "Error checking accountability tasks", e)
            Result.retry()
        }
    }

    private fun sendAccountabilityNotification(taskKey: Long, taskLabel: String, notifId: Int) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val yesIntent = PendingIntent.getBroadcast(
            appContext, notifId * 10 + 1,
            Intent(appContext, TaskCheckInReceiver::class.java).apply {
                action = TaskCheckInReceiver.ACTION_YES
                putExtra(TaskCheckInReceiver.EXTRA_NOTIF_ID, notifId)
                putExtra(TaskCheckInReceiver.EXTRA_TASK_KEY, taskKey)
                putExtra(TaskCheckInReceiver.EXTRA_TASK_LABEL, taskLabel)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val noIntent = PendingIntent.getBroadcast(
            appContext, notifId * 10 + 2,
            Intent(appContext, TaskCheckInReceiver::class.java).apply {
                action = TaskCheckInReceiver.ACTION_NO
                putExtra(TaskCheckInReceiver.EXTRA_NOTIF_ID, notifId)
                putExtra(TaskCheckInReceiver.EXTRA_TASK_KEY, taskKey)
                putExtra(TaskCheckInReceiver.EXTRA_TASK_LABEL, taskLabel)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(appContext, TaskCheckInReceiver.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Adarsh, kya aapne kiya? 🤔")
            .setContentText("\"$taskLabel\" — kya ho gaya?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Adarsh, kya aapne abhi \"$taskLabel\" poora kar liya hai?\n\nJawab dena — aapki support ke liye hai ❤️")
            )
            .addAction(android.R.drawable.checkbox_on_background, "✅ Haan, kar liya", yesIntent)
            .addAction(android.R.drawable.ic_delete, "❌ Nahi", noIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(notifId, notif)

        // Schedule timeout alarm: if no tap within 30 min -> show ❓
        scheduleTimeout(taskKey, taskLabel, notifId)
    }

    private fun scheduleTimeout(taskKey: Long, taskLabel: String, notifId: Int) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeoutIntent = PendingIntent.getBroadcast(
            appContext, notifId * 10 + 3,
            Intent(appContext, TaskCheckInReceiver::class.java).apply {
                action = TaskCheckInReceiver.ACTION_TIMEOUT
                putExtra(TaskCheckInReceiver.EXTRA_NOTIF_ID, notifId)
                putExtra(TaskCheckInReceiver.EXTRA_TASK_KEY, taskKey)
                putExtra(TaskCheckInReceiver.EXTRA_TASK_LABEL, taskLabel)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = System.currentTimeMillis() + TIMEOUT_MS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, timeoutIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, timeoutIntent)
        }
    }

    private fun sendMorningNudgeIfNeeded(
        now: Long,
        scheduledCount: Int,
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        if (hour == 5 && minute in 28..35 && scheduledCount > 0) {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = NotificationCompat.Builder(appContext, TaskCheckInReceiver.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("🌅 Subah ho gayi, Adarsh!")
                .setContentText("Aaj $scheduledCount tasks hain. Chalo ek ek karke shuru karte hain! 💪")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            nm.notify(MORNING_NUDGE_ID, notif)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(TaskCheckInReceiver.CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                TaskCheckInReceiver.CHANNEL_ID,
                "PDD Task Check-In",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Gentle accountability reminders with yes/no check-in buttons"
            },
        )
    }

    companion object {
        private const val WORK_NAME = "DoItPddAccountabilityWorker"
        const val TIMEOUT_MS = 30 * 60 * 1000L   // 30 minutes
        private const val WINDOW_MS = 20 * 60 * 1000L
        private const val MORNING_NUDGE_ID = 999_001

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<AccountabilityNotificationWorker>(
                15, TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
