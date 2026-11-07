package com.firestorm.newview

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class LLDate(val secondsSinceEpoch: Double) {
    companion object {
        fun now(): LLDate {
            return LLDate(System.currentTimeMillis() / 1000.0)
        }

        fun fromYMD(year: Int, month: Int, day: Int): LLDate {
            val local = LocalDate.of(year, month, day)
            val epochSecs = local.atStartOfDay(ZoneOffset.UTC).toEpochSecond().toDouble()
            return LLDate(epochSecs)
        }
    }

    fun toLocalDate(): LocalDate {
        return LocalDate.ofEpochDay((secondsSinceEpoch / 86400).toLong())
    }
}

object LLDateUtil {

    private val DAYS_PER_MONTH_NOLEAP = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private val DAYS_PER_MONTH_LEAP   = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    private fun daysFromMonth(year: Int, month: Int): Int {
        require(month in 1..12)
        return if (year % 4 == 0 && year % 100 != 0) {
            DAYS_PER_MONTH_LEAP[month - 1]
        } else {
            DAYS_PER_MONTH_NOLEAP[month - 1]
        }
    }

    fun dateFromPDTString(str: String): LLDate? {
        val parts = str.split("/")
        if (parts.size != 3) return null
        val month = parts[0].toIntOrNull() ?: return null
        val day   = parts[1].toIntOrNull() ?: return null
        val year  = parts[2].toIntOrNull() ?: return null
        val base = LLDate.fromYMD(year, month, day)
        // Correct from Pacific time (UTC-8) to UTC
        return LLDate(base.secondsSinceEpoch + 8.0 * 3600.0)
    }

    fun ageFromDate(bornDate: LLDate, now: LLDate): String {
        val born = bornDate.toLocalDate()
        val current = now.toLocalDate()

        var bornYear  = born.year
        var bornMonth = born.monthValue
        val bornDay   = born.dayOfMonth

        var nowYear  = current.year
        var nowMonth = current.monthValue
        var nowDay   = current.dayOfMonth

        var ageDays = nowDay - bornDay
        if (ageDays < 0) {
            nowMonth -= 1
            if (nowMonth == 0) {
                nowYear -= 1
                nowMonth = 12
            }
            ageDays += daysFromMonth(nowYear, nowMonth)
        }
        var ageMonths = nowMonth - bornMonth
        if (ageMonths < 0) {
            nowYear -= 1
            ageMonths += 12
        }
        val ageYears = nowYear - bornYear

        val ageDaysTotal = ((now.secondsSinceEpoch - bornDate.secondsSinceEpoch) / 86400).toInt()

        if (ageMonths > 0 || ageYears > 0) {
            return when {
                ageYears > 0 && ageMonths > 0 ->
                    formatAge("YearsMonthsOld", ageYears, ageMonths, null, ageDaysTotal)
                ageYears > 0 ->
                    formatAge("YearsOld", ageYears, null, null, ageDaysTotal)
                else ->
                    formatAge("MonthsOld", null, ageMonths, null, ageDaysTotal)
            }
        }

        val ageWeeks = ageDays / 7
        val remainDays = ageDays % 7
        if (ageWeeks > 0) {
            return formatAge("WeeksOld", null, null, ageWeeks, ageDaysTotal)
        }

        if (ageDays > 0) {
            return formatAge("DaysOld", null, null, null, ageDays)
        }

        return "Today"
    }

    fun ageFromDate(dateString: String, now: LLDate): String {
        val bornDate = dateFromPDTString(dateString) ?: return "???"
        return ageFromDate(bornDate, now)
    }

    fun ageFromDate(dateString: String): String {
        return ageFromDate(dateString, LLDate.now())
    }

    fun secondsSinceEpochFromString(format: String, str: String): Int {
        val formatter = DateTimeFormatter.ofPattern(format)
        val date = LocalDate.parse(str, formatter)
        val epoch = LocalDate.of(1970, 1, 1)
        return ChronoUnit.SECONDS.between(
            epoch.atStartOfDay(ZoneOffset.UTC),
            date.atStartOfDay(ZoneOffset.UTC)
        ).toInt()
    }

    private fun formatAge(
        key: String,
        years: Int?,
        months: Int?,
        weeks: Int?,
        daysTotal: Int
    ): String {
        // Real implementation would call LLTrans.getString with a format map;
        // producing a plain English fallback here so the logic is exercisable without a
        // translation backend.
        return when (key) {
            "YearsMonthsOld" -> "${years} year${if (years != 1) "s" else ""}, ${months} month${if (months != 1) "s" else ""} ($daysTotal days)"
            "YearsOld"       -> "${years} year${if (years != 1) "s" else ""} ($daysTotal days)"
            "MonthsOld"      -> "${months} month${if (months != 1) "s" else ""} ($daysTotal days)"
            "WeeksOld"       -> "${weeks} week${if (weeks != 1) "s" else ""} ($daysTotal days)"
            "DaysOld"        -> "${daysTotal} day${if (daysTotal != 1) "s" else ""}"
            else             -> "Today"
        }
    }
}
