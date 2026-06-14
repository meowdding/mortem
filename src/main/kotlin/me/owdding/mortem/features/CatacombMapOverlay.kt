package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.lib.overlays.Position
import me.owdding.mortem.config.category.OverlayPositions
import me.owdding.mortem.core.catacombs.Catacomb
import me.owdding.mortem.core.catacombs.CatacombPlayer
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.CatacombRoomShape.*
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import me.owdding.mortem.utils.extensions.isHallway
import me.owdding.mortem.utils.extensions.isHorizontalHallway
import me.owdding.mortem.utils.extensions.isVerticalHallway
import me.owdding.mortem.utils.opaque
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.Rotation
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.minus
import org.joml.times
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.platform.rotate
import tech.thatgravyboat.skyblockapi.platform.scale
import tech.thatgravyboat.skyblockapi.utils.extentions.translated
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.asComponent
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

@Overlay
@Module
object CatacombMapOverlay : MortemOverlay {
    override val name: Component get() = Text.of("Catacomb Map")
    override val position: Position get() = OverlayPositions.dungeonMap
    override val bounds: Pair<Int, Int> = 20 to 20

    inline val roomWidth get() = 50
    inline val roomHeight get() = 50
    inline val horizontalHallwayWidth get() = 4
    inline val verticalHallwayWidth get() = 4
    inline val doorWidth get() = 4
    inline val headSize get() = 8

    inline val combinedWidth get() = roomWidth + verticalHallwayWidth
    inline val combinedHeight get() = roomHeight + horizontalHallwayWidth


    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val catacomb = CatacombsManager.catacomb ?: return

        val text = mutableListOf<() -> Unit>()
        catacomb.grid.forEach { [pos, node] ->

            val isVerticalDoor = pos.isHorizontalHallway
            val isHorizontalDoor = pos.isVerticalHallway
            val (x, y) = pos

            val width = min(if (isHorizontalDoor) doorWidth else roomWidth, node.dimensions)
            val height = min(if (isVerticalDoor) doorWidth else roomWidth, node.dimensions)

            val xOffset = (x / 2) * combinedWidth + if (isHorizontalDoor) roomWidth else (roomWidth - width) / 2
            val yOffset = (y / 2) * combinedHeight + if (isVerticalDoor) roomHeight else (roomHeight - height) / 2

            val roomNode = node as? RoomNode
            graphics.fill(
                xOffset,
                yOffset,
                xOffset + width,
                yOffset + height,
                ARGB.opaque(node.getColor()),
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
                            x = xOffset - verticalHallwayWidth / 2
                            y = yOffset + height / 2 - McFont.height / 2
                        } else {
                            x = xOffset + width / 2
                            textWidth = roomWidth
                            y = yOffset - horizontalHallwayWidth / 2 - McFont.height / 2
                        }
                    }

                    ONE_BY_THREE -> {
                        val isHorizontal = roomNode.rotation == Rotation.NONE || roomNode.rotation == Rotation.CLOCKWISE_180

                        if (isHorizontal) {
                            textWidth = combinedWidth + roomWidth
                            x = xOffset + roomWidth / 2
                            y = yOffset + height / 2 - McFont.height / 2
                        } else {
                            textWidth = roomWidth
                            x = xOffset + roomWidth / 2
                            y = yOffset - horizontalHallwayWidth / 2 - McFont.height / 2
                        }
                    }

                    TWO_BY_TWO -> {
                        x = xOffset + verticalHallwayWidth / 2
                        y = yOffset + horizontalHallwayWidth / 2 - McFont.height / 2
                        textWidth = combinedWidth + roomWidth
                    }

                    STAIR -> {
                        x = xOffset + roomWidth / 2
                        y = yOffset + roomWidth / 2 - McFont.height / 2
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

        graphics.extractPlayers(catacomb)

        super.extract(graphics, mouseX, mouseY)
    }

    private fun GuiGraphicsExtractor.extractPlayers(catacomb: Catacomb) {
        val players = catacomb.playerList.filterNotNull()

        for (player in players) {
            val realPosition = player.realPosition
            if (player.realDataUpToDate && realPosition != null) {
                extractPlayerFromLevelData(player, realPosition)
                continue
            }
            val mapPosition = player.mapPosition
            if (mapPosition != null) {
                extractPlayerFromMapData(player, mapPosition)
            }
        }

    }

    private fun GuiGraphicsExtractor.extractPlayerFromLevelData(player: CatacombPlayer, pos: Vector3d) {
        val gridPos = CatacombsManager.worldPosToGridPos(pos)

        val scalarX: Float
        val scalarY: Float
        if (gridPos.isVerticalHallway) {
            scalarX = 0.5f
        } else {
            val roomCenterX = CatacombsManager.gridPosToWorldPos(gridPos.x)
            val roomStartX = roomCenterX - 15
            val pos = (pos.x - roomStartX).absoluteValue
            scalarX = (((pos.toFloat() % 31) / 31f))
        }
        if (gridPos.isHorizontalHallway) {
            scalarY = 0.5f
        } else {
            val roomCenterY = CatacombsManager.gridPosToWorldPos(gridPos.y)
            val roomStartY = roomCenterY - 15
            val pos = (pos.z - roomStartY).absoluteValue
            scalarY = (((pos.toFloat() % 31) / 31f))
        }

        extractPlayer(gridPos.x, gridPos.y, scalarX, scalarY, ((player.realRotation ?: 0f) % 360f) / 360f, player)
    }

    private fun GuiGraphicsExtractor.extractPlayerFromMapData(player: CatacombPlayer, pos: Vector2i) {
        val catacomb = player.catacomb

        val roomDoorWidth = catacomb.mapRoomAndDoorSize * 2
        val roomWidth = catacomb.mapRoomSize * 2
        val doorWith = roomDoorWidth - roomWidth

        val topLeft = (catacomb.mapTopLeft ?: return) * 2
        val catacombPosition = pos - topLeft
        val xCellOffset = (catacombPosition.x / roomDoorWidth) * 2
        val yCellOffset = (catacombPosition.y / roomDoorWidth) * 2

        val relativeX = catacombPosition.x % roomDoorWidth
        val extraCellOffsetX: Int
        val scalarX = if (relativeX > (roomWidth + 1)) {
            extraCellOffsetX = 1
            (relativeX - roomWidth - 1) / (doorWith - 1).toFloat()
        } else {
            extraCellOffsetX = 0
            relativeX / (roomWidth + 1).toFloat()
        }

        val relativeY = catacombPosition.y % roomDoorWidth
        val extraCellOffsetY: Int
        val scalarY = if (relativeY > (roomWidth + 1)) {
            extraCellOffsetY = 1
            (relativeY - roomWidth - 1) / (doorWith - 1).toFloat()
        } else {
            extraCellOffsetY = 0
            relativeY / (roomWidth + 1).toFloat()
        }

        extractPlayer(xCellOffset + extraCellOffsetX, yCellOffset + extraCellOffsetY, scalarX, scalarY, (player.mapRotation ?: 0) / 16f, player)
    }

    private fun GuiGraphicsExtractor.extractPlayer(gridX: Int, gridY: Int, scalarX: Float, scalarY: Float, rotation: Float, player: CatacombPlayer) {
        val baseOffsetX = (gridX / 2) * combinedWidth + if (gridX.isHallway) roomWidth else 0
        val baseOffsetY = (gridY / 2) * combinedHeight + if (gridY.isHallway) roomHeight else 0

        val actualWidth = if (gridX.isHallway) verticalHallwayWidth else roomWidth
        val actualHeight = if (gridY.isHallway) horizontalHallwayWidth else roomHeight
        val x = baseOffsetX + (actualWidth * scalarX).roundToInt()
        val y = baseOffsetY + (actualHeight * scalarY).roundToInt()
        translated(x, y) {
            centeredText(
                McClient.self.font,
                player.name,
                0,
                McClient.self.font.lineHeight,
                0xFFFFFFFF.toInt(),
            )
            rotate(180f + rotation * 360f)
            fill(
                -(headSize * 1.2).toInt(),
                -(headSize * 1.2).toInt(),
                (headSize * 1.2).toInt(),
                (headSize * 1.2).toInt(),
                player.dungeonClass?.getColor()?.opaque() ?: -1,
            )
            PlayerFaceExtractor.extractRenderState(this, player.skin ?: return, -headSize, -headSize, headSize * 2)
        }
    }
}
