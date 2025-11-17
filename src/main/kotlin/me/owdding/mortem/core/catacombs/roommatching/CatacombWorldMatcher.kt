package me.owdding.mortem.core.catacombs.roommatching

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import kotlin.math.sign
import me.owdding.ktmodules.Module
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.catacombs.CatacombRoomType
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.CatacombRoomShape
import me.owdding.mortem.core.catacombs.nodes.DoorNode
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.core.event.ChunkEvent
import me.owdding.mortem.utils.extensions.mutableCopy
import me.owdding.mortem.utils.tag.BlockTagKey
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.chunk.ChunkAccess
import org.joml.Vector2i
import org.joml.Vector2ic
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McLevel

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

    private val matchedKeys = mutableSetOf<Vector2ic>()
    fun scanChunk(chunkAccess: ChunkAccess) {
        val catacomb = CatacombsManager.catacomb ?: return
        val chunkPos = chunkAccess.pos
        val maxChunkX = -12 + (catacomb.size.boundaryX * 2)
        val maxChunkZ = -12 + (catacomb.size.boundaryY * 2)
        if (chunkPos.x !in -12..maxChunkX || chunkPos.z !in -12..maxChunkZ) return

        val center = BlockPos(chunkPos.getBlockX(7), 255, chunkPos.getBlockZ(7))
        cacheColumnHash(Vector2i(center.x, center.z), hashColumn(center))
        hashColumn(center, Direction.NORTH)

        if (Mortem.isDebugEnabled()) {
            hashes.keys().filterNot(matchedKeys::contains).forEach {
                val hashes = hashes.get(it)
                val room = hashes.firstNotNullOfOrNull(CatacombsManager.backingRooms::get) ?: return@forEach
                val roomNode = RoomNode(room.type)
                val roomPos = CatacombsManager.worldPosToGridPos(it)
                val directionHashes = CatacombWorldMatcher.hashes.get(it.add(0, -4))
                val direction = directionHashes.firstNotNullOfOrNull { hash -> room.directionalHashes[hash] }

                val rotation = when (direction) {
                    Direction.EAST -> Rotation.COUNTERCLOCKWISE_90
                    Direction.SOUTH -> Rotation.CLOCKWISE_180
                    Direction.WEST -> Rotation.CLOCKWISE_90
                    Direction.NORTH -> Rotation.NONE
                    else -> null
                }

                val otherPos = buildList {
                    when (room.shape) {
                        CatacombRoomShape.TWO_BY_TWO -> {
                            for (x in -1..1) {
                                for (y in -1..1) {
                                    if (x == 0 && y == 0) continue
                                    add(Vector2i(x, y))
                                }
                            }
                        }
                        CatacombRoomShape.STAIR -> {
                            add(Vector2i(1, 0))
                            add(Vector2i(2, 0))
                            add(Vector2i(0, -1))
                            add(Vector2i(0, -2))
                        }
                        CatacombRoomShape.ONE_BY_TWO -> {
                            add(Vector2i(1, 0))
                            add(Vector2i(-1, 0))
                        }
                        CatacombRoomShape.ONE_BY_THREE -> {
                            add(Vector2i(1, 0))
                            add(Vector2i(2, 0))
                            add(Vector2i(-1, 0))
                            add(Vector2i(-2, 0))
                        }
                        CatacombRoomShape.ONE_BY_FOUR -> {
                            add(Vector2i(1, 0))
                            add(Vector2i(2, 0))
                            add(Vector2i(3, 0))
                            add(Vector2i(-1, 0))
                            add(Vector2i(-2, 0))
                            add(Vector2i(-3, 0))
                        }
                        else -> return@buildList
                    }
                }.mapNotNull { rotation?.let { rotation -> it.rotate(rotation) } }

                roomNode.backingData = room
                roomNode.rotation = rotation
                catacomb.grid[roomPos] = roomNode
                otherPos.forEach {
                    catacomb.grid[it.add(roomPos)] = roomNode
                }
            }
        }

        matchData(todo)
        todo.removeIf { it.rotation != null }
    }

    private fun Vector2i.rotate(rotation: Rotation) = when (rotation) {
        Rotation.COUNTERCLOCKWISE_90 -> Vector2i(y(), -x())
        Rotation.CLOCKWISE_180 -> Vector2i(-x(), -y())
        Rotation.CLOCKWISE_90 -> Vector2i(-y(), x())
        else -> this.mutableCopy()
    }

    fun createDirectionalHashes(center: BlockPos, consumer: (String, List<String>) -> Unit = { _, _ -> }): Map<Direction?, String> {
        val center = center.atY(255)
        val hash = hashColumn(center)
        cacheColumnHash(Vector2i(center.x, center.z), hash)
        return buildMap {
            this[null] = hash
            this[Direction.NORTH] = hashColumn(center, Direction.NORTH, consumer).apply { consumer(this, emptyList()) }
            this[Direction.SOUTH] = hashColumn(center, Direction.SOUTH, consumer).apply { consumer(this, emptyList()) }
            this[Direction.EAST] = hashColumn(center, Direction.EAST, consumer).apply { consumer(this, emptyList()) }
            this[Direction.WEST] = hashColumn(center, Direction.WEST, consumer).apply { consumer(this, emptyList()) }
        }
    }

    @Suppress("UnstableApiUsage")
    fun hashColumn(center: BlockPos, direction: Direction, consumer: (String, List<String>) -> Unit = { _, _ -> }): String {
        val hasher: Hasher = Hashing.sha256().newHasher()
        val offset = center.relative(direction, 4)
        hasher.putString(hashColumn(offset, consumer), Charsets.UTF_8)
        hasher.putString(hashColumn(offset.relative(direction.clockWise, 3), consumer), Charsets.UTF_8)
        hasher.putString(hashColumn(offset.relative(direction.counterClockWise, 5), consumer), Charsets.UTF_8)
        hasher.putString(hashColumn(center.relative(direction.opposite, 2), consumer), Charsets.UTF_8)
        hasher.putString(hashColumn(center.relative(direction.clockWise, 4), consumer), Charsets.UTF_8)

        val hash = hasher.hash().asBytes().toHexString()
        cacheColumnHash(Vector2i(offset.x, offset.z), hash)
        return hash
    }

    fun cacheColumnHash(pos: Vector2i, hash: String) {
        hashes.put(pos, hash)
    }

    @Suppress("UnstableApiUsage")
    fun hashColumn(top: BlockPos, consumer: (String, List<String>) -> Unit = { _, _ -> }): String {
        val hasher = Hashing.sha256().newHasher()
        val blocks = mutableListOf<String>()
        BlockPos.betweenClosed(top, top.below(255)).forEach {
            hasher.putString(
                blockAt(it).apply {
                    blocks.add("${it.y} - $this")
                },
                Charsets.UTF_8,
            )
        }
        return hasher.hash().asBytes().toHexString().apply {
            consumer(this, blocks)
        }
    }

    fun blockAt(blockPos: BlockPos): String {
        val state = McLevel.self.getBlockState(blockPos)
        return BuiltInRegistries.BLOCK.getKey(
            if (state.isAir || state in BlockTagKey.IGNORED_BLOCKS) Blocks.AIR else state.block,
        ).toString()
    }

    fun RoomNode.rotationForOneByOne(): Direction? {
        val catacomb = CatacombsManager.catacomb ?: return null
        val gridPosition = positions.first()
        val doors = catacomb.getNeighbours<DoorNode>(gridPosition)
        if (doors.size == 1) {
            val (position) = doors.entries.first()
            return when {
                (gridPosition.x - position.x).sign == 1 -> Direction.WEST
                (gridPosition.x - position.x).sign == -1 -> Direction.EAST
                (gridPosition.y - position.y).sign == 1 -> Direction.NORTH
                (gridPosition.y - position.y).sign == -1 -> Direction.SOUTH
                else -> null
            }
        }
        return null
    }

    fun matchData(rooms: MutableSet<RoomNode>) {
        rooms.filter { it.roomType != CatacombRoomType.UNKNOWN }.forEach {
            val center = it.getCenter()
            val centerHashes = hashes.get(center)
            val directionHashes = hashes.get(center.add(0, -4))
            val storedRoom = centerHashes.firstNotNullOfOrNull { hash -> CatacombsManager.backingRooms[hash] }
            if (storedRoom != null) {
                it.backingData = storedRoom

                val direction = (if (storedRoom.extraRotationHandling && it.shape == CatacombRoomShape.ONE_BY_ONE) {
                    it.rotationForOneByOne()
                } else null) ?: directionHashes.firstNotNullOfOrNull { hash -> storedRoom.directionalHashes[hash] }

                matchedKeys.add(center)
                matchedKeys.add(center.add(0, -4))
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
        matchedKeys.clear()
    }
}
