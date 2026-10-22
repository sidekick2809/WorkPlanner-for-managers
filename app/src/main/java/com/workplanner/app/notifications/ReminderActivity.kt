package com.workplanner.app.notifications

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.workplanner.app.MainActivity
import com.workplanner.app.data.RepositoryProvider
import com.workplanner.app.ui.reminder.ReminderAlarmScreen
import com.workplanner.app.ui.theme.WorkPlannerTheme

/**
 * Target of the full-screen notification intent. It is allowed to appear over
 * the lock screen and turns the display on, so a reminder is impossible to miss.
 */
class ReminderActivity : ComponentActivity() {

    private var taskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()

        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val id = taskId
        if (id != null) ReminderNotifications.cancel(this, id)

        setContent {
            WorkPlannerTheme {
                val task = id?.let { RepositoryProvider.get(this).task(it) }
                if (task == null) {
                    LaunchedEffect(Unit) { finish() }
                    return@WorkPlannerTheme
                }
                val person = RepositoryProvider.get(this).snapshot().person(task.personId)

                ReminderAlarmScreen(
                    task = task,
                    person = person,
                    onOpen = { openTask(task.id) },
                    onSnooze = { snooze(task.id) },
                    onComplete = { complete(task.id) },
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        taskId = newId
        ReminderNotifications.cancel(this, newId)
        recreate()
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openTask(id: String) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_TASK_ID, id)
            }
        )
        finish()
    }

    private fun snooze(id: String) {
        val repository = RepositoryProvider.get(this)
        val task = repository.task(id)
        if (task != null && !task.isDone) {
            val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
            val snoozed = task.copy(reminderAt = at)
            repository.updateTask(snoozed)
            ReminderScheduler(this).schedule(snoozed)
        }
        finish()
    }

    private fun complete(id: String) {
        RepositoryProvider.get(this).setTaskDone(id, true)
        ReminderScheduler(this).cancel(id)
        ReminderNotifications.cancel(this, id)
        finish()
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_reminder_task_id"
        private const val SNOOZE_MINUTES = 10L
    }
}
