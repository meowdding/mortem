package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt

object CommandsConfig : CategoryKt("commands") {
    override val name = Translated("mortem.config.commands")

    var join by boolean(true) {
        translation = "mortem.config.commands.join"
    }
}
