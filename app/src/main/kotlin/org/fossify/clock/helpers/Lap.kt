package org.fossify.clock.helpers

import org.fossify.clock.models.Lap

fun Lap.isLive() = id == STOPWATCH_LIVE_LAP_ID
