package me.owdding.mortem.core.catacombs.types

import me.owdding.mortem.config.category.catacombs.CatacombsColorConfig

enum class CatacombRoomCheckmark(val provider: CatacombsColorProvider, val puzzleOnly: Boolean = false) : CatacombsColorProvider by provider {
    CLEARED(CatacombsColorConfig::clearedCheckmark),
    COMPLETE(CatacombsColorConfig::completeCheckmark),
    FAILED(CatacombsColorConfig::failedCheckmark, true),
    NONE(CatacombsColorConfig::noneCheckmark),
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
