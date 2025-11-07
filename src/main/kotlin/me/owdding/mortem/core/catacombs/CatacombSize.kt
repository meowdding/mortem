package me.owdding.mortem.core.catacombs

import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor.*

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
            E -> TINY
            F1, M1 -> SMALL
            F2, M2, F3, M3 -> NORMAL
            F4, M4 -> LARGE
            F5, M5, F6, M6 -> GIGANTIC
            else -> COLOSSAL
        }
    }
}

enum class MapSize(
    val roomWidth: Int,
    val offset: Int,
) {
    SMALL(22, 0),
    LARGE(20, 1)
}
