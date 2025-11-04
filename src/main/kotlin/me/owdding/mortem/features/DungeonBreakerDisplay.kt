package me.owdding.mortem.features

import me.owdding.lib.overlays.Position
import me.owdding.mortem.config.category.OverlayConfig
import me.owdding.mortem.config.category.OverlayPositions
import me.owdding.mortem.utils.CachedValue
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import me.owdding.mortem.utils.ticks
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.platform.drawString
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextBuilder.append
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

@Overlay
object DungeonBreakerDisplay : MortemOverlay {

    override val name: Component = Text.of("Dungeonbreaker Charges")
    override val enabled: Boolean get() = OverlayConfig.dungeonBreakerOverlay && SkyBlockIsland.THE_CATACOMBS.inIsland()
    override val position: Position = OverlayPositions.dungeonBreaker
    override val bounds: Pair<Int, Int> get() = display.getValue()?.let { McFont.width(it) to McFont.height } ?: (0 to 0)

    private val display = CachedValue(1.ticks) {
        val (current, max) = McPlayer.inventory.firstNotNullOfOrNull { it.getData(DataTypes.DUNGEONBREAKER_CHARGES) } ?: return@CachedValue null

        Text.of {
            color = TextColor.GRAY

            if (OverlayConfig.dungeonBreakerOverlayPrefix) {
                append("Charges: ")
            }
            append("$current") { color = TextColor.YELLOW }
            append("/")
            append("$max") { color = TextColor.YELLOW }
            append("⸕") { color = TextColor.RED }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val display = display.getValue() ?: return
        graphics.drawString(display, 0, 0, shadow = true)
    }

}
