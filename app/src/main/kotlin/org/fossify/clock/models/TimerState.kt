package org.fossify.clock.models

import androidx.annotation.Keep

@Keep
sealed class TimerState {
    @Keep
    object Idle : TimerState()

    @Keep
    data class Running(val duration: Long, val tick: Long) : TimerState()

    @Keep
    data class Paused(val duration: Long, val tick: Long) : TimerState()

    @Keep
    object Finished : TimerState()
}
