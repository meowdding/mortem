package me.owdding.mortem.core.catacombs

import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.roommatching.CatacombMapMatcher
import me.owdding.mortem.core.event.CatacombJoinEvent
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.utils.Utils.post
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.dungeon.DungeonEnterEvent
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.level.PacketReceivedEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object CatacombsManager {

    var catacomb: Catacomb? = null
        private set

    @Subscription
    private fun DungeonEnterEvent.onDungeonEnter() {
        if (catacomb != null) reset()
        val catacomb = Catacomb(floor)
        this@CatacombsManager.catacomb = catacomb
        CatacombJoinEvent(catacomb).post()
    }

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    private fun PacketReceivedEvent.onPacket() {
        val catacomb = catacomb ?: return
        val map = this.packet as? ClientboundMapItemDataPacket ?: return

        map.colorPatch.ifPresent {
            CatacombMapMatcher.updateInstance(catacomb, it.mapColors)
        }
    }

    @Subscription(ServerChangeEvent::class)
    fun reset() {
        val instance = catacomb ?: return
        CatacombLeaveEvent(instance).post()
        catacomb = null
    }

}
