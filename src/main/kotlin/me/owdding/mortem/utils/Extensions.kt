package me.owdding.mortem.utils

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

inline val Int.ticks: Duration get() = (this * 20).toDuration(DurationUnit.MILLISECONDS)

@Suppress("UNCHECKED_CAST")
fun <A, B> A.unsafeCast() = this as B

fun String.getIntOrNull(separator: Char = ','): Int? = this.replace(separator.toString(), "").toIntOrNull()
fun String.getFloatOrNull(separator: Char = ','): Float? = this.replace(separator.toString(), "").toFloatOrNull()
