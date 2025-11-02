package me.owdding.mortem.core.catacombs.nodes

import me.owdding.mortem.core.catacombs.CatacombDoorType
import me.owdding.mortem.core.catacombs.CatacombRoomType
import org.joml.Vector2i

sealed interface NodeType<T : CatacombsNode<T>> {
    val constructor: () -> T

    companion object {
        private class NodeTypeImpl<T : CatacombsNode<T>>(override val constructor: () -> T) : NodeType<T>

        private inline fun <reified T : CatacombsNode<T>> create(noinline constructor: () -> T) = NodeTypeImpl(constructor)

        val UNKNOWN: NodeType<UnknownNode> = create { UnknownNode }
        val VOID: NodeType<VoidNode> = create { VoidNode }
        val DOOR: NodeType<DoorNode> = create(::DoorNode)
        val ROOM: NodeType<RoomNode> = create(::RoomNode)
    }
}

open class CatacombsNode<T : CatacombsNode<T>>(
    val type: NodeType<T>,
    val dimensions: Int,
)

object UnknownNode : CatacombsNode<UnknownNode>(NodeType.UNKNOWN, 0) {
    override fun toString() = "Unknown"
}

object VoidNode : CatacombsNode<VoidNode>(NodeType.VOID, 0) {
    override fun toString() = "Void"
}

class DoorNode(
    var doorType: CatacombDoorType = CatacombDoorType.DEFAULT,
) : CatacombsNode<DoorNode>(NodeType.DOOR, 10) {
    override fun toString() = "Door"
}

class RoomNode(
    var roomType: CatacombRoomType = CatacombRoomType.DEFAULT,
) : CatacombsNode<RoomNode>(NodeType.ROOM, 50) {
    val positions: MutableList<Vector2i> = mutableListOf()

    override fun toString() = "Room[type=$roomType]"
    fun addPosition(position: Vector2i) {

    }
}
