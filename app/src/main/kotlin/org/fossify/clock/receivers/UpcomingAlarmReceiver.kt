package org.fossify.clock.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.fossify.clock.R
import org.fossify.clock.extensions.getClosestEnabledAlarmString
import org.fossify.clock.extensions.getOpenAlarmTabIntent
import org.fossify.clock.extensions.getSkipUpcomingAlarmPendingIntent
import org.fossify.clock.extensions.goAsync
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.EARLY_ALARM_DISMISSAL_CHANNEL_ID
import org.fossify.clock.helpers.UPCOMING_ALARM_NOTIFICATION_ID
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.helpers.isOreoPlus

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
        context.getClosestEnabledAlarmString { alarmString ->
            val notificationManager = context.notificationManager
            if (isOreoPlus()) {
                NotificationChannel(
                    EARLY_ALARM_DISMISSAL_CHANNEL_ID,
                    context.getString(R.string.early_alarm_dismissal),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setBypassDnd(true)
                    setSound(null, null)
                    notificationManager.createNotificationChannel(this)
                }
            }

            val contentIntent = context.getOpenAlarmTabIntent()
            val dismissIntent = context.getSkipUpcomingAlarmPendingIntent(
                alarmId = alarmId, notificationId = UPCOMING_ALARM_NOTIFICATION_ID
            )

            val notification = NotificationCompat.Builder(context)
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
                .setChannelId(EARLY_ALARM_DISMISSAL_CHANNEL_ID)
                .build()

            notificationManager.notify(UPCOMING_ALARM_NOTIFICATION_ID, notification)
        }
    }
}
