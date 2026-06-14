package me.owdding.mortem.core.event.catacomb

import me.owdding.mortem.core.catacombs.CatacombEndData
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

data class CatacombsEndEvent(
    val data: CatacombEndData,
) : SkyBlockEvent()
