package me.owdding.mortem.utils

import me.owdding.ktmodules.AutoCollect
import me.owdding.ktmodules.Module
import me.owdding.lib.events.overlay.FinishOverlayEditingEvent
import me.owdding.lib.overlays.Overlay
import me.owdding.lib.overlays.Overlays
import me.owdding.mortem.Mortem
import me.owdding.mortem.config.ConfigManager
import me.owdding.mortem.generated.MortemOverlays
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription

interface MortemOverlay : Overlay {
    override val modId: String get() = Mortem.MOD_ID
}

@Module
object MortemOverlaysHandler {
    init {
        MortemOverlays.collected.forEach { Overlays.register(it) }
    }

    @Subscription
    fun finishEditing(event: FinishOverlayEditingEvent) {
        if (event.modId == Mortem.MOD_ID) {
            ConfigManager.save()
        }
    }
}

@AutoCollect("Overlays")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Overlay
