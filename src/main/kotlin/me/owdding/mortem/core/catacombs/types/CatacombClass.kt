package me.owdding.mortem.core.catacombs.types

import me.owdding.mortem.config.category.catacombs.CatacombsColorConfig

enum class CatacombClass(val display: String, val colorProvider: CatacombsColorProvider, val relic: CatacombM7Relic): CatacombsColorProvider by colorProvider {
    ARCHER("Archer", CatacombsColorConfig::archer, CatacombM7Relic.RED),
    BERSERKER("Berserk", CatacombsColorConfig::berserker, CatacombM7Relic.ORANGE),
    HEALER("Healer", CatacombsColorConfig::healer, CatacombM7Relic.PURPLE),
    MAGE("Mage", CatacombsColorConfig::mage, CatacombM7Relic.BLUE),
    TANK("Tank", CatacombsColorConfig::tank, CatacombM7Relic.GREEN),
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
