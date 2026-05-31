package org.fossify.clock.models

import androidx.annotation.Keep
import org.fossify.clock.helpers.getCurrentEpochDay
import org.fossify.clock.helpers.getTomorrowEpochDay
import org.fossify.clock.helpers.TODAY_BIT
import org.fossify.clock.helpers.TOMORROW_BIT

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
    var scheduledDate: Long = 0L,
) {
    fun isRecurring() = days > 0

    fun hasAbsoluteDate() = scheduledDate > 0L

    fun getScheduledDateEpochDay(): Long {
        if (scheduledDate > 0L) {
            return scheduledDate
        }

        return when (days) {
            TODAY_BIT -> getCurrentEpochDay()
            TOMORROW_BIT -> getTomorrowEpochDay()
            else -> 0L
        }
    }

    fun isToday() = !isRecurring() && getScheduledDateEpochDay() == getCurrentEpochDay()

    fun isTomorrow() = !isRecurring() && getScheduledDateEpochDay() == getTomorrowEpochDay()
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
    var j: Long = 0L,
) {
    fun toAlarm() = Alarm(a, b, c, d, e, f, g, h, i, j)
}
