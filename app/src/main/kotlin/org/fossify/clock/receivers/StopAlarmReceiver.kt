package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.goAsync
import org.fossify.clock.helpers.ALARM_ID

/**
 * Receiver responsible for stopping running alarms.
 */
class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        if (id != -1) {
            goAsync {
                context.alarmController.stopAlarm(id)
            }
        }
    }
}
