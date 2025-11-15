package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.mortem.config.category.MiscConfig
import me.owdding.mortem.core.event.MortemRegisterCommandsEvent
import me.owdding.mortem.utils.GfsQueue
import me.owdding.mortem.utils.extensions.sendWithPrefix
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.dungeon.DungeonEnterEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent.Companion.argument
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.utils.command.EnumArgument
import tech.thatgravyboat.skyblockapi.utils.text.Text

@Module
object ItemRefill {

    @Subscription
    fun onCommand(event: MortemRegisterCommandsEvent) {
        event.register("refill") {
            thenCallback("item", EnumArgument<RefillItems>()) {
                refill(argument<RefillItems>("item"))
            }

            callback {
                refill(*MiscConfig.itemRefill)
            }
        }
    }

    @Subscription
    fun onEnter(event: DungeonEnterEvent) {
        if (!MiscConfig.automaticRefillOnEnter) return

        refill(*MiscConfig.itemRefill)
    }

    private fun refill(vararg items: RefillItems, sendOutput: Boolean = true) {
        val output = items.mapNotNull {
            val count = countItem(it.id)
            if (count < it.maxStackSize) {
                GfsQueue.add(it.id, it.maxStackSize - count)
                "${it.maxStackSize - count}x ${it.name}"
            } else null
        }

        if (sendOutput) {
            // TODO fancy colors
            Text.join("Item Refill | ", output.joinToString(", ")).sendWithPrefix()
        }
    }

    private fun countItem(id: SkyBlockId) = McPlayer.inventory.filter { it.getSkyBlockId() == id }.sumOf { it.count }

    enum class RefillItems(val maxStackSize: Int = 64, id: String? = null) {
        SPIRIT_LEAP(maxStackSize = 16),
        SUPERBOOM_TNT,
        ENDER_PEARL(maxStackSize = 16),
        DECOY,
        INFLATABLE_JERRY,
        ;

        val id = SkyBlockId.item(id ?: name)
    }
}
