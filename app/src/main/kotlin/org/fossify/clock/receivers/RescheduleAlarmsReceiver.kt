package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.goAsync

class RescheduleAlarmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        goAsync {
            context.alarmController.rescheduleEnabledAlarms()
        }
    }
}
