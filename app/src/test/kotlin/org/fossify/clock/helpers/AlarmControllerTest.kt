package org.fossify.clock.helpers

import org.fossify.clock.models.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AlarmController reschedule logic.
 * Tests the actual functions used in AlarmController.
 */
class AlarmControllerTest {

    /**
     * Test basic behavior: current time is 7 AM, alarm is 8 AM
     * Should be marked as TODAY since alarm time hasn't passed yet
     */
    @Test
    fun testAlarmBeforeCurrentTime_ShouldBeToday() {
        val alarm = createAlarm(timeInMinutes = 480) // 8:00 AM
        val currentTime = 420 // 7:00 AM
        
        updateNonRecurringAlarmDay(alarm, currentTime)
        
        assertEquals(TODAY_BIT, alarm.days)
    }

    /**
     * Test basic behavior: current time is 9 AM, alarm is 8 AM
     * Should be marked as TOMORROW since alarm time has passed
     */
    @Test
    fun testAlarmAfterCurrentTime_ShouldBeTomorrow() {
        val alarm = createAlarm(timeInMinutes = 480) // 8:00 AM
        val currentTime = 540 // 9:00 AM
        
        updateNonRecurringAlarmDay(alarm, currentTime)
        
        assertEquals(TOMORROW_BIT, alarm.days)
    }

    /**
     * BUG REPRODUCTION TEST using actual AlarmController.shouldRescheduleAlarm():
     * 
     * Scenario: User sets alarm for 8 AM tomorrow, saves to DB with TOMORROW_BIT.
     * Next day arrives, user restarts phone at 9 AM (after alarm time).
     * AlarmController.rescheduleEnabledAlarms() reads alarm from DB and calls shouldRescheduleAlarm().
     * 
     * Expected: Alarm should NOT be rescheduled (time has passed for today's intended alarm)
     * Actual Bug: shouldRescheduleAlarm returns TRUE because alarm still has TOMORROW_BIT!
     * 
     * This test asserts the CORRECT behavior, so it FAILS now (demonstrating the bug exists)
     * and will PASS once we implement the fix.
     */
    @Test
    fun testBugScenario_StaleAlarmFromDBGetsRescheduledIncorrectly() {
        // Alarm was set yesterday for "tomorrow" at 8:00 AM and saved to DB with TOMORROW_BIT
        val alarm = createAlarm(timeInMinutes = 480, initialDays = TOMORROW_BIT) // 8:00 AM
        
        // Next day: it's now 9:00 AM (after the alarm time)
        // The alarm in DB still has TOMORROW_BIT (stale data)
        val currentTime = 540 // 9:00 AM
        
        // AlarmController.rescheduleEnabledAlarms() calls shouldRescheduleAlarm
        val shouldSchedule = AlarmController.shouldRescheduleAlarm(alarm, currentTime)
        
        // Assert CORRECT behavior: should be FALSE (don't reschedule stale alarms)
        // This will FAIL now because the bug makes it return TRUE
        assertFalse("Stale alarm should NOT be rescheduled - time has passed", shouldSchedule)
    }

    private fun createAlarm(timeInMinutes: Int, initialDays: Int = 0): Alarm {
        return Alarm(
            id = 1,
            timeInMinutes = timeInMinutes,
            days = initialDays,
            isEnabled = true,
            vibrate = true,
            soundTitle = "Test",
            soundUri = "",
            label = "",
            oneShot = false
        )
    }
}
