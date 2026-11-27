/*
 * Copyright 2026 Adarsh Kumar Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ru.aleshin.timeplanner.presentation.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.aleshin.core.data.datasources.schedules.SchedulesDataBase

/**
 * Receives the user's tap on "✅ हाँ, कर लिया" or "❌ नहीं" action buttons
 * in PDD accountability notifications, then marks the task accordingly.
 *
 * If neither button is tapped within [AccountabilityNotificationWorker.TIMEOUT_MS],
 * a separate alarm fires with ACTION_TIMEOUT which marks the task as ❓.
 */
class TaskCheckInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val taskKey = intent.getLongExtra(EXTRA_TASK_KEY, -1L)
        val taskLabel = intent.getStringExtra(EXTRA_TASK_LABEL) ?: "task"

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_YES -> {
                nm.cancel(notifId)
                showFollowUp(context, nm, notifId, "✅ Shukriya, Adarsh!", "\"$taskLabel\" mark kar diya — ek aur step aage! 💪")
                markTask(context, taskKey, completed = true)
                Log.d("TaskCheckIn", "Task $taskKey marked DONE by user")
            }
            ACTION_NO -> {
                nm.cancel(notifId)
                showFollowUp(context, nm, notifId, "❤️괜찮아, Adarsh", "\"$taskLabel\" baad mein try karo — har din naya mauqa hai.")
                markTask(context, taskKey, completed = false)
                Log.d("TaskCheckIn", "Task $taskKey marked NOT DONE by user")
            }
            ACTION_TIMEOUT -> {
                // User didn't respond — replace notification with ❓ banner
                nm.cancel(notifId)
                showFollowUp(context, nm, notifId, "❓ Pata nahi...", "\"$taskLabel\" — koi jawab nahi aaya. Status ❓ mark kar diya.")
                markTaskTimeout(context, taskKey)
                Log.d("TaskCheckIn", "Task $taskKey timed out — marked UNKNOWN")
            }
        }
    }

    private fun showFollowUp(context: Context, nm: NotificationManager, notifId: Int, title: String, body: String) {
        val followUp = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(notifId + 10000, followUp)
    }

    private fun markTaskTimeout(context: Context, taskKey: Long) {
        if (taskKey < 0) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SchedulesDataBase.create(context)
                db.fetchTimeTaskDao().updateTaskStatusAndNoteById(taskKey, completed = false, note = "❓ Check-in timed out (no response)")
            } catch (e: Exception) {
                Log.e("TaskCheckIn", "DB update failed for key=$taskKey", e)
            }
        }
    }

    private fun markTask(context: Context, taskKey: Long, completed: Boolean) {
        if (taskKey < 0) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SchedulesDataBase.create(context)
                db.fetchTimeTaskDao().updateTaskCompletionById(taskKey, completed)
            } catch (e: Exception) {
                Log.e("TaskCheckIn", "DB update failed for key=$taskKey", e)
            }
        }
    }

    companion object {
        const val ACTION_YES      = "ru.aleshin.timeplanner.TASK_YES"
        const val ACTION_NO       = "ru.aleshin.timeplanner.TASK_NO"
        const val ACTION_TIMEOUT  = "ru.aleshin.timeplanner.TASK_TIMEOUT"
        const val EXTRA_NOTIF_ID  = "notif_id"
        const val EXTRA_TASK_KEY  = "task_key"
        const val EXTRA_TASK_LABEL = "task_label"
        const val CHANNEL_ID      = "pdd_accountability"
    }
}
