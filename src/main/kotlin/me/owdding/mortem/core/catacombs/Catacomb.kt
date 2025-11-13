package me.owdding.mortem.core.catacombs

import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.GenerateCodec
import me.owdding.mortem.core.Instance
import me.owdding.mortem.core.InstanceType
import me.owdding.mortem.core.catacombs.nodes.CatacombNodeType
import me.owdding.mortem.core.catacombs.nodes.CatacombsNode
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.Utils.unsafeCast
import net.minecraft.core.Direction
import org.joml.Vector2i
import org.joml.minus
import org.joml.plus
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.utils.extentions.filterValuesNotNull
import java.util.concurrent.ConcurrentHashMap
import me.owdding.mortem.core.catacombs.nodes.RoomNode
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

    operator fun get(position: Vector2i): CatacombsNode<*>? = grid[position]

    fun <T : CatacombsNode<T>> getOrCreateNode(position: Vector2i, type: CatacombNodeType<T>) : T = grid.getOrPut(position, type.constructor).unsafeCast()

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

fun interface CatacombsColorProvider {
    fun getColor(): Int
}

enum class CatacombRoomType(val provider: CatacombsColorProvider) : CatacombsColorProvider by provider {
    NORMAL({ 0xAb6b00 }),
    TRAP({ 0xFF7F0F }),
    FAIRY({ 0xF080FF }),
    PUZZLE({ 0xe050F0 }),
    MINIBOSS({ 0xFFFF00 }),
    BLOOD({ 0xFF0000 }),
    START({ 0x00FF00 }),
    UNKNOWN({ 0xababab }),
    DEFAULT({ 0x000000 }),
    ;

    companion object {
        fun getByColor(color: CatacombMapColor): CatacombRoomType? = when (color) {
            CatacombMapColor.COMPLETE -> START
            CatacombMapColor.UNKNOWN -> UNKNOWN
            CatacombMapColor.FAILED -> BLOOD
            CatacombMapColor.PUZZLE -> PUZZLE
            CatacombMapColor.TRAP -> TRAP
            CatacombMapColor.MINIBOSS -> MINIBOSS
            CatacombMapColor.FAIRY -> FAIRY
            CatacombMapColor.NORMAL -> NORMAL
            else -> null
        }
    }

}

enum class CatacombDoorType(val provider: CatacombsColorProvider) : CatacombsColorProvider by provider {
    WITHER({ 0x4f4f4f }),
    BLOOD({ 0xFF0000 }),
    NORMAL({ 0xab6b00 }),
    TRAP({ 0xff7f0f }),
    MINIBOSS({ 0xFFFF00 }),
    PUZZLE({ 0xe060f0 }),
    FAIRY({ 0xf080ff }),
    DEFAULT({ 0x000000 }),
;

    companion object {
        fun getByColor(color: CatacombMapColor): CatacombDoorType? = when (color) {
            CatacombMapColor.FAILED -> BLOOD
            CatacombMapColor.NORMAL -> NORMAL
            CatacombMapColor.WITHER -> WITHER
            CatacombMapColor.FAIRY -> FAIRY
            CatacombMapColor.PUZZLE -> PUZZLE
            CatacombMapColor.TRAP -> TRAP
            CatacombMapColor.MINIBOSS -> MINIBOSS
            CatacombMapColor.UNKNOWN -> DEFAULT
            else -> null
        }
    }
}

@GenerateCodec
data class StoredCatacombRoom(
    var name: String,
    var id: String,
    var secrets: Int = 0,
    @FieldName("center") val centerHash: String,
    @FieldName("directions") val directionalHashes: Map<String, Direction>,
    @FieldName("extra_rotation_handling") val extraRotationHandling: Boolean = false,
) {
    var shouldSerialize = false

    fun markChange() {
        shouldSerialize = true
    }
}
