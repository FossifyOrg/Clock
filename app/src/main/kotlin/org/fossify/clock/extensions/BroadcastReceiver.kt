package org.fossify.clock.extensions

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun BroadcastReceiver.goAsync(callback: suspend () -> Unit) {
    val pendingResult = goAsync()
    coroutineScope.launch {
        try {
            callback()
        } finally {
            pendingResult.finish()
        }
    }
}
