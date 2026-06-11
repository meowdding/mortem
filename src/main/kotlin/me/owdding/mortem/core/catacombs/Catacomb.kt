package me.owdding.mortem.core.catacombs

import me.owdding.mortem.core.Instance
import me.owdding.mortem.core.InstanceType
import me.owdding.mortem.core.catacombs.nodes.CatacombNodeType
import me.owdding.mortem.core.catacombs.nodes.CatacombsNode
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.Utils.unsafeCast
import org.joml.Vector2i
import org.joml.minus
import org.joml.plus
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.utils.extentions.filterValuesNotNull
import java.util.concurrent.ConcurrentHashMap
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.types.CatacombSize
import me.owdding.mortem.core.event.catacomb.CatacombNodeChangeEvent
import me.owdding.mortem.core.event.catacomb.CatacombRoomChangeEvent
import me.owdding.mortem.utils.Utils.post
import tech.thatgravyboat.skyblockapi.helpers.McPlayer

data class Catacomb(
    val floor: DungeonFloor,
) : Instance {
    var size: CatacombSize = CatacombSize.forFloor(floor)

    var mapTopLeft: Vector2i? = null
    var mapRoomSize: Int = 0
        set(value) {
            mapRoomAndDoorSize = value + DOOR_WIDTH
            field = value
        }
    var mapRoomAndDoorSize: Int = 0

    var lastNode: CatacombsNode<*>? = null
    var lastRoom: RoomNode? = null
    var lastPosition = Vector2i(-1, -1)

    val grid: MutableMap<Vector2i, CatacombsNode<*>> = ConcurrentHashMap()

    fun <T : CatacombsNode<T>> getOrCreateNode(position: Vector2i, type: CatacombNodeType<T>) : T = grid.getOrPut(position) {
        type.constructor.invoke(this)
    }.unsafeCast()
    inline fun <reified T : CatacombsNode<T>> getNodeOrNull(position: Vector2i) : T? = grid[position] as? T

    inline fun <reified T : CatacombsNode<T>> getNeighbours(position: Vector2i): Map<Vector2i, T> = buildList {
        add(position + Utils.vectorOneZero)
        add(position + Utils.vectorZeroOne)
        add(position - Utils.vectorOneZero)
        add(position - Utils.vectorZeroOne)
    }.associateWith { grid[it] as? T }.filterValuesNotNull()

    fun tick() {
        val player = McPlayer.self ?: return
        val nextPosition = CatacombsManager.worldPosToGridPos(player.blockPosition())
        val currentNode = lastNode

        val nextNode = grid[nextPosition] ?: return
        if (currentNode == nextNode) return
        lastNode = nextNode
        lastPosition = nextPosition
        if (nextNode is RoomNode && nextNode != lastRoom) {
            CatacombRoomChangeEvent(currentNode as? RoomNode ?: lastRoom, nextNode).post()
            lastRoom = nextNode
        }
        CatacombNodeChangeEvent(currentNode, nextNode).post()
    }

    override val instance: InstanceType get() = InstanceType.CATACOMBS
}


