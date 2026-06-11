package me.owdding.mortem.core.catacombs.types

import me.owdding.mortem.core.catacombs.MapSize
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor

enum class CatacombSize(
    val xRooms: Int,
    val yRooms: Int,
    val mapSize: MapSize,
    val boundaryX: Int = xRooms,
    val boundaryY: Int = yRooms,
) {

    TINY(4, 4, MapSize.SMALL),
    SMALL(4, 5, MapSize.SMALL),
    NORMAL(5, 5, MapSize.SMALL),
    LARGE(6, 5, MapSize.LARGE),
    GIGANTIC(6, 5, MapSize.LARGE, 6, 6),
    COLOSSAL(6, 6, MapSize.LARGE),
    ;

    companion object {

        fun forFloor(floor: DungeonFloor): CatacombSize = when (floor) {
            DungeonFloor.E -> TINY
            DungeonFloor.F1, DungeonFloor.M1 -> SMALL
            DungeonFloor.F2, DungeonFloor.M2, DungeonFloor.F3, DungeonFloor.M3 -> NORMAL
            DungeonFloor.F4, DungeonFloor.M4 -> LARGE
            DungeonFloor.F5, DungeonFloor.M5, DungeonFloor.F6, DungeonFloor.M6 -> GIGANTIC
            else -> COLOSSAL
        }
    }
}
