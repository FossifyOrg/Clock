package org.fossify.clock.models

import androidx.annotation.Keep

@Keep
@kotlinx.serialization.Serializable
data class AlarmTimerBackup(
    val alarms: List<Alarm>,
    val timers: List<Timer>,
)
