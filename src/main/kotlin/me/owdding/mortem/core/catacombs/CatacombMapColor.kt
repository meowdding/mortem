package me.owdding.mortem.core.catacombs

import net.minecraft.world.level.material.MapColor

enum class CatacombMapColor(
    val mapColor: MapColor,
    val brightness: MapColor.Brightness,
) {

    /** Also used for entrance */
    COMPLETE(MapColor.PLANT, MapColor.Brightness.HIGH),

    /** Also used for blood */
    FAILED(MapColor.FIRE, MapColor.Brightness.HIGH),
    CLEARED(MapColor.SNOW, MapColor.Brightness.HIGH),
    NORMAL(MapColor.COLOR_ORANGE, MapColor.Brightness.LOWEST),
    PUZZLE(MapColor.COLOR_MAGENTA, MapColor.Brightness.HIGH),
    MINIBOSS(MapColor.COLOR_YELLOW, MapColor.Brightness.HIGH),
    FAIRY(MapColor.COLOR_PINK, MapColor.Brightness.HIGH),
    TRAP(MapColor.COLOR_ORANGE, MapColor.Brightness.HIGH),
    WITHER(MapColor.COLOR_BLACK, MapColor.Brightness.LOWEST),
    UNKNOWN(MapColor.COLOR_GRAY, MapColor.Brightness.NORMAL),
    NONE(MapColor.NONE, MapColor.Brightness.LOW),
    ;

    val packedId = mapColor.getPackedId(brightness)
    val colorId = mapColor.id
    val brightnessId = brightness.id

    companion object {
        fun getByPackedId(id: Byte?): CatacombMapColor = when (id) {
            COMPLETE.packedId -> COMPLETE
            FAILED.packedId -> FAILED
            CLEARED.packedId -> CLEARED
            NORMAL.packedId -> NORMAL
            PUZZLE.packedId -> PUZZLE
            MINIBOSS.packedId -> MINIBOSS
            FAIRY.packedId -> FAIRY
            TRAP.packedId -> TRAP
            WITHER.packedId -> WITHER
            UNKNOWN.packedId -> UNKNOWN
            else -> NONE
        }
    }
}
