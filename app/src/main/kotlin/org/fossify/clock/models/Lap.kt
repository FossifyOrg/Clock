package org.fossify.clock.models

import org.fossify.clock.helpers.SORT_BY_LAP
import org.fossify.clock.helpers.SORT_BY_LAP_TIME
import org.fossify.commons.helpers.SORT_DESCENDING

data class Lap(val id: Int, var lapTime: Long, var totalTime: Long) : Comparable<Lap> {
    companion object {
        var sorting = 0
    }

    override fun compareTo(other: Lap): Int {
        var result = when {
            sorting and SORT_BY_LAP != 0 -> when {
                id == other.id -> 0
                id > other.id -> 1
                else -> -1
            }

            sorting and SORT_BY_LAP_TIME != 0 -> when {
                lapTime == other.lapTime -> 0
                lapTime > other.lapTime -> 1
                else -> -1
            }

            else -> when {
                totalTime == other.totalTime -> 0
                totalTime > other.totalTime -> 1
                else -> -1
            }
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }

        return result
    }
}
