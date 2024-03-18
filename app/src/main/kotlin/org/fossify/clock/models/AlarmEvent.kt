package org.fossify.clock.models

sealed interface AlarmEvent {
    object Refresh : AlarmEvent
}
