package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.lib.overlays.ConfigPosition

object OverlayConfig  : CategoryKt("overlays") {
    override val name = Translated("mortem.config.overlays")

    var dungeonBreakerOverlay by boolean(true) {
        translation = "mortem.config.overlays.dungeon_breaker_overlay"
    }

    var dungeonBreakerOverlayPrefix by boolean(true) {
        translation = "mortem.config.overlays.dungeon_breaker_overlay_prefix"
    }
}

object OverlayPositions : CategoryKt("overlaysPositions") {
    override val hidden: Boolean = true

    val dungeonBreaker by obj(ConfigPosition(100, 200))
}
