package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.hideTimerNotification
import org.fossify.clock.helpers.INVALID_TIMER_ID
import org.fossify.clock.helpers.TIMER_ID
import org.fossify.clock.models.TimerEvent
import org.greenrobot.eventbus.EventBus

class HideTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID)
        context.hideTimerNotification(timerId)
        EventBus.getDefault().post(TimerEvent.Reset(timerId))
    }
}
