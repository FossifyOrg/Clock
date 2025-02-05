package org.fossify.clock.models

import androidx.annotation.Keep

@Keep
@kotlinx.serialization.Serializable
sealed class TimerState {
    @Keep
    @kotlinx.serialization.Serializable
    object Idle : TimerState()

    @Keep
    @kotlinx.serialization.Serializable
    data class Running(val duration: Long, val tick: Long) : TimerState()

    @Keep
    @kotlinx.serialization.Serializable
    data class Paused(val duration: Long, val tick: Long) : TimerState()

    @Keep
    @kotlinx.serialization.Serializable
    object Finished : TimerState()
}
