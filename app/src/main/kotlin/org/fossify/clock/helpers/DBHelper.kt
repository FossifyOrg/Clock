package org.fossify.clock.helpers

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.createNewAlarm
import org.fossify.clock.models.Alarm
import org.fossify.commons.extensions.getIntValue
import org.fossify.commons.extensions.getStringValue
import org.fossify.commons.helpers.FRIDAY_BIT
import org.fossify.commons.helpers.MONDAY_BIT
import org.fossify.commons.helpers.SATURDAY_BIT
import org.fossify.commons.helpers.SUNDAY_BIT
import org.fossify.commons.helpers.THURSDAY_BIT
import org.fossify.commons.helpers.TUESDAY_BIT
import org.fossify.commons.helpers.WEDNESDAY_BIT

class DBHelper private constructor(
    val context: Context,
) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val alarmsTableName = "contacts"  // wrong table name, ignore it
    private val colId = "id"
    private val colTimeInMinutes = "time_in_minutes"
    private val colDays = "days"
    private val colIsEnabled = "is_enabled"
    private val colVibrate = "vibrate"
    private val colSoundTitle = "sound_title"
    private val colSoundUri = "sound_uri"
    private val colLabel = "label"
    private val colOneShot = "one_shot"
    private val colScheduledDate = "scheduled_date"

    private val mDb = writableDatabase

    companion object {
        private const val DB_VERSION = 3
        const val DB_NAME = "alarms.db"
        private const val VERSION_WITH_SCHEDULED_DATE = 3

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var dbInstance: DBHelper? = null

        fun newInstance(context: Context): DBHelper {
            val appContext = context.applicationContext
            return dbInstance ?: synchronized(this) {
                dbInstance ?: DBHelper(appContext).also { dbInstance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $alarmsTableName (" +
                "$colId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$colTimeInMinutes INTEGER, " +
                "$colDays INTEGER, " +
                "$colIsEnabled INTEGER, " +
                "$colVibrate INTEGER, " +
                "$colSoundTitle TEXT, " +
                "$colSoundUri TEXT, " +
                "$colLabel TEXT, " +
                "$colOneShot INTEGER, " +
                "$colScheduledDate INTEGER NOT NULL DEFAULT 0)"
        )
        insertInitialAlarms(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion > oldVersion) {
            db.execSQL("ALTER TABLE $alarmsTableName ADD COLUMN $colOneShot INTEGER NOT NULL DEFAULT 0")
        }

        if (oldVersion < VERSION_WITH_SCHEDULED_DATE) {
            db.execSQL(
                "ALTER TABLE $alarmsTableName " +
                    "ADD COLUMN $colScheduledDate INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    private fun insertInitialAlarms(db: SQLiteDatabase) {
        val weekDays = MONDAY_BIT or TUESDAY_BIT or WEDNESDAY_BIT or THURSDAY_BIT or FRIDAY_BIT
        val weekDaysAlarm = context.createNewAlarm(420, weekDays)
        insertAlarm(weekDaysAlarm, db)

        val weekEnd = SATURDAY_BIT or SUNDAY_BIT
        val weekEndAlarm = context.createNewAlarm(540, weekEnd)
        insertAlarm(weekEndAlarm, db)
    }

    fun insertAlarm(alarm: Alarm, db: SQLiteDatabase = mDb): Int {
        val values = fillAlarmContentValues(alarm)
        return db.insert(alarmsTableName, null, values).toInt()
    }

    fun updateAlarm(alarm: Alarm): Boolean {
        val selectionArgs = arrayOf(alarm.id.toString())
        val values = fillAlarmContentValues(alarm)
        val selection = "$colId = ?"
        return mDb.update(alarmsTableName, values, selection, selectionArgs) == 1
    }

    fun updateAlarmEnabledState(id: Int, isEnabled: Boolean): Boolean {
        val selectionArgs = arrayOf(id.toString())
        val values = ContentValues()
        values.put(colIsEnabled, isEnabled)
        val selection = "$colId = ?"
        return mDb.update(alarmsTableName, values, selection, selectionArgs) == 1
    }

    fun deleteAlarms(alarms: ArrayList<Alarm>) {
        alarms.filter { it.isEnabled }.forEach {
            context.cancelAlarmClock(it)
        }

        val args = TextUtils.join(", ", alarms.map { it.id.toString() })
        val selection = "$alarmsTableName.$colId IN ($args)"
        mDb.delete(alarmsTableName, selection, null)
    }

    fun getAlarmWithId(id: Int) = getAlarms().firstOrNull { it.id == id }

    fun getAlarmsWithUri(uri: String) = getAlarms().filter { it.soundUri == uri }

    private fun fillAlarmContentValues(alarm: Alarm): ContentValues {
        return ContentValues().apply {
            put(colTimeInMinutes, alarm.timeInMinutes)
            put(colDays, alarm.days)
            put(colIsEnabled, alarm.isEnabled)
            put(colVibrate, alarm.vibrate)
            put(colSoundTitle, alarm.soundTitle)
            put(colSoundUri, alarm.soundUri)
            put(colLabel, alarm.label)
            put(colOneShot, alarm.oneShot)
            put(colScheduledDate, alarm.scheduledDate)
        }
    }

    fun getEnabledAlarms() = getAlarms().filter { it.isEnabled }

    fun getAlarms(): ArrayList<Alarm> {
        val alarms = ArrayList<Alarm>()
        val cols = arrayOf(
            colId,
            colTimeInMinutes,
            colDays,
            colIsEnabled,
            colVibrate,
            colSoundTitle,
            colSoundUri,
            colLabel,
            colOneShot,
            colScheduledDate
        )
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(alarmsTableName, cols, null, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    try {
                        val id = cursor.getIntValue(colId)
                        val timeInMinutes = cursor.getIntValue(colTimeInMinutes)
                        val days = cursor.getIntValue(colDays)
                        val isEnabled = cursor.getIntValue(colIsEnabled) == 1
                        val vibrate = cursor.getIntValue(colVibrate) == 1
                        val soundTitle = cursor.getStringValue(colSoundTitle)
                        val soundUri = cursor.getStringValue(colSoundUri)
                        val label = cursor.getStringValue(colLabel)
                        val oneShot = cursor.getIntValue(colOneShot) == 1
                        val scheduledDateIndex = cursor.getColumnIndex(colScheduledDate)
                        val scheduledDate = if (scheduledDateIndex == -1) 0L else cursor.getLong(scheduledDateIndex)

                        val alarm = Alarm(
                            id = id,
                            timeInMinutes = timeInMinutes,
                            days = days,
                            isEnabled = isEnabled,
                            vibrate = vibrate,
                            soundTitle = soundTitle,
                            soundUri = soundUri,
                            label = label,
                            oneShot = oneShot,
                            scheduledDate = scheduledDate
                        )
                        alarms.add(alarm)
                    } catch (e: Exception) {
                        continue
                    }
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        return alarms
    }
}
