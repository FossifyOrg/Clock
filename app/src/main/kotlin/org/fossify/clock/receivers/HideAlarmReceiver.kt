package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.deleteNotificationChannel
import org.fossify.clock.extensions.hideNotification
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.ALARM_NOTIFICATION_CHANNEL_ID
import org.fossify.commons.helpers.ensureBackgroundThread

class HideAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        val channelId = intent.getStringExtra(ALARM_NOTIFICATION_CHANNEL_ID)
        channelId?.let { context.deleteNotificationChannel(channelId) }
        context.hideNotification(id)

        ensureBackgroundThread {
            val alarm = context.dbHelper.getAlarmWithId(id)
            if (alarm != null && alarm.days < 0) {
                if (alarm.oneShot) {
                    alarm.isEnabled = false
                    context.dbHelper.deleteAlarms(arrayListOf(alarm))
                } else {
                    context.dbHelper.updateAlarmEnabledState(alarm.id, false)
                }
                context.updateWidgets()
            }
        }
    }
}
