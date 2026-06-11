package me.owdding.mortem.core.catacombs.types

enum class CatacombRoomCheckmark(val provider: CatacombsColorProvider, val puzzleOnly: Boolean = false): CatacombsColorProvider by provider {
    CLEARED({ 0xffffff }),
    COMPLETE({ 0x00ff00}),
    FAILED({ 0xff0000 }, true),
    NONE({ 0xababab }),
    ;

    fun canMutateTo(other: CatacombRoomCheckmark) = when (this) {
        CLEARED if other == COMPLETE -> true
        COMPLETE -> false
        FAILED if other == NONE -> true
        NONE -> true
        else -> false
    }


    companion object {
        fun getByColor(color: CatacombMapColor): CatacombRoomCheckmark = when (color) {
            CatacombMapColor.COMPLETE -> COMPLETE
            CatacombMapColor.FAILED -> FAILED
            CatacombMapColor.CLEARED -> CLEARED
            else -> NONE
        }
    }
}
