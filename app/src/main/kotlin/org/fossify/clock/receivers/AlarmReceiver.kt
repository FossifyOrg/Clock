package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.goAsync
import org.fossify.clock.extensions.hideNotification
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.UPCOMING_ALARM_NOTIFICATION_ID

/**
 * Receiver responsible for sounding alarms. It is also responsible for hiding the
 * upcoming alarm notification and scheduling the next occurrence.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        if (id == -1) return

        cancelUpcomingAlarmNotification(context)
        goAsync {
            context.alarmController.onAlarmTriggered(id)
        }
    }

    private fun cancelUpcomingAlarmNotification(context: Context) {
        context.hideNotification(UPCOMING_ALARM_NOTIFICATION_ID)
    }
}
