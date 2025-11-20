package me.owdding.mortem.utils

import java.util.Properties
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.io.path.Path
import kotlin.io.path.notExists
import kotlin.io.path.reader
import me.owdding.mortem.Mortem
import me.owdding.mortem.utils.extensions.sendWithPrefix
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.DebugToggle
import tech.thatgravyboat.skyblockapi.utils.DevUtils
import tech.thatgravyboat.skyblockapi.utils.extentions.parseFormattedInt

internal fun debugToggle(path: String, description: String = path): DebugToggle {
    return DebugToggle(Mortem.id(path), description, MortemDevUtils)
}

object MortemDevUtils : DevUtils() {
    override val commandName: String = "mortem"
    val properties: Map<String, String> = loadFromProperties()

    fun getInt(key: String, default: Int = 0): Int {
        return properties[key].parseFormattedInt(default)
    }

    fun getBoolean(key: String): Boolean {
        return properties[key] == "true"
    }

    fun getDebugBoolean(key: String) = Mortem.isDebugEnabled() && getBoolean(key)

    private fun loadFromProperties(): Map<String, String> {
        val properties = Properties()
        val path = System.getProperty("mortem.property_path")?.let { Path(it) } ?: McClient.config.resolve("mortem.properties")
        if (path.notExists()) return emptyMap()
        path.reader(Charsets.UTF_8).use {
            properties.load(it)
        }
        val map = mutableMapOf<String, String>()
        properties.forEach { (key, value) ->
            ResourceLocation.tryBySeparator(key.toString(), '@')?.let {
                if (value.toString() == "true") {
                    states[it] = true
                }
            }
            map[key.toString()] = value.toString()
        }
        return map
    }

    @Subscription
    fun registerCommadns(event: RegisterCommandsEvent) = super.onCommandRegister(event)
    override fun send(component: MutableComponent) = component.sendWithPrefix()
}
