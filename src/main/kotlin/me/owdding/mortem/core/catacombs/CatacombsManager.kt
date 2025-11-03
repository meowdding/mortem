package me.owdding.mortem.core.catacombs

import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.roommatching.CatacombMapMatcher
import me.owdding.mortem.core.event.CatacombJoinEvent
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.core.event.ChunkEvent
import me.owdding.mortem.utils.Utils.post
import me.owdding.mortem.utils.extensions.sendWithPrefix
import me.owdding.mortem.utils.extensions.toBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.ChunkPos
import org.joml.Vector2i
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.dungeon.DungeonEnterEvent
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.level.PacketReceivedEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send
import kotlin.math.abs
import kotlin.math.floor

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

    @Subscription
    fun command(event: RegisterCommandsEvent) {
        event.registerWithCallback("mortem dev create_room") {
            val instance = catacomb ?: return@registerWithCallback
            val gridPosition = worldPosToGridPos(this.source.position.toBlockPos())
            val node = instance.grid[gridPosition] as? RoomNode ?: return@registerWithCallback
            val origin = node.minMiddleChunkPos()
            val offset = node.getMiddleChunkOffset()

            if (offset == null) {
                Text.of("Offset is null").sendWithPrefix()
            } else {
                val chunkPos = origin.add(offset)
                val pos = ChunkPos(chunkPos.x, chunkPos.y)
                val middle = BlockPos(pos.getBlockX(7), 0, pos.getBlockZ(7))
                Text.of("Center pos is $middle ($origin, $offset)").sendWithPrefix()
            }
        }
    }

    fun worldPosToGridPos(pos: BlockPos): Vector2i {
        val chunkX = floor(pos.x / 16f).toInt()
        val chunkY = floor(pos.z / 16f).toInt()
        val chunkRelativeX  = pos.x and 15
        val chunkRelativeY  = pos.z and 15

        val isHallwayX = abs(chunkX) % 2 == 1
        val isHallwayY = abs(chunkY) % 2 == 1
        val baseGridPosX = chunkX + 12
        val baseGridPosY = chunkY + 12

        val gridPosX = when {
            isHallwayX && chunkRelativeX > 7 -> baseGridPosX + 1
            isHallwayX && chunkRelativeX < 7 -> baseGridPosX - 1
            else -> baseGridPosX
        }

        val gridPosY = when {
            isHallwayY && chunkRelativeY > 7 -> baseGridPosY + 1
            isHallwayY && chunkRelativeY < 7 -> baseGridPosY - 1
            else -> baseGridPosY
        }

        return Vector2i(gridPosX, gridPosY)
    }

}
