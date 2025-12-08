package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfig.api.types.info.Translatable
import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.lib.overlays.ConfigPosition
import me.owdding.mortem.config.separator

object OverlayConfig  : CategoryKt("overlays") {
    val translation = "mortem.config.overlays"
    override val name = Translated(translation)

    init {
        separator("$translation.dungeonbreaker_separator")
    }

    var dungeonBreakerOverlay by boolean(true) {
        translation = "$translation.dungeonbreaker_overlay"
    }

    var dungeonBreakerOverlayPrefix by boolean(true) {
        translation = "$translation.dungeonbreaker_overlay_prefix"
    }

    var dungeonBreakerShowWhenHolding by boolean(false) {
        translation = "$translation.dungeonbreaker_show_when_holding"
    }

    init {
        separator("$translation.score_separator")
    }

    var scoreOverlay by boolean(true) {
        translation = "$translation.score_overlay"
    }

    var displayMode by enum(DisplayMode.DETAILED) {
        translation = "$translation.score_display_mode"
    }
}

enum class DisplayMode : Translatable {
    DETAILED, COMPACT, SHORT;

    override fun getTranslationKey() = "mortem.config.overlays.display_mode.$name".lowercase()
}

object OverlayPositions : CategoryKt("overlaysPositions") {
    override val hidden: Boolean = true

    val dungeonBreaker by obj(ConfigPosition(100, 200))
    val score by obj(ConfigPosition(10, 500) )
}
