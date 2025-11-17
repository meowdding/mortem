package me.owdding.mortem.core.event

import me.owdding.ktmodules.Module
import me.owdding.mortem.utils.Utils.post
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.AbstractModRegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent

class MortemRegisterCommandsEvent(baseEvent: RegisterCommandsEvent) : AbstractModRegisterCommandsEvent(baseEvent, "mortem") {
    @Module
    companion object {
        @Subscription
        fun onRegisterCommands(event: RegisterCommandsEvent) {
            MortemRegisterCommandsEvent(event).post()
        }
    }
}
