package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.hideNotification
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.EARLY_ALARM_NOTIF_ID
import org.fossify.clock.services.AlarmService
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.isOreoPlus

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        if (id == -1) return

        // Hide early dismissal notification if not already dismissed
        context.hideNotification(EARLY_ALARM_NOTIF_ID)

        try {
            Intent(context, AlarmService::class.java).apply {
                putExtra(ALARM_ID, id)
                if (isOreoPlus()) {
                    context.startForegroundService(this)
                } else {
                    context.startService(this)
                }
            }
        } catch (e: Exception) {
            context.showErrorToast(e)
        }
    }
}
