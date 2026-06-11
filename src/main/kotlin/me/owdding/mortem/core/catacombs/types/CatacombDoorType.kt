package me.owdding.mortem.core.catacombs.types

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
