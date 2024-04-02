package org.fossify.clock.helpers

import org.fossify.clock.models.Alarm
import org.fossify.commons.helpers.ExportResult
import java.io.OutputStream

object AlarmsExporter {
    fun exportAlarms(
        alarms: ArrayList<Alarm>,
        outputStream: OutputStream?,
        callback: (result: ExportResult) -> Unit,
    ) {
        if (outputStream == null) {
            callback.invoke(ExportResult.EXPORT_FAIL)
            return
        }

        val alarmsToExport = alarmsToJSON(alarms)

        try {
            outputStream.bufferedWriter().use { out ->
                out.write(alarmsToExport)
            }
            callback.invoke(ExportResult.EXPORT_OK)
        } catch (e: Exception) {
            callback.invoke(ExportResult.EXPORT_FAIL)
        }
    }

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

}
