package com.kexu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReminderPermissionGuide(
    notificationsGranted: Boolean,
    exactRemindersAllowed: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExactReminders: () -> Unit,
    onDismiss: () -> Unit,
) {
    val allReady = notificationsGranted && exactRemindersAllowed
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (allReady) "提醒已就绪" else "设置提醒",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    "系统会在提醒时间唤醒不着急，短暂处理后结束，无需常驻后台或轮询。你可以现在设置，也可以稍后在设置页调整。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                PermissionGuideRow(
                    title = "通知权限",
                    description = "用于显示任务、课程、习惯和纪念日提醒",
                    granted = notificationsGranted,
                )
                Spacer(Modifier.height(10.dp))
                PermissionGuideRow(
                    title = "精确定时",
                    description = "用于按设定时间唤醒；不开启时提醒可能被系统推迟",
                    granted = exactRemindersAllowed,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "无需关闭全局省电功能。若锁屏后仍收不到提醒，可在设置页检查通知类别、应用后台限制和测试记录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = when {
                    !notificationsGranted -> onRequestNotifications
                    !exactRemindersAllowed -> onRequestExactReminders
                    else -> onDismiss
                },
            ) {
                Text(
                    when {
                        !notificationsGranted -> "开启通知"
                        !exactRemindersAllowed -> "设置精确定时"
                        else -> "完成"
                    },
                )
            }
        },
        dismissButton = {
            if (!allReady) {
                TextButton(onClick = onDismiss) {
                    Text("稍后设置")
                }
            }
        },
    )
}

@Composable
private fun PermissionGuideRow(
    title: String,
    description: String,
    granted: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = if (granted) Color(0xFFE0F5EA) else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (granted) Color(0xFF28764B) else MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                if (granted) "已开启" else "未开启",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
