package me.owdding.mortem.core.event.catacomb

import me.owdding.mortem.core.catacombs.nodes.CatacombsNode
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

data class CatacombNodeChangeEvent<Previous : CatacombsNode<Previous>, Current : CatacombsNode<Current>>(val previous: CatacombsNode<Previous>?, val current: CatacombsNode<Current>) : SkyBlockEvent()

data class CatacombRoomChangeEvent(val previous: RoomNode?, val current: RoomNode) : SkyBlockEvent()

