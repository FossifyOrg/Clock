package org.fossify.clock.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.commons.extensions.showPickSecondsDialog
import org.fossify.commons.helpers.MINUTE_SECONDS

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmController.silenceAlarm()

        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        showPickSecondsDialog(
            curSeconds = config.snoozeTime * MINUTE_SECONDS,
            isSnoozePicker = true,
            cancelCallback = {
                alarmController.stopAlarm(alarmId)
                dialogCancelled()
            }
        ) {
            config.snoozeTime = it / MINUTE_SECONDS
            alarmController.snoozeAlarm(alarmId, config.snoozeTime)
            finishActivity()
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
