package me.owdding.mortem.core.catacombs.roommatching

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.hash.Hashing
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
import org.joml.plus
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object CatacombWorldMatcher {

    private val hashes: Multimap<Vector2i, String> = MultimapBuilder.SetMultimapBuilder.hashKeys().hashSetValues().build()

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

    fun createDirectionalHashes(chunkAccess: ChunkAccess): Map<Direction?, String> {
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

    fun hashColumn(chunkAccess: ChunkAccess, top: BlockPos): String {
        val hash = Hashing.sha256().hashString(BlockPos.betweenClosed(top, top.below(255)).joinToString("_") {
            val state: BlockState = chunkAccess.getBlockState(it)

            return@joinToString BuiltInRegistries.BLOCK.getKey(
                if (state.isAir || state in BlockTagKey.IGNORED_BLOCKS) {
                    Blocks.AIR
                } else state.block,
            ).toString()
        }, Charsets.UTF_8).asBytes()
        val hashString = hash.toHexString()
        hashes.put(Vector2i(top.x, top.z), hashString)
        return hashString
    }

    fun matchData(rooms: MutableSet<RoomNode>) {
        rooms.forEach {
            val origin = it.minMiddleChunkPos()
            val offset = it.getMiddleChunkOffset() ?: return@forEach
            val hashes = hashes.get((origin + offset).mul(16).add(7, 7))
            val storedRoom = hashes.firstNotNullOfOrNull { CatacombsManager.backingRooms[it] } ?: return@forEach
            it.backingData = storedRoom
        }
    }

    @Subscription(CatacombLeaveEvent::class)
    fun onLeave() {
        hashes.clear()
    }
}
