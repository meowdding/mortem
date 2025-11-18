package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.lib.builder.ComponentFactory
import me.owdding.lib.overlays.Position
import me.owdding.mortem.config.category.OverlayConfig
import me.owdding.mortem.config.category.OverlayPositions
import me.owdding.mortem.utils.CachedValue
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.colors.MortemColors
import me.owdding.mortem.utils.ticks
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.platform.drawString
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.width
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import tech.thatgravyboat.skyblockapi.utils.text.TextUtils.splitLines

@Module
@Overlay
object ScoreDisplay : MortemOverlay {

    override val name: Component = Text.of("Score Display")
    override val enabled: Boolean get() = OverlayConfig.scoreOverlay && SkyBlockIsland.THE_CATACOMBS.inIsland()
    override val position: Position = OverlayPositions.score
    override val bounds: Pair<Int, Int>
        get() = display?.let {
            it.width to it.splitLines().size * McFont.height
        } ?: (0 to 0)


    private val display by CachedValue(2.ticks) {
        val score = ScoreCalculator.getScore() ?: return@CachedValue null

        ComponentFactory.multiline {
            string("Score: ") {
                color = MortemColors.BASE_TEXT
                append("${score.total}") { color = MortemColors.HIGHLIGHT }
                append("(") { color = MortemColors.SEPARATOR }
                append(score.rank.component)
                append(")") { color = MortemColors.SEPARATOR }
            }
            string(" - Skill: ") {
                color = MortemColors.BASE_TEXT
                append("${score.skill}") { color = CatppuccinColors.Mocha.pink }
            }
            string(" - Exploration: ") {
                color = MortemColors.BASE_TEXT
                append("${score.exploration}") { color = CatppuccinColors.Mocha.yellow }
            }
            string(" - Speed: ") {
                color = MortemColors.BASE_TEXT
                append("${score.speed}") { color = CatppuccinColors.Mocha.green }
            }
            string(" - Bonus: ") {
                color = MortemColors.BASE_TEXT
                append("${score.bonus}") { color = CatppuccinColors.Mocha.blue }
            }

        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        display?.let { graphics.drawString(it, 0, 0) }
    }
}
