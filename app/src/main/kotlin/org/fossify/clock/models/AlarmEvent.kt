package org.fossify.clock.models

sealed interface AlarmEvent {
    data object Refresh : AlarmEvent
    data class Stopped(val alarmId: Int) : AlarmEvent
}
