package me.owdding.mortem.core.catacombs.nodes

import me.owdding.mortem.core.catacombs.CatacombDoorType
import me.owdding.mortem.core.catacombs.CatacombRoomType
import me.owdding.mortem.core.catacombs.CatacombsColorProvider
import me.owdding.mortem.core.catacombs.StoredCatacombRoom
import me.owdding.mortem.generated.MortemCodecs
import me.owdding.mortem.utils.StructureUtils
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.extensions.mutableCopy
import me.owdding.mortem.utils.extensions.sendWithPrefix
import me.owdding.mortem.utils.extensions.toVec2d
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.world.level.block.Rotation
import org.joml.*
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.text.Text
import kotlin.math.max
import kotlin.math.min

sealed class CatacombNodeType<T : CatacombsNode<T>>(val constructor: () -> T) {
    object Unknown : CatacombNodeType<UnknownNode>({ UnknownNode })
    object Void : CatacombNodeType<VoidNode>({ VoidNode })
    object Door : CatacombNodeType<DoorNode>(::DoorNode)
    object Room : CatacombNodeType<RoomNode>(::RoomNode)
}

abstract class CatacombsNode<T : CatacombsNode<T>>(
    val type: CatacombNodeType<T>,
    val dimensions: Int,
) : CatacombsColorProvider

object UnknownNode : CatacombsNode<UnknownNode>(CatacombNodeType.Unknown, 0) {
    override fun toString() = "Unknown"
    override fun getColor(): Int = 0xF0F0b0
}

object VoidNode : CatacombsNode<VoidNode>(CatacombNodeType.Void, 0) {
    override fun toString() = "Void"
    override fun getColor(): Int = 0x696969
}

class DoorNode(
    var doorType: CatacombDoorType = CatacombDoorType.DEFAULT,
) : CatacombsNode<DoorNode>(CatacombNodeType.Door, 10) {
    override fun toString() = "Door"
    fun mutateType(type: CatacombDoorType) {
        doorType = when (doorType) {
            CatacombDoorType.DEFAULT -> type
            else -> doorType
        }
    }

    override fun getColor() = doorType.getColor()
}

class RoomNode(
    var roomType: CatacombRoomType = CatacombRoomType.DEFAULT,
) : CatacombsNode<RoomNode>(CatacombNodeType.Room, 50) {
    var shape: CatacombRoomShape = CatacombRoomShape.ONE_BY_ONE
    val positions: MutableSet<Vector2i> = mutableSetOf()
    var backingData: StoredCatacombRoom? = null
    var rotation: Rotation? = null

    override fun toString() = "Room[type=$roomType]"
    fun addPosition(position: Vector2i) {
        positions.add(position)
        calculateShape()
    }

    fun calculateShape() {
        val positions = positions
        val xSize = positions.distinctBy { it.x }.count()
        val ySize = positions.distinctBy { it.y }.count()
        val minSize = min(xSize, ySize)
        val maxSize = max(xSize, ySize)

        shape = when {
            positions.size == 1 -> CatacombRoomShape.ONE_BY_ONE
            positions.size == 2 -> CatacombRoomShape.ONE_BY_TWO
            xSize == ySize && positions.size == 4 -> CatacombRoomShape.TWO_BY_TWO
            minSize == 1 && maxSize == 3 -> CatacombRoomShape.ONE_BY_THREE
            minSize == 1 && maxSize == 4 -> CatacombRoomShape.ONE_BY_FOUR
            else -> CatacombRoomShape.STAIR
        }
    }

    private val vectorThreeZero = Vector2i(3, 0)
    private val vectorZeroThree = Vector2i(0, 3)

    @Suppress("IntroduceWhenSubject")
    fun getMiddleChunkOffset(): Vector2i? {
        val xSize = positions.distinctBy { it.x }.count()
        val minX = positions.minBy { (x) -> x }.x
        val minY = positions.minBy { (_, y) -> y }.y

        return when (shape) {
            CatacombRoomShape.ONE_BY_ONE -> Utils.vectorZeroZero
            CatacombRoomShape.ONE_BY_TWO -> if (xSize == 1) Utils.vectorZeroOne else Utils.vectorOneZero
            CatacombRoomShape.ONE_BY_THREE -> if (xSize == 1) Utils.vectorZeroTwo else Utils.vectorTwoZero
            CatacombRoomShape.ONE_BY_FOUR -> if (xSize == 1) vectorZeroThree else vectorThreeZero
            CatacombRoomShape.TWO_BY_TWO -> Utils.vectorOneOne
            CatacombRoomShape.STAIR -> {
                val xNodes = positions.count { (x) -> x == minX }
                val yNodes = positions.count { (_, y) -> y == minY }

                when {
                    xNodes == 2 && yNodes == 2 -> Utils.vectorZeroZero
                    xNodes == 2 && yNodes == 1 -> Utils.vectorZeroTwo
                    xNodes == 1 && yNodes == 2 -> Utils.vectorTwoZero
                    xNodes == 1 && yNodes == 1 -> Utils.vectorTwoTwo
                    else -> null
                }
            }
        }
    }

    fun minMiddleChunkPos(): Vector2i = Vector2i(
        -12 + positions.minBy { (x) -> x }.x * 2,
        -12 + positions.minBy { (_, y) -> y }.y * 2,
    )

    override fun getColor() = roomType.getColor()
    fun mutateType(type: CatacombRoomType) {
        roomType = when (roomType) {
            CatacombRoomType.DEFAULT, CatacombRoomType.UNKNOWN -> type
            else -> roomType
        }
    }

    fun getCenter(): Vector2i = minMiddleChunkPos().add(getMiddleChunkOffset() ?: Utils.vectorZeroZero).mul(16).add(7, 7)

    fun worldToRoom(vec3d: Vector3dc): Vector3dc {
        val origin = getCenter().toVec2d().add(0.5, 0.5)
        Text.of(origin.toString()).sendWithPrefix()
        val original = vec3d.mutableCopy().sub(origin.x, 0.0, origin.y)
        return when (rotation) {
            Rotation.CLOCKWISE_90 -> Vector3d(original.z(), original.y(), -original.x())
            Rotation.CLOCKWISE_180 -> Vector3d(-original.x(), original.y(), -original.z())
            Rotation.COUNTERCLOCKWISE_90 -> Vector3d(-original.z(), original.y(), original.x())
            else -> original
        }
    }

    fun worldToRoom(vec3i: Vector3ic): Vector3ic {
        val origin = getCenter()
        val original = vec3i.mutableCopy().sub(origin.x, 0, origin.y)
        return when (rotation) {
            Rotation.CLOCKWISE_90 -> Vector3i(original.z(), original.y(), -original.x())
            Rotation.CLOCKWISE_180 -> Vector3i(-original.x(), original.y(), -original.z())
            Rotation.COUNTERCLOCKWISE_90 -> Vector3i(-original.z(), original.y(), original.x())
            else -> original
        }
    }

    fun roomToWorld(vec3i: Vector3ic): Vector3i {
        val room = when (rotation) {
            Rotation.COUNTERCLOCKWISE_90 -> Vector3i(vec3i.z(), vec3i.y(), -vec3i.x())
            Rotation.CLOCKWISE_180 -> Vector3i(-vec3i.x(), vec3i.y(), -vec3i.z())
            Rotation.CLOCKWISE_90 -> Vector3i(-vec3i.z(), vec3i.y(), vec3i.x())
            else -> vec3i.mutableCopy()
        }
        val origin = getCenter()
        return room.add(origin.x, 0, origin.y)
    }

    fun roomToWorld(vec3d: Vector3dc): Vector3d {
        val room = when (rotation) {
            Rotation.COUNTERCLOCKWISE_90 -> Vector3d(vec3d.z(), vec3d.y(), -vec3d.x())
            Rotation.CLOCKWISE_180 -> Vector3d(-vec3d.x(), vec3d.y(), -vec3d.z())
            Rotation.CLOCKWISE_90 -> Vector3d(-vec3d.z(), vec3d.y(), vec3d.x())
            else -> vec3d.mutableCopy()
        }
        val origin = getCenter().toVec2d().add(0.5, 0.5)
        return room.add(origin.x, 0.0, origin.y)
    }

    fun exportToStructure(): CompoundTag? {

        val chunks = buildList {
            for (first in positions) {
                add(
                    Vector2i(
                        (-12 + first.x * 2) * 16 + 7,
                        (-12 + first.y * 2) * 16 + 7,
                    ),
                )
                for (second in positions) {
                    if ((first.x - second.x == 1) != (first.y - second.y == 1)) {
                        add(
                            Vector2i(
                                (-12 + first.x + second.x) * 16 + 7,
                                (-12 + first.y + second.y) * 16 + 7,
                            ),
                        )
                    }
                }
            }
            if (shape == CatacombRoomShape.TWO_BY_TWO) {
                val centerX = positions.sumOf { 2 * it.x } / positions.size
                val centerY = positions.sumOf { 2 * it.y } / positions.size

                add(
                    Vector2i(
                        (-12 + centerX) * 16 + 7,
                        (-12 + centerY) * 16 + 7,
                    ),
                )
            }
        }

        val roomStructure = StructureUtils.encodeStructureFromRegions(
            McLevel.level,
            chunks.map { center ->
                BlockPos(center.x - 15, 0, center.y - 15) to BlockPos(center.x + 15, 255, center.y + 15)
            },
            rotation ?: Rotation.NONE,
        ) ?: return null

        if (backingData != null) {
            val storedRoomCodec = MortemCodecs.getCodec<StoredCatacombRoom>()

            val roomData = storedRoomCodec.encodeStart(NbtOps.INSTANCE, backingData).getOrThrow()

            roomStructure.put("backing_room_data", roomData)
        }

        return roomStructure
    }
}

enum class CatacombRoomShape {
    ONE_BY_ONE,
    ONE_BY_TWO,
    ONE_BY_THREE,
    ONE_BY_FOUR,
    TWO_BY_TWO,
    STAIR,
}
