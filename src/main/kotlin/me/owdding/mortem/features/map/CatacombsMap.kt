package me.owdding.mortem.features.map

import me.owdding.ktmodules.Module
import me.owdding.lib.overlays.ConfigPosition
import me.owdding.lib.overlays.Position
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.CatacombNodeType
import me.owdding.mortem.core.catacombs.nodes.CatacombsNode
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.extensions.mutableCopy
import me.owdding.mortem.utils.extensions.toVector3d
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.entity.player.Player
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.component1
import org.joml.component2
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.platform.rotate
import tech.thatgravyboat.skyblockapi.platform.skin
import tech.thatgravyboat.skyblockapi.utils.extentions.isRealPlayer
import tech.thatgravyboat.skyblockapi.utils.extentions.scaled
import tech.thatgravyboat.skyblockapi.utils.extentions.translated
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped


private const val ROOM_WIDTH = 35
private const val SPACING = 5

private const val DOOR_SPACING = SPACING
private const val DOOR_WIDTH = ROOM_WIDTH - (DOOR_SPACING * 2)


private const val AVERAGE = (ROOM_WIDTH + SPACING) / 2
private const val DIFFERENCE = ROOM_WIDTH - AVERAGE
private const val SCALE_FACTOR = AVERAGE / 16.0

private const val BACKGROUND_COLOR = 0x80FF5555.toInt()
private const val SHOW_NAMES = false
private const val HEAD_SIZE = 6

@Module
@Overlay
object CatacombsMap : MortemOverlay {

    override val name: Component = Text.of("Dungeon Map")
    override val position: Position = ConfigPosition(0, 0)
    override val bounds: Pair<Int, Int> = 20 to 20

    //region DrawData
    data class DrawData(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
    ) {
        fun center(): Vector2i = Vector2i(((maxX - minX) / 2) + minX, ((maxY - minY) / 2) + minY)
    }
    //endregion

    private fun RoomNode.getCenterStringDrawPos(): Vector2i {
        val minX = positions.minOf { it.x }
        val minY = positions.minOf { it.y }
        val min = Vector2i(minX, minY).mul(2)
        val offset = when (shape) {
            STAIR -> {
                val temp = getMiddleChunkOffset() ?: Utils.vectorZeroZero.mutableCopy()
                if (temp.x == 0) temp.add(1, 0)
                else temp.sub(1, 0)
            }

            else -> getMiddleChunkOffset() ?: Utils.vectorZeroZero
        }

        val drawPos = min.add(offset).drawPos(this)
        val center = drawPos.center()
        return center
    }

    private fun RoomNode.maxWidth(): Int {
        val positions = positions.groupBy { it.y }.values.maxOf { it.size }
        return (positions * ROOM_WIDTH) +
            ((positions - 1) / SPACING)
    }

    private fun Vector2i.drawPos(node: CatacombsNode<*>): DrawData {
        val isDoor = node.type == CatacombNodeType.Door

        val (x, y) = this

        val isHorizontal = (x % 2) == 1
        val isVertical = (y % 2) == 1

        fun roomsBefore(int: Int): Int = (int + 1) / 2
        fun doorsBefore(int: Int) = int - roomsBefore(int)

        val minX = SPACING +
            (roomsBefore(x) * ROOM_WIDTH) +
            (doorsBefore(x) * SPACING) +
            (if (isDoor && isVertical) DOOR_SPACING else 0)

        val minY = SPACING +
            (roomsBefore(y) * ROOM_WIDTH) +
            (doorsBefore(y) * SPACING) +
            (if (isDoor && isHorizontal) DOOR_SPACING else 0)


        val maxX = minX + when {
            isHorizontal -> SPACING
            isDoor -> DOOR_WIDTH
            else -> ROOM_WIDTH
        }
        val maxY = minY + when {
            isVertical -> SPACING
            isDoor -> DOOR_WIDTH
            else -> ROOM_WIDTH
        }

        return DrawData(minX, minY, maxX, maxY)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val catacomb = CatacombsManager.catacomb ?: return

        val mapSize = catacomb.size

        val (xSize, ySize) = mapSize.xRooms to mapSize.yRooms

        graphics.fill(
            0,
            0,
            AVERAGE * xSize * 2 + SPACING,
            AVERAGE * ySize * 2 + SPACING,
            BACKGROUND_COLOR,
        )


        for ((pos, node) in catacomb.grid) {
            val drawData = pos.drawPos(node)

            val (minX, minY, maxX, maxY) = drawData
            graphics.fill(
                minX,
                minY,
                maxX,
                maxY,
                ARGB.color(180, ARGB.opaque(node.getColor())).let {
                    if (node is RoomNode && node.backingData == null) ARGB.scaleRGB(it, 0.8f) else it
                },
            )
        }

        val roomNodes = catacomb.grid.values.filterIsInstance<RoomNode>().distinct()

        for (room in roomNodes) {
            val (x, y) = room.getCenterStringDrawPos()
            val backingData = room.backingData
            if (backingData == null) {
                graphics.drawCenteredString(
                    McFont.self,
                    "Unknown",
                    x,
                    y - (McFont.height / 2),
                    ARGB.opaque(TextColor.RED),
                )
                continue
            }

            val secretString = if (backingData.secrets > 0) ""
            else " (${room.secrets}/${backingData.secrets})"

            val split = (backingData.name + secretString).split(" ")

            var yOffset = -(split.size * McFont.height / 2)
            val maxWidth = room.maxWidth()

            split.forEach { string ->
                val width = McFont.width(string)
                fun drawString() = graphics.drawString(
                    McFont.self,
                    string,
                    x - (width / 2),
                    y + yOffset,
                    -1, // TODO: proper color depending on completion
                    true
                )
                if (width > maxWidth) {
                    graphics.scaled(maxWidth / width.toFloat()) {
                        drawString()
                    }
                } else drawString()

                yOffset += McFont.height
            }
        }

        fun renderRealPlayer(player: Player) {
            if (player !is AbstractClientPlayer) return
            if (!player.isRealPlayer()) return
            val playerPos = player.position().toVector3d().toMapPos()

            graphics.translated(playerPos.x, playerPos.y) {
                if (SHOW_NAMES) {
                    graphics.drawCenteredString(
                        McFont.self,
                        player.name.stripped,
                        0,
                        McFont.height,
                        -1,
                    )
                }
                graphics.rotate(180f + player.rotationVector.y)
                graphics.fill(
                    -(HEAD_SIZE * 1.2).toInt(),
                    -(HEAD_SIZE * 1.2).toInt(),
                    (HEAD_SIZE * 1.2).toInt(),
                    (HEAD_SIZE * 1.2).toInt(),
                    ARGB.opaque(TextColor.GREEN),
                )
                PlayerFaceRenderer.draw(
                    graphics,
                    player.skin(),
                    -HEAD_SIZE,
                    -HEAD_SIZE,
                    2 * HEAD_SIZE,
                )
            }

        }
        McPlayer.self?.let(::renderRealPlayer)

        super.render(graphics, mouseX, mouseY)
    }

    private fun Vector3d.toMapPos(): Vector2i {
        return this.add(208.0, 0.0, 208.0)
            .mul(SCALE_FACTOR, 1.0, SCALE_FACTOR)
            .sub(DIFFERENCE / 2.0, 0.0, DIFFERENCE / 2.0)
            .let { Vector2i(it.x.toInt(), it.z.toInt()) }
    }

}
