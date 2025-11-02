package me.owdding.mortem.core.event

import me.owdding.mortem.core.Instance
import me.owdding.mortem.core.catacombs.Catacomb
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

data class InstanceJoinEvent(val instance: Instance) : SkyBlockEvent()
data class InstanceLeaveEvent(val instance: Instance) : SkyBlockEvent()

data class CatacombJoinEvent(val instance: Catacomb) : SkyBlockEvent()
data class CatacombLeaveEvent(val instance: Catacomb) : SkyBlockEvent()
