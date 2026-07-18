package org.fossify.clock.helpers

import android.content.Context
import android.net.Uri
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.Group
import org.fossify.clock.models.AlarmTimerBackup
import org.fossify.clock.models.Timer
import org.fossify.commons.extensions.showErrorToast

class ImportHelper(
    private val context: Context,
    private val dbHelper: DBHelper,
    private val timerHelper: TimerHelper,
) {

    enum class ImportResult {
        IMPORT_INCOMPLETE,
        IMPORT_FAIL,
        IMPORT_OK
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun importData(uri: Uri): ImportResult {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val backup = Json.decodeFromStream<AlarmTimerBackup>(inputStream)
                val importedGroups = insertGroups(backup.groups)
                val updatedAlarms = updateAlarmGroupIds(backup.alarms)
                val importedAlarms = insertAlarms(updatedAlarms)
                val importedTimers = insertTimers(backup.timers)
                when {
                    importedGroups.count() > 0 || importedAlarms > 0 || importedTimers > 0 -> ImportResult.IMPORT_OK
                    importedGroups.count() == 0 && importedAlarms == 0 && importedTimers == 0 -> ImportResult.IMPORT_INCOMPLETE
                    else -> ImportResult.IMPORT_FAIL
                }
            } ?: ImportResult.IMPORT_FAIL
        } catch (e: Exception) {
            context.showErrorToast(e)
            ImportResult.IMPORT_FAIL
        }
    }

    private fun insertAlarms(alarms: List<Alarm>): Int {
        val existingAlarms = dbHelper.getAlarms()
        var insertedCount = 0
        alarms.forEach { alarm ->
            if (isAlarmAlreadyInserted(alarm, existingAlarms)) {
                dbHelper.updateAlarm(alarm)
            } else {
                if (dbHelper.insertAlarm(alarm) != -1) {
                    insertedCount++
                }
            }
        }
        return insertedCount
    }

    private fun updateAlarmGroupIds(alarms: List<Alarm>): List<Alarm> {
        alarms.forEach { alarm ->
            val groups = dbHelper.getGroups().map { it.ref to it.id }
            if (groups.any { (key) -> alarm.groupRef == key }) {
                val tmp = groups[alarm.groupRef]
                alarm.groupRef = tmp.second
            }
        }

        return alarms
    }

    private fun insertGroups(groups: List<Group>): MutableMap<Int, Int> {
        val existingGroups = dbHelper.getGroups()
        val idMap = mutableMapOf<Int, Int>()
        groups.forEach { group ->
            if (!isGroupInserted(group, existingGroups)){
                val newId = dbHelper.insertGroup(group.title)
                if (newId != -1){
                    idMap.put(group.id, newId)
                }
            }
        }
        return idMap
    }

    private fun insertTimers(timers: List<Timer>): Int {
        var insertedCount = 0
        timers.forEach { timer ->
            timerHelper.getTimers { existingTimers ->
                timer.id = if (existingTimers.isNotEmpty()) {
                    existingTimers.last().id?.plus(1)
                } else {
                    1
                }
                if (!isTimerAlreadyInserted(timer, existingTimers)) {
                    timerHelper.insertOrUpdateTimer(timer) { id ->
                        if (id != -1L) {
                            insertedCount++
                        }
                    }
                }
            }
        }
        return insertedCount
    }

    private fun isAlarmAlreadyInserted(alarm: Alarm, existingAlarms: List<Alarm>): Boolean {
        for (existingAlarm in existingAlarms) {
            if (alarm.timeInMinutes == existingAlarm.timeInMinutes &&
                alarm.days == existingAlarm.days &&
                alarm.vibrate == existingAlarm.vibrate &&
                alarm.soundTitle == existingAlarm.soundTitle &&
                alarm.soundUri == existingAlarm.soundUri &&
                alarm.label == existingAlarm.label &&
                alarm.oneShot == existingAlarm.oneShot
            ) {
                return true
            }
        }
        return false
    }

    private fun isGroupInserted(group: Group, existingGroups: List<Group>): Boolean{
        for (existingGroup in existingGroups) {
            if (group.title == existingGroup.title) {
                return true
            }
        }
        return false
    }

    private fun isTimerAlreadyInserted(timer: Timer, existingTimers: List<Timer>): Boolean {
        for (existingTimer in existingTimers) {
            if (timer.seconds == existingTimer.seconds &&
                timer.vibrate == existingTimer.vibrate &&
                timer.soundUri == existingTimer.soundUri &&
                timer.soundTitle == existingTimer.soundTitle &&
                timer.label == existingTimer.label &&
                timer.createdAt == existingTimer.createdAt
            ) {
                return true
            }
        }
        return false
    }
}

