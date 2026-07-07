package me.owdding.mortem.core.catacombs.types

import me.owdding.mortem.config.category.catacombs.CatacombsColorConfig

enum class CatacombRoomType(val provider: CatacombsColorProvider, val canHaveCheckmarks: Boolean = true) : CatacombsColorProvider by provider {
    NORMAL(CatacombsColorConfig::normalRoom),
    RARE(CatacombsColorConfig::rareRoom),
    TRAP(CatacombsColorConfig::trapRoom),
    FAIRY(CatacombsColorConfig::fairyRoom, false),
    PUZZLE(CatacombsColorConfig::puzzleRoom),
    MINIBOSS(CatacombsColorConfig::minibossRoom),
    BLOOD(CatacombsColorConfig::bloodRoom),
    START(CatacombsColorConfig::startRoom, false),
    UNKNOWN(CatacombsColorConfig::unknownRoom),
    DEFAULT(CatacombsColorConfig::defaultRoom, false),
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
