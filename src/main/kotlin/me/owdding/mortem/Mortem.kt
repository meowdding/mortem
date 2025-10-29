package me.owdding.mortem

import me.owdding.mortem.events.BootstrapConditionalPropertiesEvent
import me.owdding.mortem.events.BootstrapNumericPropertiesEvent
import me.owdding.mortem.events.BootstrapSelectPropertiesEvent
import me.owdding.mortem.generated.CatharsisModules
import me.owdding.mortem.utils.CatharsisLogger
import me.owdding.mortem.utils.geometry.BakedBedrockGeometry
import me.owdding.mortem.utils.geometry.BedrockGeometryBaker
import me.owdding.mortem.utils.geometry.BedrockGeometryParser
import me.owdding.ktmodules.Module
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties
import net.minecraft.resources.ResourceLocation
import org.intellij.lang.annotations.Pattern
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import java.nio.file.Files
import java.nio.file.Path

@Module
object Mortem : ClientModInitializer, CatharsisLogger by CatharsisLogger.autoResolve() {

    const val MOD_ID = "mortem"
    override fun onInitializeClient() {
        info("MortemModules client initialized!")
        MortemModules.init { SkyBlockAPI.eventBus.register(it) }
    }

    fun id(@Pattern("[a-z_0-9\\/.-]+") path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath("catharsis", path)
    fun mc(@Pattern("[a-z_0-9\\/.-]+") path: String): ResourceLocation = ResourceLocation.withDefaultNamespace(path)
    fun sbapi(@Pattern("[a-z_0-9\\/.-]+") path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(SkyBlockAPI.MOD_ID, path)
}
