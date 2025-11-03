package me.owdding.mortem.core.catacombs.roommatching

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Decoder
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.Encoder
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.core.event.ChunkEvent
import me.owdding.mortem.utils.tag.BlockTagKey
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import org.joml.Vector2i
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.util.*
import java.util.function.Function

@Module
object CatacombWorldMatcher {

    private val hashes: Multimap<Vector2i, Int> = MultimapBuilder.SetMultimapBuilder.hashKeys().hashSetValues().build()

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun updateWorld(updateChunk: ChunkEvent.ChunkLoadEvent) = scanChunk(updateChunk.chunk)

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun updateWorld(updateChunk: ChunkEvent.ChunkUpdateEvent) = scanChunk(updateChunk.chunk)

    fun scanChunk(chunkAccess: ChunkAccess) {
        val catacomb = CatacombsManager.catacomb ?: return
        val chunkPos = chunkAccess.pos
        val maxChunkX = -12 + (catacomb.size.boundaryX * 2)
        val maxChunkZ = -12 + (catacomb.size.boundaryY * 2)
        if (chunkPos.x !in -12..maxChunkX || chunkPos.z !in -12..maxChunkZ) return

        val center = BlockPos(chunkPos.getBlockX(7), 255, chunkPos.getBlockZ(7))
        hashColumn(chunkAccess, center)
        hashColumn(chunkAccess, center.relative(Direction.NORTH, 2))
    }

    fun createDirectionalHashes(chunkAccess: ChunkAccess, top: BlockPos): Map<Direction?, Int> {
        val chunkPos = chunkAccess.pos
        val center = BlockPos(chunkPos.getBlockX(7), 255, chunkPos.getBlockZ(7))
        return buildMap {
            this[null] = hashColumn(chunkAccess, center)
            this[Direction.NORTH] = hashColumn(chunkAccess, center.relative(Direction.NORTH, 2))
            this[Direction.SOUTH] = hashColumn(chunkAccess, center.relative(Direction.SOUTH, 2))
            this[Direction.EAST] = hashColumn(chunkAccess, center.relative(Direction.EAST, 2))
            this[Direction.WEST] = hashColumn(chunkAccess, center.relative(Direction.WEST, 2))
        }
    }

    fun hashColumn(chunkAccess: ChunkAccess, top: BlockPos): Int {
        val hash = Objects.hash(
            BlockPos.betweenClosed(top, top.below(255)).mapNotNull {
                val state: BlockState = chunkAccess.getBlockState(top)

                return@mapNotNull BuiltInRegistries.BLOCK.getKey(
                    if (state.isAir || state in BlockTagKey.IGNORED_BLOCKS) {
                        Blocks.AIR
                    } else state.block,
                )
            }.toTypedArray(),
        )
        hashes.put(Vector2i(top.x, top.z), hash)
        return hash
    }

    fun matchData(rooms: MutableSet<RoomNode>) {
    }

    @Subscription(CatacombLeaveEvent::class)
    fun onLeave() {
        hashes.clear()
    }
}
