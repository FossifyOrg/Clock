package org.fossify.clock.extensions

import android.text.format.DateFormat
import org.fossify.commons.extensions.getFormattedDuration
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

fun Long.formatStopwatchTime(useLongerMSFormat: Boolean): String {
    val MSFormat = if (useLongerMSFormat) "%03d" else "%01d"
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this))
    var ms = this % 1000
    if (!useLongerMSFormat) {
        ms /= 100
    }

    return when {
        hours > 0 -> {
            val format = "%02d:%02d:%02d.$MSFormat"
            String.format(format, hours, minutes, seconds, ms)
        }

        minutes > 0 -> {
            val format = "%02d:%02d.$MSFormat"
            String.format(format, minutes, seconds, ms)
        }

        else -> {
            val format = "%d.$MSFormat"
            String.format(format, seconds, ms)
        }
    }
}

fun Long.timestampFormat(format: String = "dd. MM. yyyy"): String {
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = this

    return DateFormat.format(format, calendar).toString()
}

fun Long.getFormattedDuration(forceShowHours: Boolean = false): String {
    return this.div(1000F).roundToInt().getFormattedDuration(forceShowHours)
}

val Long.secondsToMillis get() = TimeUnit.SECONDS.toMillis(this)
val Long.millisToSeconds get() = TimeUnit.MILLISECONDS.toSeconds(this)
