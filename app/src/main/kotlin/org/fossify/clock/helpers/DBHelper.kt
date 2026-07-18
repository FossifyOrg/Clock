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
import org.fossify.clock.models.Group
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

    private val ALARMS_TABLE_NAME = "contacts"  // wrong table name, ignore it
    private val COL_ID = "id"
    private val COL_TIME_IN_MINUTES = "time_in_minutes"
    private val COL_DAYS = "days"
    private val COL_IS_ENABLED = "is_enabled"
    private val COL_VIBRATE = "vibrate"
    private val COL_SOUND_TITLE = "sound_title"
    private val COL_SOUND_URI = "sound_uri"
    private val COL_LABEL = "label"
    private val COL_ONE_SHOT = "one_shot"
    private val GROUPS_TABLE_NAME = "alarm_groups"
    private val COL_GROUP_ID = "group_id"
    private val COL_GROUP_REF = "group_ref"
    private val COL_GROUP_TITLE = "title"


    private val mDb = writableDatabase

    companion object {
        private const val DB_VERSION = 3
        const val DB_NAME = "alarms.db"

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
            "CREATE TABLE IF NOT EXISTS $ALARMS_TABLE_NAME (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_TIME_IN_MINUTES INTEGER, $COL_DAYS INTEGER, " +
                "$COL_IS_ENABLED INTEGER, $COL_VIBRATE INTEGER, $COL_SOUND_TITLE TEXT, $COL_SOUND_URI TEXT, " +
                "$COL_LABEL TEXT, $COL_ONE_SHOT INTEGER, $COL_GROUP_ID INTEGER)"
        )
        createGroupsTable(db)
        insertInitialAlarms(db)
    }

    private fun createGroupsTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $GROUPS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_GROUP_REF INTEGER, $COL_GROUP_TITLE TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion > oldVersion) {
            db.execSQL("ALTER TABLE $ALARMS_TABLE_NAME ADD COLUMN $COL_ONE_SHOT INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $GROUPS_TABLE_NAME ADD COLUMN $COL_GROUP_REF INTEGER DEFAULT 0")
            createGroupsTable(db)
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
        return db.insert(ALARMS_TABLE_NAME, null, values).toInt()
    }

    fun updateAlarm(alarm: Alarm): Boolean {
        val selectionArgs = arrayOf(alarm.id.toString())
        val values = fillAlarmContentValues(alarm)
        val selection = "$COL_ID = ?"
        return mDb.update(ALARMS_TABLE_NAME, values, selection, selectionArgs) == 1
    }

    fun updateAlarmEnabledState(id: Int, isEnabled: Boolean): Boolean {
        val selectionArgs = arrayOf(id.toString())
        val values = ContentValues()
        values.put(COL_IS_ENABLED, isEnabled)
        val selection = "$COL_ID = ?"
        return mDb.update(ALARMS_TABLE_NAME, values, selection, selectionArgs) == 1
    }

    fun deleteAlarms(alarms: ArrayList<Alarm>) {
        alarms.filter { it.isEnabled }.forEach {
            context.cancelAlarmClock(it)
        }

        val args = TextUtils.join(", ", alarms.map { it.id.toString() })
        val selection = "$ALARMS_TABLE_NAME.$COL_ID IN ($args)"
        mDb.delete(ALARMS_TABLE_NAME, selection, null)
    }

    fun insertGroup(title: String, ref: Int = 0): Int {
        val values = ContentValues().apply {
            put(COL_GROUP_TITLE, title)
        }
        val res = mDb.insert(GROUPS_TABLE_NAME, null, values).toInt()
        if (res == -1) {
            return -1
        }

        val finalRef = if (ref == 0) res else ref
        if (ref == 0) {
            updateGroup(Group(res, res, title))
        } else {
            updateGroup(Group(res, ref, title))
        }

        return finalRef
    }

    fun updateGroup(group: Group) {
        val values = ContentValues().apply {
            put(COL_GROUP_TITLE, group.title)
            put(COL_GROUP_REF, group.ref) }
        mDb.update(GROUPS_TABLE_NAME, values, "$COL_ID = ?", arrayOf(group.id.toString()))
    }

    fun deleteGroup(groupRef: Int) {
        val values = ContentValues().apply { putNull(COL_GROUP_ID) }
        mDb.update(ALARMS_TABLE_NAME, values, "$COL_GROUP_ID = ?", arrayOf(groupRef.toString()))
        mDb.delete(GROUPS_TABLE_NAME, "$COL_GROUP_REF = ?", arrayOf(groupRef.toString()))
    }
    fun getGroups(): ArrayList<Group> {
        val groups = ArrayList<Group>()
        val cursor = mDb.query(GROUPS_TABLE_NAME, arrayOf(COL_ID, COL_GROUP_REF, COL_GROUP_TITLE), null, null, null, null, null)
        cursor.use {
            if (it.moveToFirst()){
                do {
                    groups.add(Group(id = it.getIntValue(COL_ID), ref = it.getIntValue(COL_GROUP_REF), title = it.getStringValue(COL_GROUP_TITLE)))
                } while (it.moveToNext())
            }
        }

        return groups
    }

    fun assignAlarmsToGroup(alarmIds: List<Int>, groupRef: Int) {
        val values = ContentValues().apply {
            if (groupRef == 0) {
                putNull(COL_GROUP_ID)
            } else {
                put(COL_GROUP_ID, groupRef)
            }
        }
        val args = TextUtils.join(", ", alarmIds)
        mDb.update(ALARMS_TABLE_NAME, values, "$COL_ID IN ($args)", null)
    }

    fun updateGroupEnabledState(groupRef: Int, isEnabled: Boolean): List<Alarm> {
        val values = ContentValues().apply { put(COL_IS_ENABLED, isEnabled) }
        mDb.update(ALARMS_TABLE_NAME, values, "$COL_GROUP_ID = ?", arrayOf(groupRef.toString()))
        return  getAlarms().filter { it.groupRef == groupRef }
    }

    fun getAlarmWithId(id: Int) = getAlarms().firstOrNull { it.id == id }

    fun getAlarmsWithUri(uri: String) = getAlarms().filter { it.soundUri == uri }

    private fun fillAlarmContentValues(alarm: Alarm): ContentValues {
        return ContentValues().apply {
            put(COL_TIME_IN_MINUTES, alarm.timeInMinutes)
            put(COL_DAYS, alarm.days)
            put(COL_IS_ENABLED, alarm.isEnabled)
            put(COL_VIBRATE, alarm.vibrate)
            put(COL_SOUND_TITLE, alarm.soundTitle)
            put(COL_SOUND_URI, alarm.soundUri)
            put(COL_LABEL, alarm.label)
            put(COL_ONE_SHOT, alarm.oneShot)
            put(COL_GROUP_ID, alarm.groupRef)
        }
    }

    fun getEnabledAlarms() = getAlarms().filter { it.isEnabled }

    fun getAlarms(): ArrayList<Alarm> {
        val alarms = ArrayList<Alarm>()
        val cols = arrayOf(
            COL_ID,
            COL_TIME_IN_MINUTES,
            COL_DAYS,
            COL_IS_ENABLED,
            COL_VIBRATE,
            COL_SOUND_TITLE,
            COL_SOUND_URI,
            COL_LABEL,
            COL_ONE_SHOT,
            COL_GROUP_ID
        )
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(ALARMS_TABLE_NAME, cols, null, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    try {
                        val id = cursor.getIntValue(COL_ID)
                        val timeInMinutes = cursor.getIntValue(COL_TIME_IN_MINUTES)
                        val days = cursor.getIntValue(COL_DAYS)
                        val isEnabled = cursor.getIntValue(COL_IS_ENABLED) == 1
                        val vibrate = cursor.getIntValue(COL_VIBRATE) == 1
                        val soundTitle = cursor.getStringValue(COL_SOUND_TITLE)
                        val soundUri = cursor.getStringValue(COL_SOUND_URI)
                        val label = cursor.getStringValue(COL_LABEL)
                        val oneShot = cursor.getIntValue(COL_ONE_SHOT) == 1
                        val groupId = cursor.getIntValue(COL_GROUP_ID)

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
                            groupRef = groupId
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
