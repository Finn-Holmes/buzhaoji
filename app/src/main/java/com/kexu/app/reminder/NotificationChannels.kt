package com.kexu.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val TASKS = "task_reminders"
    const val COURSES = "course_reminders"
    const val HABITS = "habit_reminders"
    const val COUNTDOWNS = "countdown_reminders"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(TASKS, "任务提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "作业、考试和普通任务的到期提醒"
                    enableVibration(true)
                },
                NotificationChannel(COURSES, "上课提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "课程开始前的提醒"
                    enableVibration(true)
                },
                NotificationChannel(HABITS, "习惯提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "每日习惯打卡提醒"
                    enableVibration(true)
                },
                NotificationChannel(COUNTDOWNS, "纪念日提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "倒数日和纪念日提醒"
                    enableVibration(true)
                },
            ),
        )
    }
}
