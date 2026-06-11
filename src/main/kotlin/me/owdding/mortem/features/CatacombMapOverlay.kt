package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.lib.overlays.Position
import me.owdding.mortem.config.category.OverlayPositions
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.CatacombRoomShape.*
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import me.owdding.mortem.utils.extensions.isHorizontalHallway
import me.owdding.mortem.utils.extensions.isVerticalHallway
import me.owdding.mortem.utils.opaque
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.Rotation
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.platform.scale
import tech.thatgravyboat.skyblockapi.utils.extentions.translated
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.asComponent
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.min

@Overlay
@Module
object CatacombMapOverlay : MortemOverlay {
    override val name: Component get() = Text.of("Catacomb Map")
    override val position: Position get() = OverlayPositions.dungeonMap
    override val bounds: Pair<Int, Int> = 20 to 20

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val catacomb = CatacombsManager.catacomb ?: return

        val roomWidth = 50
        val hallwayWidth = 4
        val combinedWidth = roomWidth + hallwayWidth

        val text= mutableListOf<() -> Unit>()
        catacomb.grid.forEach { [pos, node] ->

            val isVerticalDoor = pos.isVerticalHallway
            val isHorizontalDoor = pos.isHorizontalHallway
            val (x, y) = pos

            val width = min(if (isHorizontalDoor) hallwayWidth else roomWidth, node.dimensions)
            val height = min(if (isVerticalDoor) hallwayWidth else roomWidth, node.dimensions)

            val xOffset = (x / 2) * combinedWidth + if (isHorizontalDoor) roomWidth else (roomWidth - width) / 2
            val yOffset = (y / 2) * combinedWidth + if (isVerticalDoor) roomWidth else (roomWidth - height) / 2

            val roomNode = node as? RoomNode
            graphics.fill(
                xOffset,
                yOffset,
                xOffset + width,
                yOffset + height,
                ARGB.opaque(node.getColor())
            )

            val data = roomNode?.backingData
            if (data != null && CatacombsManager.worldPosToGridPos(roomNode.getCenter()) == pos) {
                fun renderScaledOrNormal(x: Int, y: Int, component: Component, maxWidth: Int) = text.add {
                    val componentWidth = McFont.width(component)
                    val scale = if (componentWidth > maxWidth) {
                        maxWidth / componentWidth.toFloat()
                    } else 1f

                    graphics.translated(x, y) {
                        graphics.scale(scale, scale)
                        graphics.centeredText(McFont.self, component, 0, 0, -1)
                    }
                }

                val textWidth: Int
                val x: Int
                val y: Int
                when (roomNode.shape) {
                    ONE_BY_ONE -> {
                        x = xOffset + width / 2
                        y = yOffset + height / 2 - McFont.height / 2
                        textWidth = roomWidth
                    }
                    ONE_BY_TWO, ONE_BY_FOUR -> {
                        val isHorizontal = roomNode.rotation == Rotation.NONE || roomNode.rotation == Rotation.CLOCKWISE_180

                        if (isHorizontal) {
                            textWidth = combinedWidth + roomWidth
                            x = xOffset- hallwayWidth / 2
                            y = yOffset+ height / 2 - McFont.height / 2
                        } else {
                            x = xOffset + width / 2
                            textWidth = roomWidth
                            y = yOffset - hallwayWidth / 2 - McFont.height / 2
                        }
                    }
                    ONE_BY_THREE -> {
                        val isHorizontal = roomNode.rotation == Rotation.NONE || roomNode.rotation == Rotation.CLOCKWISE_180

                        if (isHorizontal) {
                            textWidth = combinedWidth + roomWidth
                            x = xOffset + roomWidth / 2
                            y = yOffset + height / 2 - McFont.height / 2
                        } else {
                            x = xOffset + roomWidth / 2
                            textWidth = roomWidth
                            y = yOffset - hallwayWidth / 2 - McFont.height / 2
                        }
                    }
                    TWO_BY_TWO -> {
                        x = xOffset + hallwayWidth / 2
                        y = yOffset + hallwayWidth / 2 - McFont.height / 2
                        textWidth = combinedWidth + roomWidth
                    }
                    STAIR -> {
                        x = xOffset + roomWidth/ 2
                        y = yOffset + roomWidth/ 2 - McFont.height / 2
                        textWidth = roomWidth
                    }
                }
                val name = data.name.asComponent {
                    this.color = roomNode.checkmark()?.getColor()?.opaque() ?: -1
                }
                if (data.secretCount != 0) {
                    renderScaledOrNormal(x, y - McFont.height / 2, name, textWidth)
                    renderScaledOrNormal(x, y + McFont.height / 2, "0 / ${data.secretCount}".asComponent(), textWidth)
                } else {
                    renderScaledOrNormal(x, y, name, textWidth)
                }
            }
        }

        text.forEach { runnable ->
            runnable()
        }

        super.extract(graphics, mouseX, mouseY)
    }
}
