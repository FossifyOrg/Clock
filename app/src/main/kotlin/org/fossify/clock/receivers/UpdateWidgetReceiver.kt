package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.updateWidgets

class UpdateWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.updateWidgets()
    }
}
