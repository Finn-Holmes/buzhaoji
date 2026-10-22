package com.kexu.app.reminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Writes are synchronous and tiny; delivery/reconciliation callers run on Dispatchers.IO. */
internal class ReminderStore(context: Context) {
    private val preferences = context.getSharedPreferences("reminder_occurrences_v2", Context.MODE_PRIVATE)

    fun records(): List<ReminderRecord> = synchronized(lock) { decode(preferences.getString("records", "[]") ?: "[]") }
    fun revision(): Long = synchronized(lock) { preferences.getLong("revision", 0) }

    fun replace(records: List<ReminderRecord>) = synchronized(lock) {
        check(preferences.edit().putString("records", encode(records))
            .putLong("revision", preferences.getLong("revision", 0) + 1).commit()) { "Unable to persist reminders" }
    }

    companion object {
        internal val lock = Any()

        internal fun encode(records: List<ReminderRecord>): String = JSONArray().apply {
            records.forEach { record -> put(JSONObject().apply {
                put("key", record.key); put("action", record.action); put("entityId", record.entityId)
                put("triggerAt", record.triggerAt); put("expiresAt", record.expiresAt)
                put("source", record.source); put("title", record.title); put("generation", record.generation)
                put("dueAt", record.dueAt); put("location", record.location); put("occurrenceDay", record.occurrenceDay)
                put("snoozed", record.snoozed); put("deliveredAt", record.deliveredAt ?: JSONObject.NULL)
            }) }
        }.toString()

        internal fun decode(value: String): List<ReminderRecord> = runCatching {
            val array = JSONArray(value)
            (0 until array.length()).mapNotNull { index -> runCatching {
                val item = array.getJSONObject(index)
                ReminderRecord(item.getString("key"), item.getString("action"), item.getLong("entityId"),
                    item.getLong("triggerAt"), item.getLong("expiresAt"), item.getString("source"),
                    item.getString("title"), item.getString("generation"), item.optLong("dueAt"),
                    item.optString("location"), item.optLong("occurrenceDay"), item.optBoolean("snoozed"),
                    if (item.isNull("deliveredAt")) null else item.getLong("deliveredAt"))
            }.getOrNull() }
        }.getOrDefault(emptyList())
    }
}

data class ReminderDeliveryEvent(val scheduledAt: Long, val submittedAt: Long, val result: String, val title: String)

object ReminderDeliveryLog {
    fun latest(context: Context): ReminderDeliveryEvent? {
        val preferences = context.getSharedPreferences("reminder_delivery_log", Context.MODE_PRIVATE)
        if (!preferences.contains("submittedAt")) return null
        return ReminderDeliveryEvent(preferences.getLong("scheduledAt", 0), preferences.getLong("submittedAt", 0),
            preferences.getString("result", "").orEmpty(), preferences.getString("title", "").orEmpty())
    }

    internal fun record(context: Context, scheduledAt: Long, title: String, result: String) {
        context.getSharedPreferences("reminder_delivery_log", Context.MODE_PRIVATE).edit()
            .putLong("scheduledAt", scheduledAt).putLong("submittedAt", System.currentTimeMillis())
            .putString("title", title).putString("result", result).apply()
    }
}
