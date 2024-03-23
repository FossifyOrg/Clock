package org.fossify.clock.extensions.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory
import org.fossify.clock.models.TimerState

val timerStates = valueOf<TimerState>()
    .registerSubtype(TimerState.Idle::class.java)
    .registerSubtype(TimerState.Running::class.java)
    .registerSubtype(TimerState.Paused::class.java)
    .registerSubtype(TimerState.Finished::class.java)

inline fun <reified T : Any> valueOf(): RuntimeTypeAdapterFactory<T> = RuntimeTypeAdapterFactory.of(T::class.java)

fun GsonBuilder.registerTypes(vararg types: TypeAdapterFactory) = apply {
    types.forEach { registerTypeAdapterFactory(it) }
}

val gson: Gson = GsonBuilder().registerTypes(timerStates).create()
