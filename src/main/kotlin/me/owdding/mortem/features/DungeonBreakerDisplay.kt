package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.lib.overlays.Position
import me.owdding.mortem.config.category.OverlayConfig
import me.owdding.mortem.config.category.OverlayPositions
import me.owdding.mortem.utils.CachedValue
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import me.owdding.mortem.utils.ticks
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.datatype.DataType
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.platform.drawString
import tech.thatgravyboat.skyblockapi.utils.extentions.getRawLore
import tech.thatgravyboat.skyblockapi.utils.extentions.parseFormattedInt
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextBuilder.append
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

@Module
@Overlay
object DungeonBreakerDisplay : MortemOverlay {

    val regex = "Charges: (?<current>\\d+)/(?<max>\\d+)⸕".toRegex()

    val BREAKER_CHARGES: DataType<Pair<Int, Int>> = DataType("breaking") {
        if (DataTypes.ID.factory(it) != "DUNGEONBREAKER") return@DataType null

        var output: Pair<Int, Int>? = null
        regex.anyMatch(it.getRawLore(), "current", "max") { (current, max) ->
            output = current.parseFormattedInt() to max.parseFormattedInt()
        }
        output
    }
    override val name: Component = Text.of("Dungeonbreaker Charges")
    override val enabled: Boolean get() = OverlayConfig.dungeonBreakerOverlay && SkyBlockIsland.THE_CATACOMBS.inIsland()
    override val position: Position = OverlayPositions.dungeonBreaker
    override val bounds: Pair<Int, Int> get() = display.get()?.let { McFont.width(it) to McFont.height } ?: (0 to 0)

    private val display = CachedValue(1.ticks) {
        val (current, max) = McPlayer.hotbar.firstNotNullOfOrNull { it.getData(BREAKER_CHARGES) } ?: return@CachedValue null

        Text.of("Charges: ") {
            color = TextColor.GRAY

            append("$current") { color = TextColor.YELLOW }
            append("/") { color = TextColor.GRAY }
            append("$max") { color = TextColor.YELLOW }
            append("⸕") { color = TextColor.RED }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val display = display.get() ?: return
        graphics.drawString(display, 0, 0, shadow = true)
    }

}
