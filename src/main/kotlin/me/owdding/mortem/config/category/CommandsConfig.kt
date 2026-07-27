package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.mortem.config.AutoTranslated

object CommandsConfig : CategoryKt("commands"), AutoTranslated {
    override val translationBase: String = "mortem.config.commands"
    override val name = Translated(translationBase)

    var join by autoBoolean(true)
}
