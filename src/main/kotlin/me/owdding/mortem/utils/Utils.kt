package me.owdding.mortem.utils

import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

object Utils {

    internal fun SkyBlockEvent.post() = this.post(SkyBlockAPI.eventBus)

    @Suppress("UNCHECKED_CAST")
    fun <From, To> From.unsafeCast() = (this as To)
}
