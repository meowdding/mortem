package me.owdding.mortem.core.event.catacomb

import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.types.CatacombRoomCheckmark
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

data class CatacombRoomCheckmarkChangeEvent(val node: RoomNode, val previous: CatacombRoomCheckmark, val new: CatacombRoomCheckmark) : SkyBlockEvent()
