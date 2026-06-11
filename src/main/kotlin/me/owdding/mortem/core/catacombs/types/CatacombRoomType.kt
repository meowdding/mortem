package me.owdding.mortem.core.catacombs.types

enum class CatacombRoomType(val provider: CatacombsColorProvider, val canHaveCheckmarks: Boolean = true) : CatacombsColorProvider by provider {
    NORMAL({ 0xAb6b00 }),
    RARE({ 0xAb6b00 }),
    TRAP({ 0xFF7F0F }),
    FAIRY({ 0xF080FF }, false),
    PUZZLE({ 0xe050F0 }),
    MINIBOSS({ 0xFFFF00 }),
    BLOOD({ 0xFF0000 }),
    START({ 0x00FF00 }, false),
    UNKNOWN({ 0xababab }),
    DEFAULT({ 0x000000 }, false),
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
