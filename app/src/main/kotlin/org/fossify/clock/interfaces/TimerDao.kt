package org.fossify.clock.interfaces

import androidx.room.*
import org.fossify.clock.models.Timer

@Dao
interface TimerDao {

    @Query("SELECT * FROM timers ORDER BY createdAt ASC")
    fun getTimers(): List<Timer>

    @Query("SELECT * FROM timers WHERE id=:id")
    fun getTimer(id: Int): Timer?

    @Query("SELECT * FROM timers WHERE seconds=:seconds AND label=:label")
    fun findTimers(seconds: Int, label: String): List<Timer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateTimer(timer: Timer): Long

    @Query("DELETE FROM timers WHERE id=:id")
    fun deleteTimer(id: Int)

    @Delete
    fun deleteTimers(list: List<Timer>)
}
