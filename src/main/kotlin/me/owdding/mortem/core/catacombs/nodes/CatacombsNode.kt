package me.owdding.mortem.core.catacombs.nodes

import me.owdding.mortem.core.catacombs.CatacombDoorType
import me.owdding.mortem.core.catacombs.CatacombRoomType
import me.owdding.mortem.core.catacombs.CatacombsColorProvider
import org.joml.Vector2i

sealed interface CatacombNodeType<T : CatacombsNode<T>> {
    val constructor: () -> T

    companion object {
        private class NodeTypeImpl<T : CatacombsNode<T>>(override val constructor: () -> T) : CatacombNodeType<T>

        private inline fun <reified T : CatacombsNode<T>> create(noinline constructor: () -> T) = NodeTypeImpl(constructor)

        val UNKNOWN: CatacombNodeType<UnknownNode> = create { UnknownNode }
        val VOID: CatacombNodeType<VoidNode> = create { VoidNode }
        val DOOR: CatacombNodeType<DoorNode> = create(::DoorNode)
        val ROOM: CatacombNodeType<RoomNode> = create(::RoomNode)
    }
}

abstract class CatacombsNode<T : CatacombsNode<T>>(
    val type: CatacombNodeType<T>,
    val dimensions: Int,
) : CatacombsColorProvider

object UnknownNode : CatacombsNode<UnknownNode>(CatacombNodeType.UNKNOWN, 0) {
    override fun toString() = "Unknown"
    override fun getColor(): Int = 0xF0F0b0
}

object VoidNode : CatacombsNode<VoidNode>(CatacombNodeType.VOID, 0) {
    override fun toString() = "Void"
    override fun getColor(): Int = 0x696969
}

class DoorNode(
    var doorType: CatacombDoorType = CatacombDoorType.DEFAULT,
) : CatacombsNode<DoorNode>(CatacombNodeType.DOOR, 10) {
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
) : CatacombsNode<RoomNode>(CatacombNodeType.ROOM, 50) {
    val positions: MutableList<Vector2i> = mutableListOf()

    override fun toString() = "Room[type=$roomType]"
    fun addPosition(position: Vector2i) {

    }

    override fun getColor() = roomType.getColor()
}
