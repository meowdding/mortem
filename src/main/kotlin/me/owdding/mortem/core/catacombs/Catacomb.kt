package me.owdding.mortem.core.catacombs

import me.owdding.mortem.core.Instance
import me.owdding.mortem.core.InstanceType
import me.owdding.mortem.core.catacombs.nodes.CatacombsNode
import me.owdding.mortem.core.catacombs.nodes.NodeType
import me.owdding.mortem.utils.Utils.unsafeCast
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Rotation
import org.joml.Vector2i
import org.joml.Vector3f
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor

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

    var grid: MutableMap<Vector2i, CatacombsNode<*>> = mutableMapOf()

    fun <T : CatacombsNode<T>> getOrCreateNode(position: Vector2i, type: NodeType<T>) : T = grid.getOrPut(position, type.constructor).unsafeCast()

    override val instance: InstanceType get() = InstanceType.CATACOMBS
}

enum class CatacombRoomType {
    NORMAL,
    TRAP,
    FAIRY,
    PUZZLE,
    BLOOD,
    START,
    UNKNOWN,
    DEFAULT,
    ;

    companion object {
        fun getByColor(color: CatacombMapColor): CatacombRoomType? = when (color) {
            CatacombMapColor.COMPLETE -> START
            CatacombMapColor.UNKNOWN -> UNKNOWN
            CatacombMapColor.FAILED -> BLOOD
            CatacombMapColor.PUZZLE -> PUZZLE
            CatacombMapColor.TRAP -> TRAP
            CatacombMapColor.FAIRY -> FAIRY
            CatacombMapColor.NORMAL -> NORMAL
            else -> null
        }
    }

}

enum class CatacombDoorType {
    WITHER,
    BLOOD,
    NORMAL,
    DEFAULT,
;

    companion object {
        fun getByColor(color: CatacombMapColor): CatacombDoorType? = when (color) {
            CatacombMapColor.FAILED -> BLOOD
            CatacombMapColor.NORMAL -> NORMAL
            CatacombMapColor.WITHER -> WITHER
            CatacombMapColor.UNKNOWN -> DEFAULT
            else -> null
        }
    }
}

data class StoredCatacombRoom(
    var name: String,
    var centerHash: Long,
    var directionalHashes: Map<Long, Direction>,
)

data class CatacombRoom(
    val roomType: CatacombRoomType,
    val center: Vector3f,
    val rotation: Rotation,
)
