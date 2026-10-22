package com.workplanner.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.workplanner.app.data.RepositoryProvider
import com.workplanner.app.data.Task

/**
 * Schedules the reminder of a single task. Exact alarms are used when the OS
 * allows it; otherwise we politely fall back to an inexact wake-up.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(task: Task) {
        val manager = alarmManager ?: return
        val at = task.reminderAt
        if (task.isDone || at == null || at <= System.currentTimeMillis()) {
            cancel(task.id)
            return
        }

        val pendingIntent = pendingIntent(task.id)
        try {
            val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                manager.canScheduleExactAlarms()
            if (canUseExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        }
    }

    fun cancel(taskId: String) {
        alarmManager?.cancel(pendingIntent(taskId))
    }

    /** Used after a reboot so reminders survive the phone being restarted. */
    fun rescheduleAll() {
        RepositoryProvider.get(context).snapshot().tasks
            .filter { !it.isDone && (it.reminderAt ?: 0L) > System.currentTimeMillis() }
            .forEach(::schedule)
    }

    private fun pendingIntent(taskId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
