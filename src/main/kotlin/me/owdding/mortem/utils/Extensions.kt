package me.owdding.mortem.utils

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

inline val Int.ticks: Duration get() = (this * 20).toDuration(DurationUnit.MILLISECONDS)
