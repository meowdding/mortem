package me.owdding.mortem.core.catacombs

import me.owdding.mortem.core.catacombs.types.CatacombClass
import me.owdding.mortem.core.catacombs.types.CatacombRating
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import kotlin.time.Duration

data class CatacombEndData(
    val floor: DungeonFloor,
    val catacombClass: CatacombClass,
    var score: Int? = null,
    var time: Duration? = null,
    var bits: Int? = null,
    var catacombsExperience: Float? = null,
    val classExperience: MutableMap<CatacombClass, Float> = mutableMapOf(),
    var damageDealt: Long? = null,
    var enemiesKilled: Int? = null,
    var secretsFound: Int? = null,
) {
    val ableToPost: Boolean get() = score != null && time != null && catacombsExperience != null && classExperience.isNotEmpty() && secretsFound != null

    val rating: CatacombRating? get() = CatacombRating.getByScore(score)
}
