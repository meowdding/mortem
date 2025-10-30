package me.owdding.mortem.config

import com.google.gson.JsonObject
import com.teamresourceful.resourcefulconfig.api.types.info.ResourcefulConfigLink
import com.teamresourceful.resourcefulconfig.api.types.options.TranslatableValue
import com.teamresourceful.resourcefulconfigkt.api.ConfigKt
import me.owdding.mortem.config.category.OverlayConfig
import me.owdding.mortem.config.category.OverlayPositions
import java.util.function.UnaryOperator

object Config : ConfigKt("mortem/config") {

    override val name: TranslatableValue = TranslatableValue("Mortem")
    override val description: TranslatableValue = TranslatableValue("Mortem (vTODO)")
    override val links: Array<ResourcefulConfigLink> = emptyArray()

    init {
        category(OverlayConfig)

        category(OverlayPositions)
    }


    override val patches: Map<Int, UnaryOperator<JsonObject>> = emptyMap()
    override val version: Int = patches.size + 1
}
