package org.fossify.clock.models

sealed class AlarmListItem {
    data class AlarmRow(val alarm: Alarm) : AlarmListItem()
    data class GroupRow(val group: Group, val alarms: List<Alarm>) : AlarmListItem()
}
