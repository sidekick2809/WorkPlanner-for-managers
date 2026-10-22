package com.workplanner.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.workplanner.app.MainActivity
import com.workplanner.app.R
import com.workplanner.app.data.RepositoryProvider
import com.workplanner.app.data.Task

object ReminderNotifications {

    const val CHANNEL_ID = "task_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, task: Task) {
        if (!AppPermissions.canPostNotifications(context)) return

        val repository = RepositoryProvider.get(context)
        val personName = repository.snapshot().person(task.personId)?.name
        val title = if (personName.isNullOrBlank()) {
            context.getString(R.string.notification_title_fallback)
        } else {
            context.getString(R.string.notification_title, personName)
        }
        val body = task.summary.ifBlank {
            context.getString(R.string.notification_body_fallback, task.title)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TASK_ID, task.id)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full-screen intent: the system launches this activity even while the
        // device is locked, which is exactly what a task reminder should do.
        val alarmIntent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderActivity.EXTRA_TASK_ID, task.id)
        }
        val alarmPendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode() * 31 + 7,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.olive_700))
            .setContentTitle(title)
            .setContentText(task.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${task.title}\n$body")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(alarmPendingIntent, true)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call.
        }
    }

    fun cancel(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())
    }

    fun canPostNotifications(context: Context): Boolean =
        AppPermissions.canPostNotifications(context)
}
