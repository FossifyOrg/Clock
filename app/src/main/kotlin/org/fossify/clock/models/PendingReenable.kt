package org.fossify.clock.models

import androidx.annotation.Keep

@Keep
@kotlinx.serialization.Serializable
data class PendingReenable(
    var entryType: Int,
    var entryId: Int,
    var alarmIds: List<Int>,
    var triggerAtMillis: Long,
)
