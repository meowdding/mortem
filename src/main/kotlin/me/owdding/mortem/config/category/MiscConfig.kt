package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.mortem.config.AutoTranslated
import me.owdding.mortem.features.ItemRefill

object MiscConfig : CategoryKt("misc"), AutoTranslated {
    override val translationBase: String = "mortem.config.misc"
    override val name = Translated(translationBase)

    var itemRefill by autoSelect(ItemRefill.RefillItems.ENDER_PEARL)

    var automaticRefillOnEnter by autoBoolean(false) {
        condition = { false }
    }

}
