package com.workplanner.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.workplanner.app.ui.WorkPlannerApp
import com.workplanner.app.ui.onboarding.OnboardingPreferences
import com.workplanner.app.ui.onboarding.OnboardingScreen
import com.workplanner.app.ui.theme.WorkPlannerTheme

class MainActivity : ComponentActivity() {

    private var taskToOpen by mutableStateOf<String?>(null)
    private var onboardingCompleted by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        taskToOpen = consumeTaskExtra(intent)
        onboardingCompleted = OnboardingPreferences.isCompleted(this)

        setContent {
            WorkPlannerTheme {
                if (onboardingCompleted) {
                    WorkPlannerApp(
                        externalTaskId = taskToOpen,
                        onExternalTaskHandled = { taskToOpen = null }
                    )
                } else {
                    OnboardingScreen(
                        onFinished = {
                            OnboardingPreferences.markCompleted(this)
                            onboardingCompleted = true
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        taskToOpen = consumeTaskExtra(intent)
    }

    private fun consumeTaskExtra(intent: Intent?): String? {
        val id = intent?.getStringExtra(EXTRA_TASK_ID)
        if (id != null) intent.removeExtra(EXTRA_TASK_ID)
        return id
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_open_task_id"
    }
}
