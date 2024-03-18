package org.fossify.clock.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.fossify.clock.R
import org.fossify.clock.activities.ReminderActivity
import org.fossify.clock.extensions.*
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.ALARM_NOTIFICATION_CHANNEL_ID
import org.fossify.clock.helpers.ALARM_NOTIF_ID
import org.fossify.clock.helpers.EARLY_ALARM_NOTIF_ID
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.isOreoPlus

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        val alarm = context.dbHelper.getAlarmWithId(id) ?: return

        context.hideNotification(EARLY_ALARM_NOTIF_ID) // hide early dismissal notification if not already dismissed

        if (context.isScreenOn()) {
            context.showAlarmNotification(alarm)
            Handler().postDelayed({
                context.hideNotification(id)
            }, context.config.alarmMaxReminderSecs * 1000L)
        } else {
            if (isOreoPlus()) {

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID) == null) {
                    oldNotificationChannelCleanup(notificationManager) // cleans up previous notification channel that had sound properties
                    NotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                        setBypassDnd(true)
                        setSound(null, null)
                        notificationManager.createNotificationChannel(this)
                    }
                }

                val pendingIntent = PendingIntent.getActivity(context, 0, Intent(context, ReminderActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(ALARM_ID, id)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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
