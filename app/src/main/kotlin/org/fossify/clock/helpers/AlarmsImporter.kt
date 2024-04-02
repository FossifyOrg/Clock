package org.fossify.clock.helpers

import android.app.Activity
import org.fossify.clock.models.Alarm
import org.fossify.commons.extensions.showErrorToast
import org.json.JSONArray
import org.json.JSONObject

import java.io.File

class AlarmsImporter(
    private val activity: Activity,
    private val dbHelper: DBHelper,
) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK
    }

    fun importAlarms(path: String): ImportResult {
        return try {
            val inputStream = File(path).inputStream()
            val jsonString = inputStream.bufferedReader().use { it.readText().trimEnd() }
            val jsonArray = JSONArray(jsonString)

            val insertedCount = insertAlarmsFromJSON(jsonArray)
            if (insertedCount > 0) {
                ImportResult.IMPORT_OK
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
            val alarm = parseAlarmFromJSON(jsonObject)
            val insertedId = dbHelper.insertAlarm(alarm)
            if (insertedId != -1) {
                insertedCount++
            }
        }
        return insertedCount
    }

    private fun parseAlarmFromJSON(jsonObject: JSONObject): Alarm {
        val id = jsonObject.getInt("id")
        val timeInMinutes = jsonObject.getInt("timeInMinutes")
        val days = jsonObject.getInt("days")
        val isEnabled = jsonObject.getBoolean("isEnabled")
        val vibrate = jsonObject.getBoolean("vibrate")
        val soundTitle = jsonObject.getString("soundTitle")
        val soundUri = jsonObject.getString("soundUri")
        val label = jsonObject.getString("label")
        val oneShot = jsonObject.optBoolean("oneShot", false)

        return Alarm(
            id,
            timeInMinutes,
            days,
            isEnabled,
            vibrate,
            soundTitle,
            soundUri,
            label,
            oneShot
        )
    }
}

