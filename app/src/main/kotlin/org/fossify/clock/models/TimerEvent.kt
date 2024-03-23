package org.fossify.clock.models

import org.fossify.clock.helpers.INVALID_TIMER_ID

sealed class TimerEvent(open val timerId: Int) {
    data class Delete(override val timerId: Int) : TimerEvent(timerId)
    data class Reset(override val timerId: Int) : TimerEvent(timerId)
    data class Start(override val timerId: Int, val duration: Long) : TimerEvent(timerId)
    data class Pause(override val timerId: Int, val duration: Long) : TimerEvent(timerId)
    data class Finish(override val timerId: Int, val duration: Long) : TimerEvent(timerId)
    object Refresh : TimerEvent(INVALID_TIMER_ID)
}
