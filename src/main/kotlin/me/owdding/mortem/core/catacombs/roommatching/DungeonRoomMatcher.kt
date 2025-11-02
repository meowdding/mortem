package me.owdding.mortem.core.catacombs.roommatching

import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.CatacombsManager
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.TimePassed
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object DungeonRoomMatcher {

    @Subscription(TickEvent::class)
    @TimePassed("2t")
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun tryMatch() {
        val catacomb = CatacombsManager.catacomb ?: return
    }
}
