package com.kexu.app.nlp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class ParsedTaskInput(
    val originalText: String,
    val cleanTitle: String,
    val dueAtMillis: Long?,
    val endAtMillis: Long? = null,
    val reminderAtMillis: Long?,
    val recurrenceRule: String?,
    val recognizedTokens: List<String>,
    val hasExplicitDate: Boolean = false,
    val hasExplicitTime: Boolean = false,
)

class ChineseTimeParser(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun parse(text: String, now: LocalDateTime = LocalDateTime.now(zoneId)): ParsedTaskInput {
        val source = text.trim()
        if (source.isEmpty()) {
            return ParsedTaskInput(
                originalText = source,
                cleanTitle = "",
                dueAtMillis = null,
                reminderAtMillis = null,
                recurrenceRule = null,
                recognizedTokens = emptyList(),
            )
        }

        val tokens = mutableListOf<String>()
        var date: LocalDate? = null
        var time: LocalTime? = null

        absoluteDate.find(source)?.let { match ->
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            val thisYear = runCatching { LocalDate.of(now.year, month, day) }.getOrNull()
            date = when {
                thisYear == null -> null
                thisYear.isBefore(now.toLocalDate()) -> thisYear.plusYears(1)
                else -> thisYear
            }
            tokens += match.value
        }

        if (date == null) {
            relativeDay.find(source)?.let { match ->
                date = now.toLocalDate().plusDays(
                    when (match.value) {
                        "今天", "今日" -> 0
                        "明天", "明日" -> 1
                        else -> 2
                    },
                )
                tokens += match.value
            }
        }
        if (date == null) {
            englishRelativeDay.find(source)?.let { match ->
                date = now.toLocalDate().plusDays(
                    when (match.value.lowercase()) {
                        "today" -> 0
                        "tomorrow" -> 1
                        else -> 2
                    },
                )
                tokens += match.value
            }
        }

        var parsedWeekday: DayOfWeek? = null
        weekday.find(source)?.let { match ->
            val target = chineseWeekday(match.groupValues[2])
            parsedWeekday = target
            val nextWeek = match.groupValues[1] == "下周"
            val base = if (nextWeek) {
                now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            } else {
                val today = now.toLocalDate()
                val delta = (target.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(delta.toLong())
            }
            date = if (nextWeek) base.plusDays((target.value - 1).toLong()) else base
            tokens += match.value
        }

        timeExpression.find(source)?.let { match ->
            val period = match.groupValues[1]
            var hour = chineseNumber(match.groupValues[2]) ?: 0
            val minute = chineseNumber(match.groupValues[3]).takeIf { match.groupValues[3].isNotBlank() } ?: 0
            if (period in afternoonPeriods && hour in 1..11) hour += 12
            if (period in eveningPeriods && hour in 1..11) hour += 12
            if (period == "中午" && hour in 1..10) hour += 12
            if (period == "凌晨" && hour == 12) hour = 0
            if (hour in 0..23 && minute in 0..59) {
                time = LocalTime.of(hour, minute)
                tokens += match.value
            }
        }
        if (time == null) {
            englishTimeExpression.find(source)?.let { match ->
                var hour = match.groupValues[1].toInt()
                val minute = match.groupValues[2].ifBlank { "0" }.toInt()
                val period = match.groupValues[3].lowercase()
                if (period == "pm" && hour in 1..11) hour += 12
                if (period == "am" && hour == 12) hour = 0
                if (hour in 0..23 && minute in 0..59) {
                    time = LocalTime.of(hour, minute)
                    tokens += match.value
                }
            }
        }
        if (time == null) {
            dayPeriod.find(source)?.let { match ->
                time = LocalTime.of(defaultHourForPeriod(match.value), 0)
                tokens += match.value
            }
        }

        if (date == null && time != null) {
            date = if (time.isAfter(now.toLocalTime())) now.toLocalDate() else now.toLocalDate().plusDays(1)
        }

        val due = date?.atTime(time ?: LocalTime.of(23, 59))
        var reminder = due.takeIf { time != null }

        advanceReminder.find(source)?.let { match ->
            val amount = chineseNumber(match.groupValues[1]) ?: 0
            reminder = when (match.groupValues[2]) {
                "天" -> due?.minusDays(amount.toLong())
                "小时", "个小时" -> due?.minusHours(amount.toLong())
                else -> due?.minusMinutes(amount.toLong())
            }
            tokens += match.value
        }

        val recurrence = recurringWeek.find(source)?.let { match ->
            tokens += match.value
            val day = chineseWeekday(match.groupValues[1]).value
            "WEEKLY:$day"
        } ?: parsedWeekday?.let { null }

        val clean = tokens
            .distinct()
            .fold(source) { current, token -> current.replace(token, " ") }
            .replace(Regex("[，,。；;]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { source }

        return ParsedTaskInput(
            originalText = source,
            cleanTitle = clean,
            dueAtMillis = due?.atZone(zoneId)?.toInstant()?.toEpochMilli(),
            endAtMillis = null,
            reminderAtMillis = reminder?.atZone(zoneId)?.toInstant()?.toEpochMilli(),
            recurrenceRule = recurrence,
            recognizedTokens = tokens.distinct(),
            hasExplicitDate = date != null && tokens.any { token ->
                relativeDay.matches(token) ||
                    englishRelativeDay.matches(token) ||
                    absoluteDate.matches(token) ||
                    weekday.matches(token)
            },
            hasExplicitTime = time != null,
        )
    }

    private fun chineseWeekday(value: String): DayOfWeek = when (value) {
        "一" -> DayOfWeek.MONDAY
        "二" -> DayOfWeek.TUESDAY
        "三" -> DayOfWeek.WEDNESDAY
        "四" -> DayOfWeek.THURSDAY
        "五" -> DayOfWeek.FRIDAY
        "六" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun chineseNumber(value: String): Int? {
        if (value.isBlank()) return null
        value.toIntOrNull()?.let { return it }
        val digits = mapOf('零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9)
        if ('十' in value) {
            val parts = value.split('十')
            val tens = if (parts.first().isBlank()) 1 else digits[parts.first().first()] ?: return null
            val ones = if (parts.size < 2 || parts[1].isBlank()) 0 else digits[parts[1].first()] ?: return null
            return tens * 10 + ones
        }
        return value.map { digits[it] ?: return null }.fold(0) { total, digit -> total * 10 + digit }
    }

    companion object {
        private val absoluteDate = Regex("(\\d{1,2})月(\\d{1,2})日")
        private val relativeDay = Regex("今天|今日|明天|明日|后天")
        private val englishRelativeDay = Regex("today|tomorrow|day\\s+after\\s+tomorrow", RegexOption.IGNORE_CASE)
        private val weekday = Regex("(下周|周|星期)([一二三四五六日天])")
        private val afternoonPeriods = setOf("下午", "傍晚", "黄昏")
        private val eveningPeriods = setOf("晚上", "晚间", "夜里", "夜间", "今晚")
        private val timeExpression = Regex("(?:(凌晨|清晨|早晨|早上|上午|中午|午间|下午|傍晚|黄昏|晚上|晚间|夜里|夜间|今晚)\\s*)?([零〇一二两三四五六七八九十\\d]{1,3})(?:[:：点时]([零〇一二两三四五六七八九十\\d]{0,3})分?)")
        private val dayPeriod = Regex("凌晨|清晨|早晨|早上|上午|中午|午间|下午|傍晚|黄昏|晚上|晚间|夜里|夜间|今晚")
        private val englishTimeExpression = Regex("(?:at\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)", RegexOption.IGNORE_CASE)
        private val advanceReminder = Regex("提前([零〇一二两三四五六七八九十\\d]+)(天|个小时|小时|分钟)")
        private val recurringWeek = Regex("每周([一二三四五六日天])")

        private fun defaultHourForPeriod(period: String): Int = when (period) {
            "凌晨" -> 2
            "清晨" -> 6
            "早晨", "早上", "上午" -> 8
            "中午", "午间" -> 12
            "下午", "傍晚", "黄昏" -> 18
            "晚上", "晚间", "今晚" -> 20
            else -> 22
        }
    }
}
