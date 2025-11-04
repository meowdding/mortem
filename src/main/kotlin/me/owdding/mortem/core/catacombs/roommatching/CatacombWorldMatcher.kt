package me.owdding.mortem.core.catacombs.roommatching

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.CatacombRoomType
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.core.event.ChunkEvent
import me.owdding.mortem.utils.tag.BlockTagKey
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
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
    private val todo: MutableSet<RoomNode> = mutableSetOf()

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
        cacheColumnHash(Vector2i(center.x, center.z), hashColumn(chunkAccess, center))
        hashColumn(chunkAccess, center, Direction.NORTH)

        matchData(todo)
        todo.removeIf { it.rotation != null }
    }

    fun createDirectionalHashes(chunkAccess: ChunkAccess): Map<Direction?, String> {
        val chunkPos = chunkAccess.pos
        val center = BlockPos(chunkPos.getBlockX(7), 255, chunkPos.getBlockZ(7))
        val hash = hashColumn(chunkAccess, center)
        cacheColumnHash(Vector2i(center.x, center.z), hash)
        return buildMap {
            this[null] = hash
            this[Direction.NORTH] = hashColumn(chunkAccess, center, Direction.NORTH)
            this[Direction.SOUTH] = hashColumn(chunkAccess, center, Direction.SOUTH)
            this[Direction.EAST] = hashColumn(chunkAccess, center, Direction.EAST)
            this[Direction.WEST] = hashColumn(chunkAccess, center, Direction.WEST)
        }
    }

    @Suppress("UnstableApiUsage")
    fun hashColumn(chunkAccess: ChunkAccess, centerBlock: BlockPos, direction: Direction): String {
        val hasher: Hasher = Hashing.sha256().newHasher()
        val offset = centerBlock.relative(direction, 4)
        hasher.putString(hashColumn(chunkAccess, offset), Charsets.UTF_8)
        hasher.putString(hashColumn(chunkAccess, offset.relative(direction.clockWise, 3)), Charsets.UTF_8)
        hasher.putString(hashColumn(chunkAccess, offset.relative(direction.counterClockWise, 5)), Charsets.UTF_8)
        hasher.putString(hashColumn(chunkAccess, centerBlock.relative(direction.opposite, 2)), Charsets.UTF_8)
        hasher.putString(hashColumn(chunkAccess, centerBlock.relative(direction.clockWise, 4)), Charsets.UTF_8)

        val hash = hasher.hash().asBytes().toHexString()
        cacheColumnHash(Vector2i(offset.x, offset.z), hash)
        return hash
    }

    fun cacheColumnHash(pos: Vector2i, hash: String) {
        hashes.put(pos, hash)
    }

    @Suppress("UnstableApiUsage")
    fun hashColumn(chunkAccess: ChunkAccess, top: BlockPos): String {
        val hasher = Hashing.sha256().newHasher()
        BlockPos.betweenClosed(top, top.below(255)).forEach {
            val state: BlockState = chunkAccess.getBlockState(it)

            hasher.putString(
                BuiltInRegistries.BLOCK.getKey(
                    if (state.isAir || state in BlockTagKey.IGNORED_BLOCKS) Blocks.AIR else state.block,
                ).toString(),
                Charsets.UTF_8,
            )
        }
        return hasher.hash().asBytes().toHexString()
    }

    fun matchData(rooms: MutableSet<RoomNode>) {
        rooms.filter { it.roomType != CatacombRoomType.UNKNOWN }.forEach {
            val origin = it.minMiddleChunkPos()
            val offset = it.getMiddleChunkOffset() ?: return@forEach
            val center = (origin + offset).mul(16).add(7, 7)
            val centerHashes = hashes.get(center)
            val directionHashes = hashes.get(center.add(0, -4))
            val storedRoom = centerHashes.firstNotNullOfOrNull { hash -> CatacombsManager.backingRooms[hash] }
            if (storedRoom != null) {
                it.backingData = storedRoom
                val direction = directionHashes.firstNotNullOfOrNull { hash -> storedRoom.directionalHashes[hash] }
                if (direction == null) {
                    todo.add(it)
                    return@forEach
                }
                it.rotation = when (direction) {
                    Direction.EAST -> Rotation.COUNTERCLOCKWISE_90
                    Direction.SOUTH -> Rotation.CLOCKWISE_180
                    Direction.WEST -> Rotation.CLOCKWISE_90
                    else -> Rotation.NONE
                }
            } else {
                todo.add(it)
            }
        }
    }

    @Subscription(CatacombLeaveEvent::class)
    fun onLeave() {
        hashes.clear()
        todo.clear()
    }
}
