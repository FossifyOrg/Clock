package org.fossify.clock.models

import android.content.Context
import androidx.annotation.Keep
import org.fossify.clock.helpers.TODAY_BIT
import org.fossify.clock.helpers.TOMORROW_BIT
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Keep
@kotlinx.serialization.Serializable
data class Alarm(
    var id: Int,
    var timeInMinutes: Int,
    var days: Int,
    var isEnabled: Boolean,
    var vibrate: Boolean,
    var soundTitle: String,
    var soundUri: String,
    var label: String,
    var oneShot: Boolean = false,
    var specificDate: Long? = null, // Unix timestamp in milliseconds for specific date alarms
) {
    companion object {
        private const val MIN_DAYS_FOR_DAY_NAME = 2
        private const val MAX_DAYS_FOR_DAY_NAME = 6
        private const val MILLIS_IN_SECOND = 1000L
        private const val SECONDS_IN_MINUTE = 60
        private const val MINUTES_IN_HOUR = 60
        private const val HOURS_IN_DAY = 24
    }
   
    fun isRecurring() = days > 0 && specificDate == null
    
    fun isToday(): Boolean {
        if (days == TODAY_BIT) return true
        return specificDate?.let { isDateToday(it) } ?: false
    }
   
    fun isTomorrow(): Boolean {
        if (days == TOMORROW_BIT) return true
        return specificDate?.let { isDateTomorrow(it) } ?: false
    }

    fun hasSpecificDate() = specificDate != null

    fun getDateLabel(context: Context): String {
        return when {
            isToday() -> context.getString(org.fossify.commons.R.string.today)
            isTomorrow() -> context.getString(org.fossify.commons.R.string.tomorrow)
            specificDate != null -> formatSpecificDate(specificDate!!)
            isRecurring() -> "Recurring"
            else -> error("Invalid alarm state: days=$days, specificDate=$specificDate")
        }
    }

    private fun formatSpecificDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val daysDiff = getDaysDifference(today, calendar)

        return when {
            daysDiff in MIN_DAYS_FOR_DAY_NAME..MAX_DAYS_FOR_DAY_NAME -> {
                // Within next 7 days - show day name
                calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: ""
            }
            else -> {
                // Show formatted date
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)
            }
        }
    }

    private fun isDateToday(timestamp: Long): Boolean {
        val alarmDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return isSameDay(alarmDate, today)
    }

    private fun isDateTomorrow(timestamp: Long): Boolean {
        val alarmDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        return isSameDay(alarmDate, tomorrow)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getDaysDifference(from: Calendar, to: Calendar): Int {
        val fromMidnight = from.clone() as Calendar
        fromMidnight.set(Calendar.HOUR_OF_DAY, 0)
        fromMidnight.set(Calendar.MINUTE, 0)
        fromMidnight.set(Calendar.SECOND, 0)
        fromMidnight.set(Calendar.MILLISECOND, 0)

        val toMidnight = to.clone() as Calendar
        toMidnight.set(Calendar.HOUR_OF_DAY, 0)
        toMidnight.set(Calendar.MINUTE, 0)
        toMidnight.set(Calendar.SECOND, 0)
        toMidnight.set(Calendar.MILLISECOND, 0)

        val diffMillis = toMidnight.timeInMillis - fromMidnight.timeInMillis
        val millisInDay = MILLIS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY
        return (diffMillis / millisInDay).toInt()
    }
}

@Keep
data class ObfuscatedAlarm(
    var a: Int,
    var b: Int,
    var c: Int,
    var d: Boolean,
    var e: Boolean,
    var f: String,
    var g: String,
    var h: String,
    var i: Boolean = false,
    var j: Long? = null
) {
    fun toAlarm() = Alarm(a, b, c, d, e, f, g, h, i, j)
}
