package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.goAsync
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.ENTRY_ID
import org.fossify.clock.helpers.ENTRY_TYPE
import org.fossify.clock.helpers.updateNonRecurringAlarmDay
import org.fossify.clock.models.AlarmEvent
import org.greenrobot.eventbus.EventBus

/*
* Receiver responsible for *ENABLING* alarms after being turned off for one day
*/
class ReenableAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ENTRY_ID, -1)
        val type = intent.getIntExtra(ENTRY_TYPE, -1)
        if (id != -1 && type != -1) {
            goAsync {
                val job = context.config.pendingReenables
                    .find { it.entryId == id && it.entryType == type } ?: return@goAsync

                job.alarmIds.forEach { alarmId ->
                    context.dbHelper.getAlarmWithId(alarmId)?.let { alarm ->
                        alarm.isEnabled = true
                        if (!alarm.isRecurring()) {
                            updateNonRecurringAlarmDay(alarm)
                        }
                        context.dbHelper.updateAlarm(alarm)
                        context.alarmController.scheduleNextOccurrence(alarm)
                    }
                }

                context.config.pendingReenables = context.config.pendingReenables
                    .filterNot { it.entryId == id && it.entryType == type }

                EventBus.getDefault().post(AlarmEvent.Refresh)
                context.updateWidgets()
            }
        }
    }
}
