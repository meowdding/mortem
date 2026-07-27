package me.owdding.mortem.config.category.catacombs

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.mortem.config.AutoTranslated

object CatacombsConfig : CategoryKt("catacombs"), AutoTranslated {
    override val translationBase: String = "mortem.config.catacombs"
    override val name = Translated(translationBase)
}
