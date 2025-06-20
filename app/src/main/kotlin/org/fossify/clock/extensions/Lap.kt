package org.fossify.clock.extensions

import org.fossify.clock.helpers.STOPWATCH_LIVE_LAP_ID
import org.fossify.clock.models.Lap

fun Lap.isLive() = id == STOPWATCH_LIVE_LAP_ID
