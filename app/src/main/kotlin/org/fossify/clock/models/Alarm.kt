package org.fossify.clock.models

import androidx.annotation.Keep
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
) {
    fun isRecurring() = days > 0

    fun isToday() = days == TODAY_BIT

    fun isTomorrow() = days == TOMORROW_BIT
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
) {
    fun toAlarm() = Alarm(a, b, c, d, e, f, g, h, i)
}
