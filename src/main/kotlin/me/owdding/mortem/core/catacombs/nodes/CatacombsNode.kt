package me.owdding.mortem.core.catacombs.nodes

import me.owdding.mortem.core.catacombs.Catacomb
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.roommatching.CatacombWorldMatcher
import me.owdding.mortem.core.catacombs.types.CatacombDoorType
import me.owdding.mortem.core.catacombs.types.CatacombRoomCheckmark
import me.owdding.mortem.core.catacombs.types.CatacombRoomType
import me.owdding.mortem.core.catacombs.types.CatacombsColorProvider
import me.owdding.mortem.core.catacombs.types.StoredCatacombRoom
import me.owdding.mortem.utils.GizmoUtils
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.extensions.maxOfNotNullOrNull
import me.owdding.mortem.utils.extensions.mutableCopy
import me.owdding.mortem.utils.extensions.sendWithPrefix
import me.owdding.mortem.utils.extensions.toVec2d
import me.owdding.mortem.utils.opaque
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.AABB
import org.joml.*
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.text.Text
import kotlin.math.max
import kotlin.math.min

sealed class CatacombNodeType<T : CatacombsNode<T>>(val constructor: (Catacomb) -> T) {
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
    var catacomb: Catacomb,
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
    var catacomb: Catacomb,
    var roomType: CatacombRoomType = CatacombRoomType.DEFAULT,
) : CatacombsNode<RoomNode>(CatacombNodeType.Room, 50) {
    var shape: CatacombRoomShape = CatacombRoomShape.ONE_BY_ONE
    val positions: MutableSet<Vector2i> = mutableSetOf()
    private var checkmark: CatacombRoomCheckmark = CatacombRoomCheckmark.NONE
    var backingData: StoredCatacombRoom? = null
    var rotation: Rotation? = null

    override fun toString() = "Room[type=$roomType]"
    fun addPosition(position: Vector2i) {
        positions.add(position)
        calculateShape()
        calculateRotation()
    }

    fun checkmark(): CatacombRoomCheckmark? = checkmark.takeIf { roomType.canHaveCheckmarks }

    private fun calculateRotation() {
        val height = positions.maxOfNotNullOrNull { CatacombWorldMatcher.heightmap[it * 2] } ?: return
        updateRotation(height)
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
        val minY = positions.minBy { (y) -> y }.y

        return when (shape) {
            CatacombRoomShape.ONE_BY_ONE -> Utils.vectorZeroZero
            CatacombRoomShape.ONE_BY_TWO -> if (xSize == 1) Utils.vectorZeroOne else Utils.vectorOneZero
            CatacombRoomShape.ONE_BY_THREE -> if (xSize == 1) Utils.vectorZeroTwo else Utils.vectorTwoZero
            CatacombRoomShape.ONE_BY_FOUR -> if (xSize == 1) vectorZeroThree else vectorThreeZero
            CatacombRoomShape.TWO_BY_TWO -> Utils.vectorOneOne
            CatacombRoomShape.STAIR -> {
                val xNodes = positions.count { (x) -> x == minX }
                val yNodes = positions.count { (y) -> y == minY }

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
        -12 + positions.minBy { (y) -> y }.y * 2,
    )

    override fun getColor(): Int {
        if (backingData?.roomType == CatacombRoomType.RARE) {
            return CatacombRoomType.RARE.getColor()
        }
        return roomType.getColor()
    }

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
        Text.of(origin.toString()).sendWithPrefix()
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

    fun updateRotation(highest: Int) {
        val level = McLevel.self ?: return
        if (rotation != null) return

        if (backingData?.roomType == CatacombRoomType.FAIRY) {
            rotation = Rotation.NONE
            return
        }

        for (node in positions) {
            val node = CatacombsManager.gridPosToWorldPos(node * 2)

            GizmoUtils.debugGizmo {
                cuboid(
                    AABB(node).setMaxY(255.0),
                    GizmoStyle.strokeAndFill(CatppuccinColors.Mocha.mauve.opaque(), 1f, CatppuccinColors.Mocha.pink.opaque()),
                ).setAlwaysOnTop().persistForMillis(10000).fadeOut()
            }

            for (clayRotation in ClayRotations.availableRotations) {

                val pos = BlockPos(
                    node.x + clayRotation.clayX,
                    highest,
                    node.z + clayRotation.clayZ,
                )

                GizmoUtils.debugGizmo {
                    cuboid(
                        AABB(pos),
                        GizmoStyle.strokeAndFill(CatppuccinColors.Mocha.green.opaque(), 1f, CatppuccinColors.Mocha.teal.opaque()),
                    ).setAlwaysOnTop().persistForMillis(10000).fadeOut()
                }

                val blockState = level.getBlockState(pos)

                when (blockState.block) {
                    Blocks.BLUE_TERRACOTTA -> {
                        if (airNeighbours(pos) < 2) continue

                        rotation = clayRotation.rotation

                        return
                    }

                    else -> {}
                }
            }
        }
    }

    // The correct clay block will always neighbour 2 air blocks 🥺
    private fun airNeighbours(pos: BlockPos): Int = Direction.Plane.HORIZONTAL.count {
        GizmoUtils.debugGizmo {
            cuboid(
                AABB(pos.relative(it)),
                GizmoStyle.strokeAndFill(CatppuccinColors.Mocha.red.opaque(), 1f, CatppuccinColors.Mocha.maroon.opaque()),
            ).setAlwaysOnTop().persistForMillis(10000).fadeOut()
        }
        McLevel[pos.relative(it)].isAir
    }

    fun mutateCheckmark(checkmark: CatacombRoomCheckmark) {
        when {
            roomType != CatacombRoomType.PUZZLE && checkmark.puzzleOnly -> return
            !roomType.canHaveCheckmarks -> return
            !this.checkmark.canMutateTo(checkmark) -> return
            else -> this.checkmark = checkmark
        }
    }
}

enum class ClayRotations(val clayX: Int, val clayZ: Int, val rotation: Rotation) {
    NORTH(15, 15, Rotation.CLOCKWISE_180),
    EAST(-15, 15, Rotation.COUNTERCLOCKWISE_90),
    SOUTH(-15, -15, Rotation.NONE),
    WEST(15, -15, Rotation.CLOCKWISE_90),
    ;

    companion object {
        val availableRotations = listOf(NORTH, EAST, SOUTH, WEST)
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
