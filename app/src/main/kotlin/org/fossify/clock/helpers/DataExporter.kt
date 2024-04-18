package org.fossify.clock.helpers

import org.fossify.clock.models.Alarm
import org.fossify.clock.models.Timer
import org.fossify.commons.helpers.ExportResult
import java.io.OutputStream

object DataExporter {

    fun exportData(
        alarms: ArrayList<Alarm>,
        timers: List<Timer>,
        outputStream: OutputStream?,
        callback: (result: ExportResult) -> Unit,
    ) {
        if (outputStream == null) {
            callback.invoke(ExportResult.EXPORT_FAIL)
            return
        }

        val alarmsToExport = alarmsToJSON(alarms)
        val timersToExport = timersToJSON(timers)

        val dataToExport = "{\"alarms\": $alarmsToExport, \"timers\": $timersToExport"

        try {
            outputStream.bufferedWriter().use { out ->
                out.write(dataToExport)
            }
            callback.invoke(ExportResult.EXPORT_OK)
        } catch (e: Exception) {
            callback.invoke(ExportResult.EXPORT_FAIL)
        }
    }

    // Replace with a generic later
    private fun alarmsToJSON(alarms: List<Alarm>?): String {
        if (alarms.isNullOrEmpty()) {
            return "[]"
        }

        val jsonAlarms = mutableListOf<String>()
        for (alarm in alarms) {
            jsonAlarms.add(alarm.toJSON())
        }

        return "[${jsonAlarms.joinToString(",")}]"
    }

    private fun timersToJSON(timers: List<Timer>?): String {
        if (timers.isNullOrEmpty()) {
            return "[]"
        }

        val jsonTimers = mutableListOf<String>()
        for (timer in timers) {
            jsonTimers.add(timer.toJSON())
        }

        return "[${jsonTimers.joinToString(",")}]"
    }
}
