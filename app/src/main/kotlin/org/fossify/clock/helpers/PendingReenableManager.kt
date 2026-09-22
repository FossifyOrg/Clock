package org.fossify.clock.helpers

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.core.app.AlarmManagerCompat
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.alarmManager
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.getReenablePendingIntent
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.PendingReenable
import kotlin.time.Clock

class PendingReenableManager(
    private val context: Application,
    private val db: DBHelper,
    private val config: Config,
) {
    fun onManuallyDisabled(entryType: Int, entryId: Int) {
        config.promptEntryType = entryType
        config.promptEntryId = entryId
    }

    fun onManuallyEnabled(entryType: Int, entryId: Int) {
        clearPromptIfMatching(entryType, entryId)
        cancelPendingReenable(entryType, entryId)
    }

    fun isPromptVisible(entryType: Int, entryId: Int): Boolean {
        return config.promptEntryType == entryType && config.promptEntryId == entryId
    }

    fun confirmReenable(entryType: Int, entryId: Int, affectedAlarms: List<Alarm>) {
        val triggerAtMillis = affectedAlarms.maxOfOrNull { getLastRelevantOccurrence(it).timeInMillis } ?: return
        if (triggerAtMillis == 0L) return

        val pendingReenable = PendingReenable(
            entryType = entryType,
            entryId = entryId,
            alarmIds = affectedAlarms.map { it.id },
            triggerAtMillis = triggerAtMillis
        )

        config.pendingReenables = config.pendingReenables.filterNot { it.entryType == entryType && it.entryId == entryId } + pendingReenable

        AlarmManagerCompat.setExactAndAllowWhileIdle(
            context.alarmManager,
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            context.getReenablePendingIntent(pendingReenable)
        )

        clearPromptIfMatching(entryType, entryId)
    }

    fun reprocessPendingReenables() {
        val now = System.currentTimeMillis()
        val (due, notYetDue) = config.pendingReenables.partition { it.triggerAtMillis <= now }

        due.forEach { pending -> enableAlarmIds(context, pending.alarmIds) }

        notYetDue.forEach { pending ->
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                context.alarmManager,
                AlarmManager.RTC_WAKEUP,
                pending.triggerAtMillis,
                context.getReenablePendingIntent(pending)
            )
        }

        config.pendingReenables = notYetDue
    }

    private fun clearPromptIfMatching(entryType: Int, entryId: Int) {
        if (config.promptEntryType == entryType && config.promptEntryId == entryId) {
            config.promptEntryType = ENTRY_TYPE_NONE
            config.promptEntryId = -1
        }
    }

    private fun cancelPendingReenable(entryType: Int, entryId: Int) {
        val pending = config.pendingReenables
            .find { it.entryType == entryType && it.entryId == entryId } ?: return

        context.alarmManager.cancel(context.getReenablePendingIntent(pending))
        config.pendingReenables = config.pendingReenables.filterNot { it == pending }
    }

    companion object {
        @Volatile
        private var instance: PendingReenableManager? = null

        fun getInstance(context: Context): PendingReenableManager {
            val appContext = context.applicationContext as Application
            return instance ?: synchronized(this) {
                instance ?: PendingReenableManager(
                    context = appContext,
                    db = appContext.dbHelper,
                    config = appContext.config
                ).also { instance = it }
            }
        }

        fun enableAlarmIds(context: Context, alarmIds: List<Int>) {
            alarmIds.forEach { alarmId ->
                context.dbHelper.getAlarmWithId(alarmId)?.let { alarm ->
                    alarm.isEnabled = true
                    if (!alarm.isRecurring()) {
                        updateNonRecurringAlarmDay(alarm)
                    }
                    context.dbHelper.updateAlarm(alarm)
                    context.alarmController.scheduleNextOccurrence(alarm)
                }
            }
        }
    }
}
