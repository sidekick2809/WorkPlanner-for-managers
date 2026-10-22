package com.workplanner.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workplanner.app.data.RepositoryProvider

/** Fires at the scheduled time and turns the alarm into a notification. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val task = RepositoryProvider.get(context).task(taskId) ?: return
        if (task.isDone) return
        ReminderNotifications.createChannel(context)
        ReminderNotifications.show(context, task)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
