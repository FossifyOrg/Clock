package org.fossify.clock.helpers

import android.app.Application
import android.content.Context
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.setupAlarmClock
import org.fossify.clock.extensions.showRemainingTimeMessage
import org.fossify.clock.extensions.startAlarmService
import org.fossify.clock.extensions.stopAlarmService
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.commons.extensions.removeBit
import org.fossify.commons.helpers.ensureBackgroundThread
import org.greenrobot.eventbus.EventBus
import java.util.Calendar

/**
 * Centralized class for handling alarm operations including dismissal, cancellation, scheduling,
 * and state management.
 */
class AlarmController(
    private val context: Application,
    private val db: DBHelper,
    private val bus: EventBus,
) {
    /**
     * Reschedules all enabled alarms with the exception of one-time alarms that were scheduled
     * for today (to avoid rescheduling skipped upcoming alarms, yeah).
     */
    fun rescheduleEnabledAlarms() {
        db.getEnabledAlarms().forEach {
            // TODO: Instead of naively not scheduling all alarms for today, skipped upcoming
            //  alarms should be tracked properly.
            if (!it.isToday() || it.timeInMinutes > getCurrentDayMinutes()) {
                scheduleNextOccurrence(it, false)
            }
        }
    }

    /**
     * Schedules the next occurrence of a repeating alarm based on its repetition rules.
     *
     * @param alarm The alarm to schedule.
     * @param showToasts If true, a remaining time toast will be shown for the alarm.
     */
    fun scheduleNextOccurrence(alarm: Alarm, showToasts: Boolean = false) {
        ensureBackgroundThread {
            scheduleNextAlarm(alarm, showToasts)
            notifyObservers()
        }
    }

    /**
     * Skips (cancels) the *next scheduled occurrence* of an alarm before it rings.
     * If the alarm is repeating, it cancels the upcoming alert and schedules the *following*
     * occurrence based on repetition rules. If the alarm is a one-time alarm, it cancels and
     * disables or deletes it.
     *
     * @param alarmId The ID of the upcoming alarm trigger to skip/cancel.
     */
    fun skipNextOccurrence(alarmId: Int) {
        ensureBackgroundThread {
            val alarm = db.getAlarmWithId(alarmId) ?: return@ensureBackgroundThread
            context.cancelAlarmClock(alarm)

            // Schedule the *next* occurrence based on the original repeating schedule.
            if (alarm.isRecurring()) {
                // TODO: This is a bit of a hack. Skipped alarms should be tracked properly.
                val todayBitmask = getTodayBit()
                if (alarm.days and todayBitmask != 0) {
                    // If there are other days set, schedule based on those remaining days.
                    val remainingDays = alarm.days.removeBit(todayBitmask)
                    if (remainingDays > 0) {
                        val alarmForScheduling = alarm.copy(days = remainingDays)
                        scheduleNextAlarm(alarmForScheduling)
                    } else {
                        // Today was the ONLY weekday set. Skipping it means no weekdays are left.
                        // TODO: But does this mean the alarm won't be scheduled for next week?
                    }
                } else {
                    // Not scheduled for today anyway, just reschedule the alarm.
                    scheduleNextAlarm(alarm)
                }
            } else {
                disableOrDeleteOneTimeAlarm(alarm)
            }

            notifyObservers()
        }
    }

    /**
     * Handles the triggering of an alarm, scheduling the next occurrence and starting the service
     * for sounding the alarm.
     *
     * @param alarmId The ID of the alarm that was triggered.
     */
    fun onAlarmTriggered(alarmId: Int) {
        ensureBackgroundThread {
            // Reschedule the next occurrence right away
            val alarm = db.getAlarmWithId(alarmId) ?: return@ensureBackgroundThread
            if (alarm.isRecurring()) {
                scheduleNextOccurrence(alarm)
            }
        }

        context.startAlarmService(alarmId)
    }

    /**
     * Dismisses an alarm that is currently ringing or has just finished ringing.
     *
     * - Stops the alarm sound/vibration service ([stopAlarmService]).
     * - If the alarm is *not* repeating and the `disable` parameter is true, the alarm is
     *   disabled or deleted via [disableOrDeleteOneTimeAlarm].
     *
     * @param alarmId The ID of the alarm to dismiss.
     * @param disable If true and the alarm is a one-time alarm, it will be disabled or deleted.
     *                This parameter has no effect on repeating alarms.
     */
    fun stopAlarm(alarmId: Int, disable: Boolean = true) {
        context.stopAlarmService()
        bus.post(AlarmEvent.Stopped(alarmId))

        ensureBackgroundThread {
            val alarm = db.getAlarmWithId(alarmId)

            // We don't reschedule alarms here.
            if (alarm != null && !alarm.isRecurring() && disable) {
                context.cancelAlarmClock(alarm)
                disableOrDeleteOneTimeAlarm(alarm)
            }

            notifyObservers()
        }
    }

    /**
     * Snoozes an alarm that is currently ringing.
     *
     * - Stops the alarm sound/vibration service ([stopAlarmService]).
     * - Schedules the alarm to ring again after [snoozeMinutes] using [setupAlarmClock]
     *   with a calculated future trigger time.
     *
     * TODO: This works but it is very rudimentary. Snoozed alarms should be tracked properly.
     *
     * @param alarmId The ID of the alarm to snooze.
     * @param snoozeMinutes The number of minutes from now until the alarm should ring again.
     */
    fun snoozeAlarm(alarmId: Int, snoozeMinutes: Int) {
        context.stopAlarmService()
        bus.post(AlarmEvent.Stopped(alarmId))

        ensureBackgroundThread {
            val alarm = db.getAlarmWithId(alarmId)
            if (alarm != null) {
                context.setupAlarmClock(
                    alarm = alarm,
                    triggerTimeMillis = Calendar.getInstance()
                        .apply { add(Calendar.MINUTE, snoozeMinutes) }
                        .timeInMillis
                )
            }

            notifyObservers()
        }
    }

    /**
     * Handles disabling or deleting a *one-time* (non-repeating) alarm based on `oneShot` property.
     * This is typically called after a one-time alarm has rung and been dismissed or stopped,
     * or when it's explicitly skipped.
     *
     * @param alarm The one-time alarm to disable or delete. Must not be repeating.
     */
    private fun disableOrDeleteOneTimeAlarm(alarm: Alarm) {
        require(!alarm.isRecurring()) {
            "Alarm ${alarm.id} is repeating but was passed to disableOrDeleteOneTimeAlarm()"
        }

        if (alarm.oneShot) {
            alarm.isEnabled = false
            db.deleteAlarms(arrayListOf(alarm))
        } else {
            db.updateAlarmEnabledState(alarm.id, false)
        }
    }

    private fun scheduleNextAlarm(alarm: Alarm, showToast: Boolean = false) {
        val triggerTimeMillis = getTimeOfNextAlarm(alarm)?.timeInMillis ?: return
        context.setupAlarmClock(alarm = alarm, triggerTimeMillis = triggerTimeMillis)

        if (showToast) {
            val now = Calendar.getInstance()
            val triggerInMillis = triggerTimeMillis - now.timeInMillis
            context.showRemainingTimeMessage(triggerInMillis)
        }
    }

    private fun notifyObservers() {
        context.updateWidgets()
        bus.post(AlarmEvent.Refresh)
    }

    companion object {
        @Volatile
        private var instance: AlarmController? = null

        fun getInstance(context: Context): AlarmController {
            val appContext = context.applicationContext as Application
            return instance ?: synchronized(this) {
                instance ?: AlarmController(
                    context = appContext,
                    db = appContext.dbHelper,
                    bus = EventBus.getDefault()
                ).also { instance = it }
            }
        }
    }
}
