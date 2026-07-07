package me.owdding.mortem.core.catacombs.types

import me.owdding.mortem.config.category.catacombs.CatacombsColorConfig

enum class CatacombDoorType(val provider: CatacombsColorProvider) : CatacombsColorProvider by provider {
    WITHER(CatacombsColorConfig::witherDoor),
    BLOOD(CatacombsColorConfig::bloodDoor),
    NORMAL(CatacombsColorConfig::normalDoor),
    TRAP(CatacombsColorConfig::trapDoor),
    MINIBOSS(CatacombsColorConfig::minibossDoor),
    PUZZLE(CatacombsColorConfig::puzzleDoor),
    FAIRY(CatacombsColorConfig::fairyDoor),
    DEFAULT(CatacombsColorConfig::defaultDoor),
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
