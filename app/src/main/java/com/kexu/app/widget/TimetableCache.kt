package com.kexu.app.widget

import android.util.AtomicFile
import android.util.Log
import androidx.room.withTransaction
import com.kexu.app.BuildConfig
import com.kexu.app.KexuApplication
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.reminder.CourseOccurrence
import com.kexu.app.reminder.ScheduleCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

internal data class TimetableData(
    val semesterStart: Long,
    val courses: List<CourseEntity>,
    val slots: List<ScheduleSlotEntity>,
    val overrides: List<CourseOverrideEntity>,
)

/** Immutable source data; only the bounded, derived week cache is mutable. */
internal class TimetableSnapshot(val data: TimetableData, val zone: ZoneId) {
    private val slotsByCourse = data.slots.groupBy { it.courseId }
    private val overridesBySlot = data.overrides.groupBy { it.slotId }
    private val weeks = linkedMapOf<Long, List<CourseOccurrence>>()

    @Synchronized fun week(start: LocalDate): List<CourseOccurrence> {
        val key = start.toEpochDay()
        weeks[key]?.let { return it }
        val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = start.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val result = data.courses.flatMap { course ->
            slotsByCourse[course.id].orEmpty().flatMap { slot ->
                ScheduleCalculator.occurrences(course, slot, data.semesterStart, from - 1,
                    days = 7, zoneId = zone, overrides = overridesBySlot[slot.id].orEmpty())
            }
        }.filter { it.startsAtMillis >= from && it.startsAtMillis < until }
        if (weeks.size >= 11) weeks.remove(weeks.keys.first())
        weeks[key] = result
        return result
    }

    fun warmAround(start: LocalDate) {
        for (offset in -2L..2L) week(start.plusWeeks(offset))
    }

    @Synchronized fun encode(): String = JSONObject()
        .put("version", 1).put("zone", zone.id).put("semester", data.semesterStart)
        .put("courses", JSONArray(data.courses.map(::courseJson)))
        .put("slots", JSONArray(data.slots.map(::slotJson)))
        .put("overrides", JSONArray(data.overrides.map { o -> JSONArray(listOf(
            o.id, o.slotId, o.weekNumber, o.isCancelled, o.dayOfWeek, o.startMinutes,
            o.endMinutes, o.location, o.note)) }))
        .put("weeks", JSONArray(weeks.map { (day, entries) -> JSONObject()
            .put("day", day).put("entries", JSONArray(entries.map { o -> JSONArray(listOf(
                courseJson(o.course), slotJson(o.slot), o.startsAtMillis, o.reminderAtMillis)) })) }))
        .toString()

    companion object {
        fun decode(text: String, zone: ZoneId): TimetableSnapshot {
            val json = JSONObject(text)
            require(json.getInt("version") == 1 && json.getString("zone") == zone.id)
            val data = TimetableData(json.getLong("semester"),
                json.getJSONArray("courses").objects { course(it as JSONArray) },
                json.getJSONArray("slots").objects { slot(it as JSONArray) },
                json.getJSONArray("overrides").objects {
                    val a = it as JSONArray
                    CourseOverrideEntity(a.getLong(0), a.getLong(1), a.getInt(2), a.getBoolean(3),
                        a.nullInt(4), a.nullInt(5), a.nullInt(6),
                        if (a.isNull(7)) null else a.getString(7), a.getString(8))
                })
            return TimetableSnapshot(data, zone).apply {
                json.getJSONArray("weeks").objects {
                    val w = it as JSONObject
                    require(weeks.size < 11)
                    weeks[w.getLong("day")] = w.getJSONArray("entries").objects { entry ->
                        val a = entry as JSONArray
                        CourseOccurrence(course(a.getJSONArray(0)), slot(a.getJSONArray(1)), a.getLong(2), a.getLong(3))
                    }
                }
            }
        }

        private fun courseJson(c: CourseEntity) = JSONArray(listOf(c.id, c.name, c.teacher,
            c.location, c.colorArgb, c.startWeek, c.endWeek, c.weekPattern, c.weekMask, c.reminderMinutes))
        private fun course(a: JSONArray) = CourseEntity(a.getLong(0), a.getString(1), a.getString(2),
            a.getString(3), a.getLong(4), a.getInt(5), a.getInt(6), a.getString(7), a.getLong(8), a.getInt(9))
        private fun slotJson(s: ScheduleSlotEntity) = JSONArray(listOf(s.id, s.courseId, s.dayOfWeek, s.startMinutes, s.endMinutes))
        private fun slot(a: JSONArray) = ScheduleSlotEntity(a.getLong(0), a.getLong(1), a.getInt(2), a.getInt(3), a.getInt(4))
        private fun JSONArray.nullInt(index: Int) = if (isNull(index)) null else getInt(index)
        private fun <T> JSONArray.objects(read: (Any) -> T): List<T> = (0 until length()).map { read(get(it)) }
    }
}

/** Disk survives process death. Room/settings flows always revalidate the initial snapshot. */
internal object TimetableCache {
    private val lock = Mutex()
    private var memory: TimetableSnapshot? = null
    private var saved: String? = null
    private fun file(app: KexuApplication) = AtomicFile(File(app.cacheDir, "timetable-weeks-v1.json"))

    suspend fun initial(app: KexuApplication): TimetableSnapshot {
        val cached = withContext(Dispatchers.IO) {
            lock.withLock {
                val zone = ZoneId.systemDefault()
                memory?.takeIf { it.zone == zone } ?: runCatching {
                    val text = file(app).openRead().bufferedReader().use { it.readText() }
                    TimetableSnapshot.decode(text, zone).also { memory = it; saved = text }
                }.getOrNull()
            }
        }
        if (BuildConfig.DEBUG) Log.d("TimetableCache", if (cached != null) "initial cache hit" else "initial cache miss")
        return cached ?: refresh(app)
    }

    suspend fun refresh(app: KexuApplication): TimetableSnapshot = withContext(Dispatchers.Default) {
        lock.withLock {
            val settings = app.settingsRepository.snapshot()
            val data = app.database.withTransaction {
                TimetableData(settings.semesterStartMillis, app.repository.currentCourses(),
                    app.repository.currentSlots(), app.repository.currentOverrides())
            }
            installLocked(app, data)
        }
    }

    fun observe(app: KexuApplication) = combine(app.settingsRepository.settings,
        app.repository.courses, app.repository.slots, app.repository.overrides) { settings, courses, slots, overrides ->
        TimetableData(settings.semesterStartMillis, courses, slots, overrides)
    }.distinctUntilChanged().map {
        // Independent Room table flows may briefly emit mixed revisions. Re-read atomically.
        refresh(app)
    }

    private suspend fun installLocked(app: KexuApplication, data: TimetableData): TimetableSnapshot {
        val zone = ZoneId.systemDefault()
        return memory?.takeIf { it.data == data && it.zone == zone } ?: TimetableSnapshot(data, zone).also {
            it.warmAround(ScheduleCalculator.startOfWeek(LocalDate.now(zone)))
            memory = it
            persist(app, it)
        }
    }

    suspend fun warm(app: KexuApplication, snapshot: TimetableSnapshot, start: LocalDate) =
        withContext(Dispatchers.Default) {
            snapshot.warmAround(start)
            lock.withLock {
                // A superseded data revision must never overwrite the fresh disk cache.
                if (memory === snapshot) persist(app, snapshot)
            }
        }

    private suspend fun persist(app: KexuApplication, snapshot: TimetableSnapshot) {
        val text = snapshot.encode()
        if (text == saved) return
        withContext(Dispatchers.IO) {
            val target = file(app)
            val stream = runCatching { target.startWrite() }.getOrNull() ?: return@withContext
            try {
                stream.write(text.toByteArray(Charsets.UTF_8))
                target.finishWrite(stream)
                saved = text
            } catch (e: Exception) {
                target.failWrite(stream)
                // A disposable cache must not prevent displaying the timetable.
                if (BuildConfig.DEBUG) Log.w("TimetableCache", "cache write skipped", e)
            }
        }
    }
}
