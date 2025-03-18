package org.fossify.clock.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.fossify.clock.R
import org.fossify.clock.activities.ReminderActivity
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.disableExpiredAlarm
import org.fossify.clock.extensions.hideNotification
import org.fossify.clock.extensions.isScreenOn
import org.fossify.clock.extensions.showAlarmNotification
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.ALARM_NOTIFICATION_CHANNEL_ID
import org.fossify.clock.helpers.ALARM_NOTIF_ID
import org.fossify.clock.helpers.EARLY_ALARM_NOTIF_ID
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.isOreoPlus

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        val alarm = context.dbHelper.getAlarmWithId(id) ?: return

        // Hide early dismissal notification if not already dismissed
        context.hideNotification(EARLY_ALARM_NOTIF_ID)

        if (context.isScreenOn()) {
            context.showAlarmNotification(alarm)
            Handler(Looper.getMainLooper()).postDelayed({
                context.hideNotification(id)
                context.disableExpiredAlarm(alarm)
            }, context.config.alarmMaxReminderSecs * 1000L)
        } else {
            if (isOreoPlus()) {
                val notificationManager = context.notificationManager
                if (notificationManager.getNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID) == null) {
                    // cleans up previous notification channel that had sound properties
                    oldNotificationChannelCleanup(notificationManager)

                    NotificationChannel(
                        ALARM_NOTIFICATION_CHANNEL_ID,
                        "Alarm",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        setBypassDnd(true)
                        setSound(null, null)
                        notificationManager.createNotificationChannel(this)
                    }
                }

                val reminderIntent = Intent(context, ReminderActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(ALARM_ID, id)
                }

                val pendingIntent = PendingIntent.getActivity(
                    context, 0, reminderIntent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alarm_vector)
                    .setContentTitle(context.getString(org.fossify.commons.R.string.alarm))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pendingIntent, true)

                try {
                    notificationManager.notify(ALARM_NOTIF_ID, builder.build())
                } catch (e: Exception) {
                    context.showErrorToast(e)
                }
            } else {
                Intent(context, ReminderActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(ALARM_ID, id)
                    context.startActivity(this)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun oldNotificationChannelCleanup(notificationManager: NotificationManager) {
        notificationManager.deleteNotificationChannel("Alarm")
    }
}
