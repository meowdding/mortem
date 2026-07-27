package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.lib.overlays.ConfigPosition
import me.owdding.mortem.config.AutoTranslated
import me.owdding.mortem.config.separator

object OverlayConfig : CategoryKt("overlays"), AutoTranslated {
    override val translationBase = "mortem.config.overlays"
    override val name = Translated(translationBase)

    init {
        separator("$translationBase.dungeon_breaker")
    }

    var dungeonBreakerOverlay by autoBoolean("dungeon_breaker-overlay", true)

    var dungeonBreakerOverlayPrefix by autoBoolean("dungeon_breaker-prefix", true)

    var dungeonBreakerShowWhenHolding by autoBoolean("dungeon_breaker-show_when_holding", false)
}

object OverlayPositions : CategoryKt("overlaysPositions") {
    override val hidden: Boolean = true

    val dungeonBreaker by obj(ConfigPosition(100, 200))
    val dungeonMap by obj(ConfigPosition(0, 0))
}
