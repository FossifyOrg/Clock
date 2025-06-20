package org.fossify.clock.helpers

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.fossify.clock.models.Lap
import java.util.concurrent.CopyOnWriteArraySet

private const val UPDATE_INTERVAL_MS = 20L

object Stopwatch {
    private var startTime = 0L
    private var accumulatedTime = 0L
    private var lapStartTime = 0L
    private var accumulatedLapTime = 0L
    private var currentLap = 1

    private val updateListeners = CopyOnWriteArraySet<UpdateListener>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var updateJob: Job? = null

    val laps = ArrayList<Lap>()
    var state = State.STOPPED
        private set(value) {
            field = value
            updateListeners.forEach {
                it.onStateChanged(value)
            }
        }

    fun reset() {
        cancelUpdateJob()
        state = State.STOPPED
        accumulatedTime = 0L
        lapStartTime = 0L
        accumulatedLapTime = 0L
        currentLap = 1
        laps.clear()
        notifyListeners(totalTime = 0, lapTime = 0, useLongerMSFormat = false)
    }

    fun toggle() {
        val now = SystemClock.elapsedRealtime()
        if (state != State.RUNNING) {
            state = State.RUNNING
            startTime = now
            lapStartTime = now
            startUpdateJob()
        } else {
            state = State.PAUSED
            cancelUpdateJob()
            accumulatedTime += now - startTime
            accumulatedLapTime += now - lapStartTime
            notifyListeners(
                totalTime = accumulatedTime,
                lapTime = accumulatedLapTime,
                useLongerMSFormat = true
            )
        }
    }

    fun lap() {
        if (state != State.RUNNING) return

        val now = SystemClock.elapsedRealtime()
        val lapDuration = accumulatedLapTime + (now - lapStartTime)
        val totalDuration = accumulatedTime + (now - startTime)

        val lap = Lap(id = currentLap++, lapTime = lapDuration, totalTime = totalDuration)
        laps.add(0, lap)
        lapStartTime = now
        accumulatedLapTime = 0L
    }

    /**
     * Add a update listener to the stopwatch. The listener gets the current state
     * immediately after adding. To avoid memory leaks the listener should be removed
     * from the stopwatch.
     * @param updateListener the listener
     */
    fun addUpdateListener(updateListener: UpdateListener) {
        updateListeners.add(updateListener)
        val totalDuration: Long
        val lapDuration: Long
        if (state == State.RUNNING) {
            val now = SystemClock.elapsedRealtime()
            totalDuration = accumulatedTime + (now - startTime)
            lapDuration = accumulatedLapTime + (now - lapStartTime)
        } else {
            totalDuration = accumulatedTime
            lapDuration = accumulatedLapTime
        }

        updateListener.onUpdate(
            totalTime = totalDuration,
            lapTime = lapDuration,
            useLongerMSFormat = state != State.STOPPED
        )
        updateListener.onStateChanged(state)
    }

    /**
     * Remove the listener from the stopwatch
     * @param updateListener the listener
     */
    fun removeUpdateListener(updateListener: UpdateListener) {
        updateListeners.remove(updateListener)
    }

    private fun startUpdateJob() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive && state == State.RUNNING) {
                val now = SystemClock.elapsedRealtime()
                notifyListeners(
                    totalTime = accumulatedTime + (now - startTime),
                    lapTime = accumulatedLapTime + (now - lapStartTime),
                    useLongerMSFormat = false
                )
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun cancelUpdateJob() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun notifyListeners(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
        updateListeners.forEach {
            it.onUpdate(
                totalTime = totalTime,
                lapTime = lapTime,
                useLongerMSFormat = useLongerMSFormat
            )
        }
    }

    enum class State {
        RUNNING,
        PAUSED,
        STOPPED
    }

    interface UpdateListener {
        fun onUpdate(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean)
        fun onStateChanged(state: State)
    }
}
