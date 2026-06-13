package me.owdding.mortem.core.catacombs.types

enum class CatacombClass(val display: String, val colorProvider: CatacombsColorProvider, val relic: CatacombM7Relic): CatacombsColorProvider by colorProvider {
    ARCHER("Archer", { 0xff00ff }, CatacombM7Relic.RED),
    BERSERKER("Berserk", { 0xff00ff }, CatacombM7Relic.ORANGE),
    HEALER("Healer", { 0xff00ff }, CatacombM7Relic.PURPLE),
    MAGE("Mage", { 0xff00ff }, CatacombM7Relic.BLUE),
    TANK("Tank", { 0xff00ff }, CatacombM7Relic.GREEN),
    ;

    companion object {
        fun byName(catacombClass: String): CatacombClass? = when (catacombClass.lowercase()) {
            "mage" -> MAGE
            "tank" -> TANK
            "healer" -> HEALER
            "bers", "berserk", "berserker" -> BERSERKER
            "arch", "archer" -> ARCHER
            else -> null
        }
    }
}
