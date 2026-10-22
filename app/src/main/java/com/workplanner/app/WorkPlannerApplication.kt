package com.workplanner.app

import android.app.Application
import com.workplanner.app.notifications.ReminderNotifications

class WorkPlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
    }
}
