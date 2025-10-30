package me.owdding.mortem

import me.owdding.ktmodules.Module
import me.owdding.lib.utils.MeowddingLogger
import me.owdding.mortem.generated.MortemModules
import net.fabricmc.api.ClientModInitializer
import net.minecraft.resources.ResourceLocation
import org.intellij.lang.annotations.Pattern
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI

@Module
object Mortem : ClientModInitializer, MeowddingLogger by MeowddingLogger.autoResolve() {

    const val MOD_ID = "mortem"

    override fun onInitializeClient() {
        info("MortemModules client initialized!")
        MortemModules.init { SkyBlockAPI.eventBus.register(it) }
    }

    fun id(@Pattern("[a-z_0-9\\/.-]+") path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
