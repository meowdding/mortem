package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.lib.builder.LayoutFactory
import me.owdding.mortem.utils.InventorySideGui
import me.owdding.mortem.utils.colors.MortemColors
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.item.calculator.getItemValue
import tech.thatgravyboat.skyblockapi.utils.extentions.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.toFormattedString
import tech.thatgravyboat.skyblockapi.utils.text.TextBuilder.append
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

@Module
object ChestProfit : InventorySideGui(".* Chest") {
    override val enabled: Boolean get() = true

    override fun ContainerInitializedEvent.getLayout() = LayoutFactory.vertical {
        val items = containerItems.filterNot { it.getSkyBlockId() == null }.associateWith { it.getItemValue() }

        string("TITLE")
        string("")
        items.forEach { (k, v) ->
            singularComponent {
                component(k.hoverName)
                string(": ") { color = TextColor.GRAY }
                string(v.price.toFormattedString()) { color = MortemColors.BETTER_GOLD }
            }
        }
        string("")
        string("Total: ") {
            color = TextColor.GRAY
            append(items.values.sumOf { it.price }.toFormattedString()) { color = MortemColors.BETTER_GOLD }
        }

        // TODO: chestprice, keyprice
    }

}
