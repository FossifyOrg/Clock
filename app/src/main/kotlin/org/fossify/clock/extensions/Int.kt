package org.fossify.clock.extensions

import java.util.concurrent.TimeUnit

val Int.secondsToMillis get() = TimeUnit.SECONDS.toMillis(this.toLong())
val Int.millisToSeconds get() = TimeUnit.MILLISECONDS.toSeconds(this.toLong())

fun Int.isBitSet(bit: Int) = (this shr bit and 1) > 0
