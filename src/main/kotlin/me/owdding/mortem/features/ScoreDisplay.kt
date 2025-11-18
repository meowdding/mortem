package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.lib.builder.DisplayFactory
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
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextBuilder.append
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

@Module
@Overlay
object ScoreDisplay : MortemOverlay {

    override val name: Component = Text.of("Score Display")
    override val enabled: Boolean get() = OverlayConfig.scoreOverlay && SkyBlockIsland.THE_CATACOMBS.inIsland()
    override val position: Position = OverlayPositions.score
    override val bounds: Pair<Int, Int>
        get() = display?.let { it.getWidth() to it.getHeight() } ?: (0 to 0)


    private val display by CachedValue(2.ticks) {
        val score = ScoreCalculator.getScore() ?: return@CachedValue null

        DisplayFactory.vertical {
            string("Score: ") {
                color = MortemColors.BASE_TEXT
                append("${score.total} ") { color = MortemColors.HIGHLIGHT }
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
        display?.render(graphics, mouseX, mouseY, partialTicks)
    }
}
