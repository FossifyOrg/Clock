package org.fossify.clock.helpers

import androidx.room.TypeConverter
import org.fossify.clock.extensions.gson.gson
import org.fossify.clock.models.StateWrapper
import org.fossify.clock.models.TimerState

class Converters {

    @TypeConverter
    fun jsonToTimerState(value: String): TimerState {
        return try {
            gson.fromJson(value, StateWrapper::class.java).state
        } catch (e: Exception) {
            TimerState.Idle
        }
    }

    @TypeConverter
    fun timerStateToJson(state: TimerState) = gson.toJson(StateWrapper(state))
}
