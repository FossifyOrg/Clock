package org.fossify.clock.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.goAsync

/**
 * Receiver responsible for rescheduling alarms in background.
 */
class RescheduleAlarmsReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        goAsync {
            context.alarmController.rescheduleEnabledAlarms()
        }
    }
}
