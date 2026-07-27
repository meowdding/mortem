package me.owdding.mortem.config

import com.google.gson.JsonObject
import com.teamresourceful.resourcefulconfig.api.types.info.ResourcefulConfigLink
import com.teamresourceful.resourcefulconfig.api.types.options.TranslatableValue
import com.teamresourceful.resourcefulconfigkt.api.ConfigKt
import me.owdding.mortem.Mortem
import me.owdding.mortem.config.category.CommandsConfig
import me.owdding.mortem.config.category.MiscConfig
import me.owdding.mortem.config.category.NotifierConfig
import me.owdding.mortem.config.category.OverlayConfig
import me.owdding.mortem.config.category.OverlayPositions
import me.owdding.mortem.config.category.catacombs.CatacombsMapConfig
import me.owdding.mortem.config.category.catacombs.CatacombsColorConfig
import me.owdding.mortem.config.category.catacombs.CatacombsConfig
import java.util.function.UnaryOperator

object Config : ConfigKt("mortem/config"), AutoTranslated {

    override val name: TranslatableValue = TranslatableValue("Mortem")
    override val description: TranslatableValue = TranslatableValue("Mortem (${Mortem.VERSION})")
    override val links: Array<ResourcefulConfigLink> = emptyArray()

    init {
        category(OverlayConfig)
        category(NotifierConfig)
        category(MiscConfig)
        category(CommandsConfig)
        category(CatacombsConfig) {
            categories(CatacombsColorConfig, CatacombsMapConfig)
        }

        category(OverlayPositions)
    }


    override val patches: Map<Int, UnaryOperator<JsonObject>> = emptyMap()
    override val version: Int = patches.size
    override val translationBase: String = "mortem.config"
}
