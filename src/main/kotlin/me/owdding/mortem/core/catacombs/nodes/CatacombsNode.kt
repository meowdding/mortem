package me.owdding.mortem.core.catacombs.nodes

import me.owdding.mortem.core.catacombs.CatacombDoorType
import me.owdding.mortem.core.catacombs.CatacombRoomType
import me.owdding.mortem.core.catacombs.CatacombsColorProvider
import me.owdding.mortem.core.catacombs.StoredCatacombRoom
import me.owdding.mortem.utils.Utils
import net.minecraft.world.level.block.Rotation
import org.joml.Vector2i
import org.joml.component1
import org.joml.component2
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
}

enum class CatacombRoomShape {
    ONE_BY_ONE,
    ONE_BY_TWO,
    ONE_BY_THREE,
    ONE_BY_FOUR,
    TWO_BY_TWO,
    STAIR,
}
