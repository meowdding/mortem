package me.owdding.mortem.config

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen
import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import me.owdding.ktmodules.Module
import me.owdding.lib.overlays.EditOverlaysScreen
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.event.MortemRegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient

@Module
object ConfigManager {

    private val configurator = Configurator(Mortem.MOD_ID)
    val config = Config.register(configurator)

    fun save() = config.save()
    fun openConfig() = McClient.setScreenAsync { ResourcefulConfigScreen.getFactory(Mortem.MOD_ID).apply(null) }

    @Subscription
    fun onCommand(event: MortemRegisterCommandsEvent) {
        event.registerBaseCallback {
            openConfig()
        }
        event.registerWithCallback("overlays") {
            McClient.setScreenAsync { EditOverlaysScreen(Mortem.MOD_ID) }
        }
    }
}
