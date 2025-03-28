package org.fossify.clock.services

import android.app.IntentService
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.ALARM_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        val id = intent!!.getIntExtra(ALARM_ID, -1)
        alarmController.snoozeAlarm(id, config.snoozeTime)
    }
}
