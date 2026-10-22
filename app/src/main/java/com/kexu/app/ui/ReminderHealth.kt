package com.kexu.app.ui

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kexu.app.MainActivity
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.reminder.NotificationChannels
import com.kexu.app.reminder.ReminderDeliveryLog
import com.kexu.app.reminder.ReminderReceiver
import com.kexu.app.reminder.ReminderScheduler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ReminderChannelHealth(val id: String, val name: String, val enabled: Boolean)

internal data class ReminderHealth(
    val notificationsAllowed: Boolean,
    val channels: List<ReminderChannelHealth>,
    val exactAllowed: Boolean,
    val batteryExempt: Boolean,
    val backgroundRestricted: Boolean,
    val automaticTime: Boolean?,
    val automaticTimeZone: Boolean?,
) {
    val notificationDisplayAllowed: Boolean get() = notificationsAllowed && channels.all { it.enabled }
    val summary: String get() = when {
        !notificationsAllowed -> "通知未开启"
        channels.any { !it.enabled } -> "部分通知类别已关闭"
        backgroundRestricted -> "后台受到系统限制"
        !exactAllowed -> "精确定时未开启"
        else -> "系统定时提醒已就绪"
    }
}

internal fun readReminderHealth(context: Context): ReminderHealth {
    val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val manager = context.getSystemService(NotificationManager::class.java)
    return ReminderHealth(
        notificationsAllowed = runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled(),
        channels = listOf(
            NotificationChannels.TASKS to "任务",
            NotificationChannels.COURSES to "课程",
            NotificationChannels.HABITS to "习惯",
            NotificationChannels.COUNTDOWNS to "纪念日",
        ).map { (id, name) ->
            val channel = manager.getNotificationChannel(id)
            ReminderChannelHealth(id, name, channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE)
        },
        exactAllowed = ReminderScheduler(context).canScheduleExactReminders(),
        batteryExempt = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
        backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.getSystemService(ActivityManager::class.java).isBackgroundRestricted,
        automaticTime = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1 }.getOrNull(),
        automaticTimeZone = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE) == 1 }.getOrNull(),
    )
}

private const val NOTIFICATION_REQUESTED = "notification_permission_requested"
private const val IMMEDIATE_TEST_TASK_ID = -998L

/** A denied permission can stop producing a dialog; send that case to system settings. */
internal fun shouldPromptForNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) return false
    val asked = context.getSharedPreferences("permission_setup", Context.MODE_PRIVATE).getBoolean(NOTIFICATION_REQUESTED, false)
    return !asked || context.findActivity()?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
    } == true
}

internal fun markNotificationPermissionRequested(context: Context) {
    context.getSharedPreferences("permission_setup", Context.MODE_PRIVATE).edit().putBoolean(NOTIFICATION_REQUESTED, true).apply()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

internal fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

internal fun openReminderSystemSettings(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(appDetailsIntent(context)) }.onFailure {
            Toast.makeText(context, "请在系统设置中打开不着急的应用信息", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
internal fun ReminderSettingsContent(
    defaultCourseReminderMinutes: Int?,
    onDefaultCourseReminderChange: (Int) -> Unit,
    onPermissionStateChanged: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var deviceHelpExpanded by rememberSaveable { mutableStateOf(false) }
    var testStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val health = remember(revision) { readReminderHealth(context) }
    val delivery = remember(revision) { ReminderDeliveryLog.latest(context) }
    val localNow = remember(revision) { Instant.now().atZone(ZoneId.systemDefault()) }
    val scheduler = remember(context) { ReminderScheduler(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        revision++
        onPermissionStateChanged()
        // Runtime permission dialogs do not always cause an Activity resume.
        (context.findActivity() as? MainActivity)?.refreshReminderPermissions()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun requestNotifications() {
        if (shouldPromptForNotifications(context)) {
            markNotificationPermissionRequested(context)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openReminderSystemSettings(context, notificationSettingsIntent(context))
        }
    }
    val taskChannelEnabled = health.channels.firstOrNull { it.id == NotificationChannels.TASKS }?.enabled == true
    val canTestNotification = health.notificationsAllowed && taskChannelEnabled

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text("声音、提醒与通知", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ReminderExplanation("按设定时间提醒，尽量减少后台耗电。不同手机的设置名称可能略有区别。")
        }
        item {
            ReminderHealthRow("通知总开关", if (health.notificationsAllowed) "已开启" else "未开启", !health.notificationsAllowed) {
                requestNotifications()
            }
            health.channels.forEach { channel ->
                ReminderHealthRow("${channel.name}通知", if (channel.enabled) "已开启" else "已关闭", !channel.enabled) {
                    openReminderSystemSettings(context, Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, channel.id))
                }
            }
            TextButton(onClick = { openReminderSystemSettings(context, notificationSettingsIntent(context)) }) {
                Text("打开系统通知设置")
            }
        }
        item {
            HorizontalDivider()
            ReminderHealthRow("精确定时", if (health.exactAllowed) "已开启" else "未开启", !health.exactAllowed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    openReminderSystemSettings(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            }
            ReminderExplanation(if (health.exactAllowed) "系统按提醒时间唤醒；厂商限制、强行停止等仍会影响派发。"
                else "未开启时使用系统非精确定时，可能明显延迟。可以继续使用，或点击上方开启“闹钟和提醒”。")
            ReminderHealthRow("电池优化", if (health.batteryExempt) "已豁免" else "系统优化中") {
                val action = if (health.batteryExempt) Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    else Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                val intent = Intent(action).apply {
                    if (!health.batteryExempt) data = Uri.parse("package:${context.packageName}")
                }
                openReminderSystemSettings(context, intent)
            }
            ReminderHealthRow("系统后台限制", if (health.backgroundRestricted) "受限制" else "未检测到限制", health.backgroundRestricted) {
                openReminderSystemSettings(context, appDetailsIntent(context))
            }
            ReminderExplanation("电池豁免是可选项，并不代表获得永久后台权限，也无法判断厂商的全部限制。无需关闭全局省电功能。")
            TextButton(onClick = { deviceHelpExpanded = !deviceHelpExpanded }) {
                Text(if (deviceHelpExpanded) "收起后台提醒检查" else "后台提醒检查")
            }
            AnimatedVisibility(deviceHelpExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReminderExplanation("打开应用信息，检查电池或后台使用设置，允许不着急在后台活动。不同手机可能显示为“不受限制”“允许后台运行”或其他名称。")
                    ReminderExplanation("如果系统提供自启动、应用启动管理或最近任务锁定，可在后台测试仍失败时调整。无需关闭整台手机的省电功能。")
                    ReminderExplanation("通知栏里有提醒却没有声音或横幅时，请检查对应通知类别的声音、横幅、锁屏显示，以及系统的勿扰模式。")
                    ReminderExplanation("强行停止应用会停止系统派发，应用无法绕过；重新打开后会恢复登记。关闭应用页面与强行停止不同。")
                }
            }
            TextButton(onClick = { openReminderSystemSettings(context, appDetailsIntent(context)) }) { Text("打开应用信息") }
        }
        item {
            HorizontalDivider()
            Text("测试提醒", Modifier.padding(top = 14.dp, bottom = 6.dp), fontWeight = FontWeight.SemiBold)
            if (!canTestNotification) {
                ReminderExplanation("请先开启通知总开关和任务通知类别，再测试通知显示。")
            }
            Button(
                enabled = canTestNotification && !testing,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!testing) {
                        testing = true
                        lifecycleOwner.lifecycleScope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        // Separate notification IDs prevent the immediate test from
                                        // making a later timed test a silent onlyAlertOnce update.
                                        NotificationManagerCompat.from(context).cancel(ReminderScheduler.taskRequestCode(IMMEDIATE_TEST_TASK_ID))
                                        ReminderReceiver.showReminder(context, Intent(ReminderScheduler.ACTION_TASK)
                                            .putExtra(ReminderScheduler.EXTRA_TASK_ID, IMMEDIATE_TEST_TASK_ID)
                                            .putExtra(ReminderScheduler.EXTRA_TITLE, "不着急 · 通知显示测试"))
                                    }
                                }
                                testStatus = result.fold(
                                    onSuccess = { submitted -> if (submitted)
                                        "已提交系统通知，请查看通知栏。这项测试不验证锁屏后的定时唤醒。"
                                        else "通知未提交，请检查通知总开关、任务通知类别及下方派发记录。" },
                                    onFailure = { "未能提交测试通知，请检查系统设置后重试。" },
                                )
                            } finally {
                                testing = false
                                revision++
                            }
                        }
                    }
                },
            ) { Text("立即测试通知显示") }
            OutlinedButton(
                enabled = canTestNotification && !testing,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!testing) {
                        testing = true
                        lifecycleOwner.lifecycleScope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val triggerAt = System.currentTimeMillis() + 30_000L
                                        NotificationManagerCompat.from(context).cancel(ReminderScheduler.taskRequestCode(ReminderScheduler.TEST_TASK_ID))
                                        scheduler.scheduleTask(TaskEntity(id = ReminderScheduler.TEST_TASK_ID,
                                            title = ReminderScheduler.TEST_TASK_TITLE, remindAt = triggerAt))
                                        triggerAt to scheduler.canScheduleExactReminders()
                                    }
                                }
                                testStatus = result.fold(
                                    onSuccess = { (triggerAt, exactAllowed) ->
                                        val time = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                        "已登记 $time 的测试。可返回桌面或锁屏等待，请勿强行停止应用。" +
                                            if (exactAllowed) "" else " 当前未开启精确定时，派发可能晚于该时间。"
                                    },
                                    onFailure = { "测试未登记成功，请重试并查看系统权限状态。" },
                                )
                            } finally {
                                testing = false
                                revision++
                            }
                        }
                    }
                },
            ) { Text("30 秒后台定时测试") }
            testStatus?.let { ReminderExplanation(it) }
            Spacer(Modifier.height(8.dp))
            Text("最近一次派发", style = MaterialTheme.typography.labelLarge)
            if (delivery == null) {
                ReminderExplanation("还没有派发记录。")
            } else {
                ReminderExplanation("${delivery.title}\n计划 ${reminderLocalTime(delivery.scheduledAt)}\n处理 ${reminderLocalTime(delivery.submittedAt)} · ${delivery.result}")
                ReminderExplanation("记录反映应用的处理结果，不代表用户已看到通知。")
            }
            TextButton(onClick = { revision++ }) { Text("刷新诊断") }
        }
        item {
            HorizontalDivider()
            Text("设备与时间", Modifier.padding(top = 14.dp, bottom = 6.dp), fontWeight = FontWeight.SemiBold)
            ReminderExplanation("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
            ReminderExplanation(localNow.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss")) +
                "\n${localNow.zone.id} · ${if (localNow.offset.totalSeconds == 0) "UTC" else "UTC${localNow.offset.id}"}")
            ReminderExplanation("自动时间：${automaticSettingText(health.automaticTime)}  ·  自动时区：${automaticSettingText(health.automaticTimeZone)}")
            ReminderExplanation("提醒以手机本地时间为准。时间不正确时请在系统中检查；不着急不会自行联网校时。")
            TextButton(onClick = { openReminderSystemSettings(context, Intent(Settings.ACTION_DATE_SETTINGS)) }) { Text("打开日期与时间设置") }
        }
        item {
            HorizontalDivider()
            Text("新课程默认提前提醒", Modifier.padding(top = 14.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 5, 10, 15, 30).forEach { minutes ->
                    FilterChip(selected = defaultCourseReminderMinutes == minutes, onClick = { onDefaultCourseReminderChange(minutes) },
                        label = { Text(if (minutes == 0) "准时" else "$minutes 分") })
                }
            }
        }
    }
}

private fun automaticSettingText(value: Boolean?): String = when (value) {
    true -> "开启"
    false -> "关闭"
    null -> "无法读取"
}

private fun reminderLocalTime(millis: Long): String = if (millis <= 0L) "即时 / 未记录" else
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M月d日 HH:mm:ss"))

@Composable
private fun ReminderExplanation(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 3.dp))
}

@Composable
private fun ReminderHealthRow(title: String, status: String, needsAttention: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("$status  ›", color = if (needsAttention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
    }
}
