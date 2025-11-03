package me.owdding.mortem.utils

import org.joml.Vector2i
import org.joml.times
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

object Utils {

    internal fun SkyBlockEvent.post() = this.post(SkyBlockAPI.eventBus)

    @Suppress("UNCHECKED_CAST")
    fun <From, To> From.unsafeCast() = (this as To)

    val vectorZeroZero = Vector2i(0, 0)
    val vectorZeroOne = Vector2i(0, 1)
    val vectorOneZero = Vector2i(1, 0)
    val vectorOneOne = Vector2i(1, 1)
    val vectorZeroTwo = vectorZeroOne * 2
    val vectorTwoZero = vectorOneZero * 2
    val vectorTwoTwo = vectorOneOne * 2
}
