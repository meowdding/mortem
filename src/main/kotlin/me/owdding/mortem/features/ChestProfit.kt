package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.lib.builder.LayoutFactory
import me.owdding.mortem.utils.InventorySideGui
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent

@Module
object ChestProfit : InventorySideGui(".* Chest") {
    override val enabled: Boolean
        get() = true

    override fun ContainerInitializedEvent.getLayout() = LayoutFactory.vertical {
        string("meow")
    }

}
