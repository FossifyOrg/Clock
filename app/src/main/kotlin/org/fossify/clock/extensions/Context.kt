package org.fossify.clock.extensions

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_ALARM
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.widget.Toast
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import org.fossify.clock.R
import org.fossify.clock.activities.ReminderActivity
import org.fossify.clock.activities.SnoozeReminderActivity
import org.fossify.clock.activities.SplashActivity
import org.fossify.clock.databases.AppDatabase
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.ALARM_NOTIFICATION_CHANNEL_ID
import org.fossify.clock.helpers.Config
import org.fossify.clock.helpers.DBHelper
import org.fossify.clock.helpers.EARLY_ALARM_DISMISSAL_INTENT_ID
import org.fossify.clock.helpers.EDITED_TIME_ZONE_SEPARATOR
import org.fossify.clock.helpers.FORMAT_12H
import org.fossify.clock.helpers.FORMAT_24H
import org.fossify.clock.helpers.MyAnalogueTimeWidgetProvider
import org.fossify.clock.helpers.MyDigitalTimeWidgetProvider
import org.fossify.clock.helpers.NOTIFICATION_ID
import org.fossify.clock.helpers.OPEN_ALARMS_TAB_INTENT_ID
import org.fossify.clock.helpers.OPEN_STOPWATCH_TAB_INTENT_ID
import org.fossify.clock.helpers.OPEN_TAB
import org.fossify.clock.helpers.REMINDER_ACTIVITY_INTENT_ID
import org.fossify.clock.helpers.TAB_ALARM
import org.fossify.clock.helpers.TAB_STOPWATCH
import org.fossify.clock.helpers.TAB_TIMER
import org.fossify.clock.helpers.TIMER_ID
import org.fossify.clock.helpers.TODAY_BIT
import org.fossify.clock.helpers.TOMORROW_BIT
import org.fossify.clock.helpers.TimerHelper
import org.fossify.clock.helpers.formatTime
import org.fossify.clock.helpers.getAllTimeZones
import org.fossify.clock.helpers.getCurrentDayMinutes
import org.fossify.clock.helpers.getDefaultTimeZoneTitle
import org.fossify.clock.helpers.getPassedSeconds
import org.fossify.clock.helpers.getTimeOfNextAlarm
import org.fossify.clock.interfaces.TimerDao
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.clock.models.MyTimeZone
import org.fossify.clock.models.Timer
import org.fossify.clock.models.TimerState
import org.fossify.clock.receivers.AlarmReceiver
import org.fossify.clock.receivers.DismissAlarmReceiver
import org.fossify.clock.receivers.EarlyAlarmDismissalReceiver
import org.fossify.clock.receivers.HideAlarmReceiver
import org.fossify.clock.receivers.HideTimerReceiver
import org.fossify.clock.services.SnoozeService
import org.fossify.commons.extensions.formatMinutesToTimeString
import org.fossify.commons.extensions.formatSecondsToTimeString
import org.fossify.commons.extensions.getDefaultAlarmSound
import org.fossify.commons.extensions.getLaunchIntent
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getSelectedDaysString
import org.fossify.commons.extensions.grantReadUriPermission
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toInt
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.EVERY_DAY_BIT
import org.fossify.commons.helpers.FRIDAY_BIT
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.MONDAY_BIT
import org.fossify.commons.helpers.SATURDAY_BIT
import org.fossify.commons.helpers.SILENT
import org.fossify.commons.helpers.SUNDAY_BIT
import org.fossify.commons.helpers.THURSDAY_BIT
import org.fossify.commons.helpers.TUESDAY_BIT
import org.fossify.commons.helpers.WEDNESDAY_BIT
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isOreoPlus
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.dbHelper: DBHelper get() = DBHelper.newInstance(applicationContext)
val Context.timerDb: TimerDao get() = AppDatabase.getInstance(applicationContext).TimerDao()
val Context.timerHelper: TimerHelper get() = TimerHelper(this)

fun Context.getFormattedDate(calendar: Calendar): String {
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7    // make sure index 0 means monday
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH)

    val dayString = resources.getStringArray(org.fossify.commons.R.array.week_days_short)[dayOfWeek]
    val monthString = resources.getStringArray(org.fossify.commons.R.array.months)[month]
    return "$dayString, $dayOfMonth $monthString"
}

fun Context.getEditedTimeZonesMap(): HashMap<Int, String> {
    val editedTimeZoneTitles = config.editedTimeZoneTitles
    val editedTitlesMap = HashMap<Int, String>()
    editedTimeZoneTitles.forEach {
        val parts = it.split(EDITED_TIME_ZONE_SEPARATOR.toRegex(), 2)
        editedTitlesMap[parts[0].toInt()] = parts[1]
    }
    return editedTitlesMap
}

fun Context.getAllTimeZonesModified(): ArrayList<MyTimeZone> {
    val timeZones = getAllTimeZones()
    val editedTitlesMap = getEditedTimeZonesMap()
    timeZones.forEach {
        if (editedTitlesMap.keys.contains(it.id)) {
            it.title = editedTitlesMap[it.id]!!
        } else {
            it.title = it.title.substring(it.title.indexOf(' ')).trim()
        }
    }
    return timeZones
}

fun Context.getModifiedTimeZoneTitle(id: Int) = getAllTimeZonesModified().firstOrNull { it.id == id }?.title ?: getDefaultTimeZoneTitle(id)

fun Context.createNewAlarm(timeInMinutes: Int, weekDays: Int): Alarm {
    val defaultAlarmSound = getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
    return Alarm(0, timeInMinutes, weekDays, false, false, defaultAlarmSound.title, defaultAlarmSound.uri, "")
}

fun Context.createNewTimer(): Timer {
    return Timer(
        null,
        config.timerSeconds,
        TimerState.Idle,
        config.timerVibrate,
        config.timerSoundUri,
        config.timerSoundTitle,
        config.timerLabel ?: "",
        System.currentTimeMillis(),
        config.timerChannelId,
    )
}

fun Context.scheduleNextAlarm(alarm: Alarm, showToast: Boolean) {
    val triggerTimeMillis = getTimeOfNextAlarm(alarm)?.timeInMillis ?: return
    setupAlarmClock(alarm = alarm, triggerTimeMillis = triggerTimeMillis)

    if (showToast) {
        val now = Calendar.getInstance()
        val triggerInMillis = triggerTimeMillis - now.timeInMillis
        showRemainingTimeMessage(triggerInMillis)
    }
}

fun Context.showRemainingTimeMessage(triggerInMillis: Long) {
    val totalSeconds = triggerInMillis.milliseconds.inWholeSeconds.toInt()
    val remainingTime = if (totalSeconds >= MINUTE_SECONDS) {
        val roundedMinutes = ceil(totalSeconds / MINUTE_SECONDS.toFloat()).toInt()
        formatMinutesToTimeString(roundedMinutes)
    } else {
        formatSecondsToTimeString(totalSeconds)
    }

    toast(
        msg = String.format(
            getString(org.fossify.commons.R.string.time_remaining), remainingTime
        ),
        length = Toast.LENGTH_LONG
    )
}

fun Context.setupAlarmClock(alarm: Alarm, triggerTimeMillis: Long) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    try {
        AlarmManagerCompat.setAlarmClock(alarmManager, triggerTimeMillis, getOpenAlarmTabIntent(), getAlarmIntent(alarm))

        // show a notification to allow dismissing the alarm 10 minutes before it actually triggers
        val dismissalTriggerTime = if (triggerTimeMillis - System.currentTimeMillis() < 10.minutes.inWholeMilliseconds) {
            System.currentTimeMillis() + 500
        } else {
            triggerTimeMillis - 10.minutes.inWholeMilliseconds
        }
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, 0, dismissalTriggerTime, getEarlyAlarmDismissalIntent(alarm))
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.getEarlyAlarmDismissalIntent(alarm: Alarm): PendingIntent {
    val intent = Intent(this, EarlyAlarmDismissalReceiver::class.java).apply {
        putExtra(ALARM_ID, alarm.id)
    }
    return PendingIntent.getBroadcast(this, EARLY_ALARM_DISMISSAL_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getOpenAlarmTabIntent(): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, SplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_ALARM)
    return PendingIntent.getActivity(this, OPEN_ALARMS_TAB_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getOpenTimerTabIntent(timerId: Int): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, SplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_TIMER)
    intent.putExtra(TIMER_ID, timerId)
    return PendingIntent.getActivity(this, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getOpenStopwatchTabIntent(): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, SplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_STOPWATCH)
    return PendingIntent.getActivity(this, OPEN_STOPWATCH_TAB_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getAlarmIntent(alarm: Alarm): PendingIntent {
    val intent = Intent(this, AlarmReceiver::class.java)
    intent.putExtra(ALARM_ID, alarm.id)
    return PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.cancelAlarmClock(alarm: Alarm) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(getAlarmIntent(alarm))
    alarmManager.cancel(getEarlyAlarmDismissalIntent(alarm))
}

fun Context.hideNotification(id: Int) {
    val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(id)
}

fun Context.deleteNotificationChannel(channelId: String) {
    if (isOreoPlus()) {
        try {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel(channelId)
        } catch (_: Throwable) {
        }
    }
}

fun Context.hideTimerNotification(timerId: Int) = hideNotification(timerId)

fun Context.updateWidgets() {
    updateDigitalWidgets()
    updateAnalogueWidgets()
}

fun Context.updateDigitalWidgets() {
    val component = ComponentName(applicationContext, MyDigitalTimeWidgetProvider::class.java)
    val widgetIds = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(component) ?: return
    if (widgetIds.isNotEmpty()) {
        val ids = intArrayOf(R.xml.widget_digital_clock_info)
        Intent(applicationContext, MyDigitalTimeWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(this)
        }
    }
}

fun Context.updateAnalogueWidgets() {
    val component = ComponentName(applicationContext, MyAnalogueTimeWidgetProvider::class.java)
    val widgetIds = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(component) ?: return
    if (widgetIds.isNotEmpty()) {
        val ids = intArrayOf(R.xml.widget_analogue_clock_info)
        Intent(applicationContext, MyAnalogueTimeWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(this)
        }
    }
}

fun Context.getFormattedTime(passedSeconds: Int, showSeconds: Boolean, makeAmPmSmaller: Boolean): SpannableString {
    val use24HourFormat = config.use24HourFormat
    val hours = (passedSeconds / 3600) % 24
    val minutes = (passedSeconds / 60) % 60
    val seconds = passedSeconds % 60

    return if (use24HourFormat) {
        val formattedTime = formatTime(showSeconds, use24HourFormat, hours, minutes, seconds)
        SpannableString(formattedTime)
    } else {
        val formattedTime = formatTo12HourFormat(showSeconds, hours, minutes, seconds)
        val spannableTime = SpannableString(formattedTime)
        val amPmMultiplier = if (makeAmPmSmaller) 0.4f else 1f
        spannableTime.setSpan(RelativeSizeSpan(amPmMultiplier), spannableTime.length - 3, spannableTime.length, 0)
        spannableTime
    }
}

fun Context.formatTo12HourFormat(showSeconds: Boolean, hours: Int, minutes: Int, seconds: Int): String {
    val appendable = getString(if (hours >= 12) org.fossify.commons.R.string.p_m else org.fossify.commons.R.string.a_m)
    val newHours = if (hours == 0 || hours == 12) 12 else hours % 12
    return "${formatTime(showSeconds, false, newHours, minutes, seconds)} $appendable"
}

fun Context.getClosestEnabledAlarmString(callback: (result: String) -> Unit) {
    getEnabledAlarms { enabledAlarms ->
        if (enabledAlarms.isNullOrEmpty()) {
            callback("")
            return@getEnabledAlarms
        }

        val now = Calendar.getInstance()
        val nextAlarmList = enabledAlarms
            .mapNotNull(::getTimeOfNextAlarm)
            .filter { it > now }

        val closestAlarmTime = nextAlarmList.minOrNull()
        if (closestAlarmTime == null) {
            callback("")
            return@getEnabledAlarms
        }

        val dayOfWeekIndex = (closestAlarmTime.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val dayOfWeek = resources.getStringArray(org.fossify.commons.R.array.week_days_short)[dayOfWeekIndex]
        val pattern = if (config.use24HourFormat) {
            FORMAT_24H
        } else {
            FORMAT_12H
        }

        val formattedTime = SimpleDateFormat(pattern, Locale.getDefault()).format(closestAlarmTime.time)
        callback("$dayOfWeek $formattedTime")
    }
}

fun Context.getEnabledAlarms(callback: (result: List<Alarm>?) -> Unit) {
    ensureBackgroundThread {
        val alarms = dbHelper.getEnabledAlarms()
        Handler(Looper.getMainLooper()).post {
            callback(alarms)
        }
    }
}

fun Context.rescheduleEnabledAlarms() {
    dbHelper.getEnabledAlarms().forEach {
        if (it.days != TODAY_BIT || it.timeInMinutes > getCurrentDayMinutes()) {
            scheduleNextAlarm(it, false)
        }
    }
}

fun Context.isScreenOn() = (getSystemService(Context.POWER_SERVICE) as PowerManager).isScreenOn

fun Context.showAlarmNotification(alarm: Alarm) {
    val pendingIntent = getOpenAlarmTabIntent()
    val notification = getAlarmNotification(pendingIntent, alarm)
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    try {
        notificationManager.notify(alarm.id, notification)
    } catch (e: Exception) {
        showErrorToast(e)
    }

    if (alarm.days > 0) {
        scheduleNextAlarm(alarm, false)
    }
}

fun Context.getTimerNotification(timer: Timer, pendingIntent: PendingIntent, addDeleteIntent: Boolean): Notification {
    var soundUri = timer.soundUri
    if (soundUri == SILENT) {
        soundUri = ""
    } else {
        grantReadUriPermission(soundUri)
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = timer.channelId ?: "simple_timer_channel_${soundUri}_${System.currentTimeMillis()}"
    timerHelper.insertOrUpdateTimer(timer.copy(channelId = channelId))

    if (isOreoPlus()) {
        try {
            notificationManager.deleteNotificationChannel(channelId)
        } catch (_: Exception) {
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(STREAM_ALARM)
            .build()

        val name = getString(R.string.timer)
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(channelId, name, importance).apply {
            setBypassDnd(true)
            enableLights(true)
            lightColor = getProperPrimaryColor()
            setSound(soundUri.toUri(), audioAttributes)

            if (!timer.vibrate) {
                vibrationPattern = longArrayOf(0L)
            }

            enableVibration(timer.vibrate)
            notificationManager.createNotificationChannel(this)
        }
    }

    val title = timer.label.ifEmpty {
        getString(R.string.timer)
    }

    val reminderActivityIntent = getReminderActivityIntent()
    val builder = NotificationCompat.Builder(this)
        .setContentTitle(title)
        .setContentText(getString(R.string.time_expired))
        .setSmallIcon(R.drawable.ic_hourglass_vector)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setCategory(Notification.CATEGORY_EVENT)
        .setAutoCancel(true)
        .setSound(soundUri.toUri(), STREAM_ALARM)
        .setChannelId(channelId)
        .addAction(
            org.fossify.commons.R.drawable.ic_cross_vector,
            getString(org.fossify.commons.R.string.dismiss),
            if (addDeleteIntent) {
                reminderActivityIntent
            } else {
                getHideTimerPendingIntent(timer.id!!)
            }
        )

    if (addDeleteIntent) {
        builder.setDeleteIntent(reminderActivityIntent)
    }

    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (timer.vibrate) {
        val vibrateArray = LongArray(2) { 500 }
        builder.setVibrate(vibrateArray)
    }

    val notification = builder.build()
    notification.flags = notification.flags or Notification.FLAG_INSISTENT
    return notification
}

fun Context.getHideTimerPendingIntent(timerId: Int): PendingIntent {
    val intent = Intent(this, HideTimerReceiver::class.java)
    intent.putExtra(TIMER_ID, timerId)
    return PendingIntent.getBroadcast(this, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getHideAlarmPendingIntent(alarm: Alarm, channelId: String): PendingIntent {
    val intent = Intent(this, HideAlarmReceiver::class.java).apply {
        putExtra(ALARM_ID, alarm.id)
        putExtra(ALARM_NOTIFICATION_CHANNEL_ID, channelId)
    }
    return PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getDismissAlarmPendingIntent(alarmId: Int, notificationId: Int): PendingIntent {
    val intent = Intent(this, DismissAlarmReceiver::class.java).apply {
        putExtra(ALARM_ID, alarmId)
        putExtra(NOTIFICATION_ID, notificationId)
    }
    return PendingIntent.getBroadcast(this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getAlarmNotification(pendingIntent: PendingIntent, alarm: Alarm): Notification {
    val soundUri = alarm.soundUri
    if (soundUri != SILENT) {
        grantReadUriPermission(soundUri)
    }
    val channelId = "simple_alarm_channel_${soundUri}_${alarm.vibrate}"
    val label = alarm.label.ifEmpty {
        getString(org.fossify.commons.R.string.alarm)
    }

    if (isOreoPlus()) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(STREAM_ALARM)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(channelId, label, importance).apply {
            setBypassDnd(true)
            enableLights(true)
            lightColor = getProperPrimaryColor()
            enableVibration(alarm.vibrate)
            setSound(soundUri.toUri(), audioAttributes)
            notificationManager.createNotificationChannel(this)
        }
    }

    val dismissIntent = getHideAlarmPendingIntent(alarm, channelId)
    val builder = NotificationCompat.Builder(this)
        .setContentTitle(label)
        .setContentText(getFormattedTime(getPassedSeconds(), false, false))
        .setSmallIcon(R.drawable.ic_alarm_vector)
        .setContentIntent(pendingIntent)
        .setPriority(Notification.PRIORITY_HIGH)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setAutoCancel(true)
        .setChannelId(channelId)
        .addAction(
            org.fossify.commons.R.drawable.ic_snooze_vector,
            getString(org.fossify.commons.R.string.snooze),
            getSnoozePendingIntent(alarm)
        )
        .addAction(org.fossify.commons.R.drawable.ic_cross_vector, getString(org.fossify.commons.R.string.dismiss), dismissIntent)
        .setDeleteIntent(dismissIntent)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (soundUri != SILENT) {
        builder.setSound(soundUri.toUri(), STREAM_ALARM)
    }

    if (alarm.vibrate) {
        val vibrateArray = LongArray(2) { 500 }
        builder.setVibrate(vibrateArray)
    }

    val notification = builder.build()
    notification.flags = notification.flags or Notification.FLAG_INSISTENT
    return notification
}

fun Context.getSnoozePendingIntent(alarm: Alarm): PendingIntent {
    val snoozeClass = if (config.useSameSnooze) SnoozeService::class.java else SnoozeReminderActivity::class.java
    val intent = Intent(this, snoozeClass).setAction("Snooze")
    intent.putExtra(ALARM_ID, alarm.id)
    return if (config.useSameSnooze) {
        PendingIntent.getService(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    } else {
        PendingIntent.getActivity(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

fun Context.getReminderActivityIntent(): PendingIntent {
    val intent = Intent(this, ReminderActivity::class.java)
    return PendingIntent.getActivity(this, REMINDER_ACTIVITY_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.checkAlarmsWithDeletedSoundUri(uri: String) {
    val defaultAlarmSound = getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
    dbHelper.getAlarmsWithUri(uri).forEach {
        it.soundTitle = defaultAlarmSound.title
        it.soundUri = defaultAlarmSound.uri
        dbHelper.updateAlarm(it)
    }
}

fun Context.getAlarmSelectedDaysString(bitMask: Int): String {
    return when (bitMask) {
        TODAY_BIT -> getString(org.fossify.commons.R.string.today)
        TOMORROW_BIT -> getString(org.fossify.commons.R.string.tomorrow)
        EVERY_DAY_BIT -> getString(org.fossify.commons.R.string.every_day)
        else -> getSelectedDaysString(bitMask)
    }
}

fun Context.orderDaysList(days: List<Int>): List<Int> {
    if (config.firstDayOfWeek > 0) {
        val range = (config.firstDayOfWeek..6).toList() + (0..<config.firstDayOfWeek).toList()
        return days.slice(range)
    } else {
        return days
    }
}

fun Context.firstDayOrder(bitMask: Int): Int {
    if (bitMask == TODAY_BIT) return -2
    if (bitMask == TOMORROW_BIT) return -1

    val dayBits = orderDaysList(arrayListOf(MONDAY_BIT, TUESDAY_BIT, WEDNESDAY_BIT, THURSDAY_BIT, FRIDAY_BIT, SATURDAY_BIT, SUNDAY_BIT))

    dayBits.forEachIndexed { i, bit ->
        if (bitMask and bit != 0) {
            return i
        }
    }

    return bitMask
}

fun Context.disableExpiredAlarm(alarm: Alarm) {
    if (alarm.days < 0) {
        if (alarm.oneShot) {
            alarm.isEnabled = false
            dbHelper.deleteAlarms(arrayListOf(alarm))
        } else {
            dbHelper.updateAlarmEnabledState(alarm.id, false)
        }

        updateWidgets()
        EventBus.getDefault().post(AlarmEvent.Refresh)
    }
}