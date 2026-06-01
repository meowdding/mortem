package me.owdding.mortem.core.catacombs.roommatching

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.hash.Hashing
import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.CatacombRoomType
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.DoorNode
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.core.event.ChunkEvent
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.opaque
import me.owdding.mortem.utils.tag.BlockTagKey
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.phys.AABB
import org.joml.Vector2i
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.math.sign

@Module
object CatacombWorldMatcher {

    private val hashes: Multimap<Vector2i, Int> = MultimapBuilder.SetMultimapBuilder.hashKeys().hashSetValues().build()
    val heightmap: MutableMap<Vector2i, Int> = mutableMapOf()
    private val todo: MutableSet<RoomNode> = mutableSetOf()

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun updateWorld(updateChunk: ChunkEvent.ChunkLoadEvent) = scanChunk(updateChunk.chunk)

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun updateWorld(updateChunk: ChunkEvent.ChunkUpdateEvent) = scanChunk(updateChunk.chunk)

    private val hashDisabledBlocks = setOf(
        Blocks.IRON_BARS,
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.PLAYER_HEAD,
        Blocks.PLAYER_WALL_HEAD
    )

    fun hashChunk(chunk: ChunkAccess): Int {
        val pos = chunk.pos
        val highestBlock = chunk.highestBlock(7, 7, hashDisabledBlocks)
        val coreString = buildString {
            val lowestBlock = chunk.lowestBlock(7, 7, hashDisabledBlocks)

            append(highestBlock)
            append(lowestBlock)

            val pos = BlockPos.MutableBlockPos().setX(7).setZ(7)

            for (i in highestBlock downTo lowestBlock) {
                val blockState = chunk.getBlockState(pos.setY(i))

                if (blockState.block in hashDisabledBlocks) {
                    append("Block{minecraft:air}")
                } else {
                    append(blockState.block.toString())
                }
            }
        }

        hashes.put(Vector2i(pos.getBlockX(7), pos.getBlockZ(7)), coreString.hashCode() - 43067951)
        return highestBlock
    }


    fun ChunkAccess.highestBlock(x: Int, z: Int, hiddenBlocks: Set<Block> = setOf()): Int {
        val pos = BlockPos.MutableBlockPos().setX(x).setZ(z)
        for (y in this.maxY downTo this.minY) {
            val state = getBlockState(pos.setY(y))
            if (!state.isAir && state.block !in hiddenBlocks) {
                return y
            }
        }
        return this.minY - 1
    }

    fun ChunkAccess.lowestBlock(x: Int, z: Int, hiddenBlocks: Set<Block> = setOf()): Int {
        val pos = BlockPos.MutableBlockPos().setX(x).setZ(z)
        for (y in this.minY..this.maxY) {
            val state = getBlockState(pos.setY(y))
            if (!state.isAir && state.block !in hiddenBlocks) {
                return y
            }
        }
        return this.maxY + 1
    }

    fun scanChunk(chunkAccess: ChunkAccess) {
        val catacomb = CatacombsManager.catacomb ?: return
        val chunkPos = chunkAccess.pos
        val maxChunkX = -12 + (catacomb.size.boundaryX * 2)
        val maxChunkZ = -12 + (catacomb.size.boundaryY * 2)
        if (chunkPos.x !in -12..maxChunkX || chunkPos.z !in -12..maxChunkZ) return
        val chunkCenter = chunkPos.getBlockAt(7, 0, 7)
        McClient.runNextTick {
            Gizmos.cuboid(
                AABB(chunkCenter).setMaxY(255.0),
                GizmoStyle.strokeAndFill(CatppuccinColors.Mocha.green.opaque(), 1f, CatppuccinColors.Mocha.teal.opaque())
            ).setAlwaysOnTop().persistForMillis(5000).fadeOut()
        }
        val highest = hashChunk(chunkAccess)
        matchData(todo)
        val grid = CatacombsManager.worldPosToGridPos(chunkCenter)
        catacomb.getNodeOrNull<RoomNode>(grid)?.updateRotation(highest)
        heightmap[grid] = highest
        todo.removeIf { it.rotation != null }
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
            val center = it.getCenter()
            val centerHashes = hashes.get(center)
            val storedRoom = centerHashes.firstNotNullOfOrNull { hash -> CatacombsManager.backingRooms[hash] }
            if (storedRoom != null) {
                it.backingData = storedRoom
            } else {
                todo.add(it)
            }
        }
    }

    @Subscription(CatacombLeaveEvent::class)
    fun onLeave() {
        hashes.clear()
        heightmap.clear()
        todo.clear()
    }
}
