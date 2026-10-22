package com.kexu.app.reminder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.kexu.app.KexuApplication
import com.kexu.app.data.model.TaskEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Debug-only driver: exercise production scheduling with explicitly owned, real task rows. */
class ReminderQaActivity : ComponentActivity() {
    private val app get() = application as KexuApplication
    private val prefs get() = getSharedPreferences(PREFS, MODE_PRIVATE)
    private lateinit var reportView: TextView
    private var operationResult = "等待操作"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportView = TextView(this).apply {
            textSize = 15f
            setTextIsSelectable(true)
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding * 2, padding, padding)
        }
        setContentView(ScrollView(this).apply { addView(reportView) })
        operationResult = prefs.getString("last_result", "只读状态，未修改任何数据").orEmpty()
        // Configuration recreation must never create a second task from the original extras.
        if (savedInstanceState == null) handleCommand(Intent(intent))
        lifecycleScope.launch {
            while (isActive) {
                refreshReport()
                delay(1_000)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCommand(Intent(intent))
    }

    private fun handleCommand(command: Intent) {
        lifecycleScope.launch {
            try {
                operationResult = withContext(Dispatchers.IO) {
                    commandMutex.withLock { performCommand(command) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                operationResult = "失败：${failure.javaClass.simpleName}: ${failure.message}"
                Log.e(TAG, operationResult, failure)
            }
            if (!command.getBooleanExtra("cleanup", false)) {
                prefs.edit().putString("last_result", operationResult).apply()
            }
            refreshReport()
            Log.i(TAG, reportView.text.toString())
            if (command.getBooleanExtra("finish", false)) finish()
        }
    }

    private suspend fun performCommand(command: Intent): String {
        val scheduleDelay = positiveSeconds(command, "schedule_delay_seconds")
        val snoozeDelay = positiveSeconds(command, "snooze_delay_seconds")
        val cleanup = command.getBooleanExtra("cleanup", false)
        val complete = command.getBooleanExtra("complete", false)
        val reschedule = command.getBooleanExtra("reschedule", false)
        val actions = listOf(scheduleDelay != null, snoozeDelay != null, cleanup, complete, reschedule)
        require(actions.count { it } <= 1) { "一次只传入一个操作，避免混淆测试结果" }
        val scheduler = ReminderScheduler(this)
        return when {
            cleanup -> cleanupOwnedTasks(scheduler)
            scheduleDelay != null -> {
                val now = System.currentTimeMillis()
                val token = UUID.randomUUID().toString()
                val task = TaskEntity(
                    title = "[Reminder QA] ${token.take(8)}",
                    notes = ownershipNotes(token),
                    dueAt = now + 3_600_000L,
                    remindAt = now + scheduleDelay * 1_000L,
                )
                val id = app.repository.saveTask(task)
                val ids = prefs.getStringSet("owned_ids", emptySet()).orEmpty().toMutableSet().apply { add(id.toString()) }
                check(prefs.edit().putStringSet("owned_ids", ids).putString("token_$id", token)
                    .putLong("current_id", id).putLong("planned_at", task.remindAt!!).commit()) {
                    "无法保存 QA 所有权记录；新建任务 ID=$id"
                }
                scheduler.scheduleTask(task.copy(id = id))
                "已经生产调度入口安排 QA 任务 $id（${scheduleDelay} 秒后）"
            }
            snoozeDelay != null -> {
                val task = currentOwnedTask()
                check(!task.isCompleted) { "QA 任务已完成，不能稍后提醒" }
                val triggerAt = System.currentTimeMillis() + snoozeDelay * 1_000L
                scheduler.scheduleTask(task, overrideAt = triggerAt)
                prefs.edit().putLong("planned_at", triggerAt).commit()
                "已稍后提醒 QA 任务 ${task.id}（${snoozeDelay} 秒后）；原任务 remindAt 保留"
            }
            complete -> {
                val task = currentOwnedTask()
                if (!task.isCompleted) {
                    check(app.repository.updateTaskIfUnchanged(
                        task, task.copy(isCompleted = true, completedAt = System.currentTimeMillis()),
                    )) { "QA 任务已并发变化，本次未覆盖" }
                }
                scheduler.cancelTask(task.id)
                NotificationManagerCompat.from(this).cancel(ReminderScheduler.taskRequestCode(task.id))
                "已完成并取消 QA 任务 ${task.id} 的提醒"
            }
            reschedule -> {
                ReminderCoordinator(this, app.repository, app.settingsRepository).rescheduleAll()
                "已调用生产 ReminderCoordinator.rescheduleAll"
            }
            else -> prefs.getString("last_result", "只读状态，未修改任何数据").orEmpty()
        }
    }

    private suspend fun currentOwnedTask(): TaskEntity {
        val id = prefs.getLong("current_id", 0L)
        check(id > 0 && id.toString() in prefs.getStringSet("owned_ids", emptySet()).orEmpty()) {
            "没有当前 QA 任务；先用 schedule_delay_seconds 创建"
        }
        val task = checkNotNull(app.repository.task(id)) { "QA 任务 $id 已不存在" }
        check(isOwned(task)) { "任务 $id 所有权标记不匹配，拒绝操作" }
        return task
    }

    private fun isOwned(task: TaskEntity): Boolean {
        val token = prefs.getString("token_${task.id}", null) ?: return false
        return task.notes == ownershipNotes(token) && task.title.startsWith("[Reminder QA]")
    }

    private suspend fun cleanupOwnedTasks(scheduler: ReminderScheduler): String {
        val ids = prefs.getStringSet("owned_ids", emptySet()).orEmpty().mapNotNull(String::toLongOrNull)
        // Validate every extant row before any cleanup, and never discover tasks by a title scan.
        val tasks = ids.associateWith { app.repository.task(it) }
        check(tasks.values.filterNotNull().all(::isOwned)) { "有任务所有权标记不匹配，已停止清理" }
        ids.forEach { id ->
            scheduler.cancelTask(id)
            NotificationManagerCompat.from(this).cancel(ReminderScheduler.taskRequestCode(id))
            tasks[id]?.let { app.repository.deleteTask(it) }
            app.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM task_completions WHERE taskId = ?", arrayOf(id),
            )
        }
        check(prefs.edit().clear().commit()) { "QA 任务已删除，清理偏好失败" }
        return "已清理本入口的 ${ids.size} 个 QA 任务、提醒、通知及完成记录"
    }

    private suspend fun refreshReport() {
        val report = withContext(Dispatchers.IO) {
            val id = prefs.getLong("current_id", 0L)
            val task = if (id > 0L) app.repository.task(id) else null
            val plan = ReminderStore(this@ReminderQaActivity).records().firstOrNull { it.key == "task:$id" }
            buildString {
                appendLine("Reminder QA · debug only")
                appendLine()
                appendLine("设备时间：${formatTime(System.currentTimeMillis())}")
                appendLine("QA ID：${id.takeIf { it > 0L } ?: "无"}")
                appendLine("名称：${task?.title ?: "无"}")
                appendLine("任务原提醒：${formatTime(task?.remindAt)}")
                appendLine("最近请求时间：${formatTime(prefs.getLong("planned_at", 0L))}")
                appendLine("持久计划时间：${formatTime(plan?.triggerAt)}")
                appendLine("计划版本：${plan?.generation ?: "无"}")
                appendLine("稍后提醒：${plan?.snoozed ?: false}")
                appendLine("计划投递标记：${formatTime(plan?.deliveredAt)}")
                appendLine("完成状态：${task?.isCompleted ?: "无"}")
                appendLine("通知许可：${NotificationManagerCompat.from(this@ReminderQaActivity).areNotificationsEnabled()}")
                appendLine()
                appendLine("操作结果：$operationResult")
                appendLine()
                appendLine("最近投递：")
                appendLine(latestDeliverySummary())
                appendLine()
                appendLine("参数：schedule_delay_seconds / snooze_delay_seconds（正秒数）")
                appendLine("或 reschedule / complete / cleanup = true")
                appendLine("可附 finish=true；重复启动无需 force-stop。")
            }
        }
        reportView.text = report
    }

    private fun latestDeliverySummary(): String {
        val event = ReminderDeliveryLog.latest(this) ?: return "尚无记录"
        return buildString {
            appendLine("名称：${event.title}")
            appendLine("计划投递：${formatTime(event.scheduledAt)}")
            appendLine("提交系统：${formatTime(event.submittedAt)}")
            append("结果：${event.result}")
        }
    }

    private fun positiveSeconds(intent: Intent, key: String): Long? {
        if (!intent.hasExtra(key)) return null
        val seconds = (intent.extras?.get(key) as? Number)?.toLong()
            ?: intent.getStringExtra(key)?.toLongOrNull()
            ?: error("$key 必须是整数秒数")
        require(seconds in 1L..86_400L) { "$key 必须在 1–86400 秒之间" }
        return seconds
    }

    private fun formatTime(millis: Long?): String = millis?.takeIf { it > 0L }?.let {
        "${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))} ($it)"
    } ?: "无"

    private companion object {
        const val PREFS = "reminder_qa"
        const val TAG = "ReminderQa"
        val commandMutex = Mutex()
        fun ownershipNotes(token: String) = "reminder-qa-owner:$token"
    }
}
