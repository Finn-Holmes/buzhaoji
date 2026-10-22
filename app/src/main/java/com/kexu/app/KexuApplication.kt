package com.kexu.app

import android.app.Application
import com.kexu.app.data.KexuDatabase
import com.kexu.app.data.KexuRepository
import com.kexu.app.reminder.NotificationChannels
import com.kexu.app.reminder.ReminderRefreshWorker
import com.kexu.app.settings.SettingsRepository

class KexuApplication : Application() {
    val database by lazy { KexuDatabase.get(this) }
    val repository by lazy { KexuRepository(database.taskDao(), database.courseDao(), database.featureDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        ReminderRefreshWorker.enqueue(this)
    }
}
