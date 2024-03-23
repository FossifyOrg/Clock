package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.rescheduleEnabledAlarms

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.rescheduleEnabledAlarms()
    }
}
