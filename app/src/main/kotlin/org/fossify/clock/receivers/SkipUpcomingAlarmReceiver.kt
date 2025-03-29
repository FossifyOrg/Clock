package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.goAsync
import org.fossify.clock.extensions.hideNotification
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.NOTIFICATION_ID

/**
 * Receiver responsible for dismissing *UPCOMING* alarms.
 */
class SkipUpcomingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId != -1) {
            goAsync {
                context.alarmController.skipNextOccurrence(alarmId)
            }
        }

        val notificationId = intent.getIntExtra(NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            context.hideNotification(notificationId)
        }
    }
}
