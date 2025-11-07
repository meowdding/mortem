package me.owdding.mortem.core.event

import me.owdding.mortem.core.Instance
import me.owdding.mortem.core.catacombs.Catacomb
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

open class InstanceEvent<T : Instance>(open val instance: T) : SkyBlockEvent()

open class InstanceJoinEvent<T : Instance>(instance: T) : InstanceEvent<T>(instance)
open class InstanceLeaveEvent<T : Instance>(instance: T) : InstanceEvent<T>(instance)

data class CatacombJoinEvent(override val instance: Catacomb) : InstanceJoinEvent<Catacomb>(instance)
data class CatacombLeaveEvent(override val instance: Catacomb) : InstanceLeaveEvent<Catacomb>(instance)
