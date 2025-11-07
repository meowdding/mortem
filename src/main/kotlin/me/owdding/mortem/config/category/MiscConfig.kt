package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.mortem.features.ItemRefill

object MiscConfig : CategoryKt("misc") {
    override val name = Translated("mortem.config.misc")

    var itemRefill by select(ItemRefill.RefillItems.ENDER_PEARL) {
        translation = "mortem.config.misc.item_refill"
    }

    var automaticRefillOnEnter by boolean(false) {
        translation = "mortem.config.misc.automatic_refill_on_enter"
        condition = { false }
    }

}
