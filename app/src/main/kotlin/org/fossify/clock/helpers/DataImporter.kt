package org.fossify.clock.helpers

import android.app.Activity
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.Timer
import org.fossify.commons.extensions.showErrorToast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DataImporter(
    private val activity: Activity,
    private val dbHelper: DBHelper,
    private val timerHelper: TimerHelper
) {

    enum class ImportResult {
        IMPORT_INCOMPLETE,
        ALARMS_IMPORT_FAIL,
        TIMERS_IMPORT_FAIL,
        IMPORT_FAIL,
        IMPORT_OK
    }

    fun importData(path: String): ImportResult {
        return try {
            val inputStream = File(path).inputStream()
            val jsonString = inputStream.bufferedReader().use { it.readText().trimEnd() }
            val jsonObject = JSONObject(jsonString)
            val alarmsFromJson = jsonObject.getJSONArray("alarms")
            val timersFromJson = jsonObject.getJSONArray("timers")

            val importedAlarms = insertAlarmsFromJSON(alarmsFromJson)
            val importedTimers = insertTimersFromJSON(timersFromJson)

            if (importedAlarms > 0 || importedTimers > 0) {
                if (importedAlarms < alarmsFromJson.length() || importedTimers < timersFromJson.length()) {
                    ImportResult.IMPORT_INCOMPLETE
                } else {
                    ImportResult.IMPORT_OK
                }
            } else if (importedAlarms == 0) {
                ImportResult.ALARMS_IMPORT_FAIL
            } else if (importedTimers == 0) {
                ImportResult.TIMERS_IMPORT_FAIL
            } else {
                ImportResult.IMPORT_FAIL
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
            ImportResult.IMPORT_FAIL
        }
    }


    private fun insertAlarmsFromJSON(jsonArray: JSONArray): Int {
        var insertedCount = 0
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            if (Alarm.parseFromJSON(jsonObject) != null) {
                val alarm = Alarm.parseFromJSON(jsonObject) as Alarm
                if (dbHelper.insertAlarm(alarm) != -1) {
                    insertedCount++
                }
            }
        }
        return insertedCount
    }

    private fun insertTimersFromJSON(jsonArray: JSONArray): Int {
        var insertedCount = 0
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            if (Timer.parseFromJSON(jsonObject) != null) {
                val timer = Timer.parseFromJSON(jsonObject) as Timer
                timerHelper.insertOrUpdateTimer(timer) { id ->
                    if (id != -1L) {
                        insertedCount++
                    }
                }
            }
        }
        return insertedCount
    }
}

