package me.owdding.mortem.features.catacombs.map

import me.owdding.lib.overlays.ConfigPosition
import me.owdding.lib.overlays.Position
import me.owdding.mortem.utils.MortemOverlay
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.Text

class CatacombMapOverlay : MortemOverlay {
    override val name: Component = Text.of("Catacomb Map")
    override val position: Position = ConfigPosition(10, 10)
    override val bounds: Pair<Int, Int> = 100 to 100

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {

    }

}
