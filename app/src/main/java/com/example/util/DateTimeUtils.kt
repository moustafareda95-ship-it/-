package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    const val DEFAULT_TIMEZONE = "Africa/Cairo"

    fun timeZone(id: String = DEFAULT_TIMEZONE): TimeZone =
        TimeZone.getTimeZone(id.ifBlank { DEFAULT_TIMEZONE })

    fun locale(isArabic: Boolean): Locale =
        if (isArabic) Locale.Builder().setLanguage("ar").setRegion("EG").build() else Locale.US

    fun formatDate(millis: Long, isArabic: Boolean, tzId: String = DEFAULT_TIMEZONE): String {
        val pattern = if (isArabic) "EEEE، d MMMM yyyy" else "EEE, MMM d, yyyy"
        val sdf = SimpleDateFormat(pattern, locale(isArabic))
        sdf.timeZone = timeZone(tzId)
        return sdf.format(Date(millis))
    }

    fun formatShortDate(millis: Long, isArabic: Boolean, tzId: String = DEFAULT_TIMEZONE): String {
        val pattern = if (isArabic) "d MMM" else "MMM d"
        val sdf = SimpleDateFormat(pattern, locale(isArabic))
        sdf.timeZone = timeZone(tzId)
        return sdf.format(Date(millis))
    }

    fun formatDateNumeric(millis: Long, tzId: String = DEFAULT_TIMEZONE): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = timeZone(tzId)
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long, isArabic: Boolean, tzId: String = DEFAULT_TIMEZONE): String {
        val pattern = "hh:mm a"
        val sdf = SimpleDateFormat(pattern, locale(isArabic))
        sdf.timeZone = timeZone(tzId)
        return sdf.format(Date(millis))
    }

    fun currentMonthKey(millis: Long = System.currentTimeMillis(), tzId: String = DEFAULT_TIMEZONE): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        sdf.timeZone = timeZone(tzId)
        return sdf.format(Date(millis))
    }

    fun startOfDayMillis(millis: Long = System.currentTimeMillis(), tzId: String = DEFAULT_TIMEZONE): Long {
        val cal = Calendar.getInstance(timeZone(tzId))
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDayMillis(millis: Long = System.currentTimeMillis(), tzId: String = DEFAULT_TIMEZONE): Long {
        val cal = Calendar.getInstance(timeZone(tzId))
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun addDays(millis: Long, days: Int, tzId: String = DEFAULT_TIMEZONE): Long {
        val cal = Calendar.getInstance(timeZone(tzId))
        cal.timeInMillis = millis
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    fun isSameDay(a: Long, b: Long, tzId: String = DEFAULT_TIMEZONE): Boolean {
        val calA = Calendar.getInstance(timeZone(tzId)).apply { timeInMillis = a }
        val calB = Calendar.getInstance(timeZone(tzId)).apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    fun isOverdue(dueMillis: Long, isCompleted: Boolean, nowMillis: Long = System.currentTimeMillis(), tzId: String = DEFAULT_TIMEZONE): Boolean {
        if (isCompleted) return false
        return dueMillis < startOfDayMillis(nowMillis, tzId)
    }

    fun formatCurrency(amount: Double, currencyCode: String = "EGP", isArabic: Boolean = true): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            minimumFractionDigits = 0
        }
        val formatted = nf.format(amount)
        val label = when (currencyCode) {
            "EGP" -> if (isArabic) "ج.م" else "EGP"
            "SAR" -> if (isArabic) "ر.س" else "SAR"
            "USD" -> "$"
            else -> currencyCode
        }
        return if (currencyCode == "USD") "$$formatted" else "$formatted $label"
    }

    fun dayOfMonth(millis: Long, tzId: String = DEFAULT_TIMEZONE): Int {
        val cal = Calendar.getInstance(timeZone(tzId)).apply { timeInMillis = millis }
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun setHourMinute(dayMillis: Long, hour: Int, minute: Int, tzId: String = DEFAULT_TIMEZONE): Long {
        val cal = Calendar.getInstance(timeZone(tzId)).apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
