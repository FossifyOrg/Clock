package org.fossify.clock.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.fossify.clock.R
import org.fossify.clock.extensions.*
import org.fossify.clock.helpers.*
import org.fossify.commons.extensions.notificationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Receiver responsible for showing a notification that allows users to skip an upcoming alarm.
 * This notification appears 10 minutes before (hardcoded) the alarm is scheduled to trigger.
 */
class UpcomingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId == -1) {
            return
        }

        goAsync {
            showUpcomingAlarmNotification(context, alarmId)
        }
    }

    private fun showUpcomingAlarmNotification(context: Context, alarmId: Int) {
        val alarm = context.dbHelper.getAlarmWithId(alarmId)
        if (alarm == null) {
            android.util.Log.w("UpcomingAlarmReceiver", "Alarm $alarmId no longer exists")
            return
        }

        val alarmTime = getTimeOfNextAlarm(alarm)
        if (alarmTime == null) {
            android.util.Log.w("UpcomingAlarmReceiver", "Alarm $alarmId has no next occurrence")
            return
        }

        val dayOfWeekIndex = (alarmTime.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val dayOfWeek = context.resources.getStringArray(org.fossify.commons.R.array.week_days_short)[dayOfWeekIndex]
        val pattern = if (context.config.use24HourFormat) FORMAT_24H else FORMAT_12H
        val formattedTime = SimpleDateFormat(pattern, Locale.getDefault()).format(alarmTime.time)
        val alarmString = "$dayOfWeek $formattedTime"

        val notificationId = getUpcomingAlarmNotificationId(alarmId)
        val notificationManager = context.notificationManager
        NotificationChannel(
            UPCOMING_ALARM_CHANNEL_ID,
            context.getString(R.string.upcoming_alarm),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setBypassDnd(true)
            setSound(null, null)
            notificationManager.createNotificationChannel(this)
        }

        val contentIntent = context.getOpenAlarmTabIntent()
        val dismissIntent = context.getSkipUpcomingAlarmPendingIntent(
            alarmId = alarmId, notificationId = notificationId
        )

        val notification = NotificationCompat.Builder(context, UPCOMING_ALARM_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.upcoming_alarm))
            .setContentText(alarmString)
            .setSmallIcon(R.drawable.ic_alarm_vector)
            .setPriority(Notification.PRIORITY_LOW)
            .addAction(
                0,
                context.getString(org.fossify.commons.R.string.dismiss),
                dismissIntent
            )
            .setContentIntent(contentIntent)
            .setSound(null)
            .setAutoCancel(true)
            .setChannelId(UPCOMING_ALARM_CHANNEL_ID)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
