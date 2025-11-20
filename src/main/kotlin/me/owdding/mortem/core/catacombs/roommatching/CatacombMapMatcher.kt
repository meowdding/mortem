package me.owdding.mortem.core.catacombs.roommatching

import kotlin.math.min
import me.owdding.ktmodules.Module
import me.owdding.lib.overlays.ConfigPosition
import me.owdding.lib.overlays.Position
import me.owdding.mortem.core.catacombs.*
import me.owdding.mortem.core.catacombs.nodes.CatacombNodeType
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.utils.MortemDevUtils
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import me.owdding.mortem.utils.Utils.vectorOneOne
import me.owdding.mortem.utils.Utils.vectorOneZero
import me.owdding.mortem.utils.Utils.vectorTwoTwo
import me.owdding.mortem.utils.Utils.vectorTwoZero
import me.owdding.mortem.utils.Utils.vectorZeroOne
import me.owdding.mortem.utils.Utils.vectorZeroTwo
import me.owdding.mortem.utils.extensions.copy
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.Rotation
import org.joml.*
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.utils.text.Text

@Module
@Overlay
object CatacombMapMatcher : MortemOverlay {

    operator fun ByteArray.get(x: Int, y: Int) = this.getOrNull(y * 128 + x)
    operator fun ByteArray.get(vector2i: Vector2i) = this.getOrNull(vector2i.y * 128 + vector2i.x)
    operator fun ByteArray.set(x: Int, y: Int, value: Byte) = this.set(y * 128 + x, value)
    operator fun ByteArray.set(vector2i: Vector2i, value: Byte) = this.set(vector2i.y * 128 + vector2i.x, value)

    fun updateInstance(instance: Catacomb, mapColors: ByteArray) {
        if (mapColors[0, 0] != CatacombMapColor.NONE.packedId) return

        if (instance.mapTopLeft == null) {
            var smallestX = 127
            var startY = 127
            for (y in 0..112) {
                for (x in 0..smallestX) {
                    if (mapColors[x, y] == CatacombMapColor.COMPLETE.packedId
                        && mapColors[x + 15, y] == CatacombMapColor.COMPLETE.packedId && smallestX > x
                    ) {
                        smallestX = x
                    }
                }


                startY = y
                if (smallestX < 127) {
                    var width = 0
                    while (CatacombMapColor.COMPLETE.packedId == mapColors[smallestX + width, y]) width++
                    instance.mapRoomSize = width
                    break
                }
            }

            if (smallestX % 20 == 5 && instance.size == CatacombSize.GIGANTIC) instance.size = CatacombSize.COLOSSAL
            if (smallestX % 20 == 11 && instance.size == CatacombSize.LARGE) instance.size = CatacombSize.NORMAL


            val topX = smallestX % instance.mapRoomAndDoorSize + if (instance.size <= CatacombSize.SMALL) instance.mapRoomAndDoorSize else 0
            val topY = startY % instance.mapRoomAndDoorSize + if (instance.size == CatacombSize.TINY) instance.mapRoomAndDoorSize else 0

            instance.mapTopLeft = Vector2i(topX, topY)
        }
        val topLeft = instance.mapTopLeft ?: return
        val roomAndDoorSize = instance.mapRoomAndDoorSize
        val roomSize = instance.mapRoomSize
        val halfRoomSize = roomSize / 2
        val halfRoom = Vector2i(halfRoomSize)
        val rightDoor = Vector2i(roomSize + 1, halfRoomSize)
        val downDoor = Vector2i(halfRoomSize, roomSize + 1)

        val rooms = mutableSetOf<RoomNode>()
        for (y in 0 until instance.size.boundaryY) {
            for (x in 0 until instance.size.boundaryX) {
                val roomCoordinate = Vector2i(x, y)
                val roomGridPosition = roomCoordinate * 2
                val mapPosition = topLeft + roomCoordinate * roomAndDoorSize
                val color = CatacombMapColor.getByPackedId(mapColors[mapPosition])

                val roomType = CatacombRoomType.getByColor(color) ?: continue

                if (roomType == CatacombRoomType.NORMAL) {
                    if (CatacombMapColor.getByPackedId(mapColors[mapPosition - vectorOneZero]) == CatacombMapColor.NORMAL) instance.mergeNodes(
                        roomGridPosition,
                        vectorOneZero,
                        vectorTwoZero,
                    )
                    if (CatacombMapColor.getByPackedId(mapColors[mapPosition - vectorZeroOne]) == CatacombMapColor.NORMAL) instance.mergeNodes(
                        roomGridPosition,
                        vectorZeroOne,
                        vectorZeroTwo,
                    )
                    if (CatacombMapColor.getByPackedId(mapColors[mapPosition - vectorOneOne]) == CatacombMapColor.NORMAL) instance.mergeNodes(
                        roomGridPosition,
                        vectorOneOne,
                        vectorTwoTwo,
                    )
                }

                val room = instance.getOrCreateNode(roomGridPosition, CatacombNodeType.Room)
                room.mutateType(roomType)
                room.addPosition(roomCoordinate)
                rooms.add(room)

                val rightDoorColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + rightDoor])
                val rightColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + Vector2i(roomSize + 1, 0)])
                val rightDoor = CatacombDoorType.getByColor(rightDoorColor)
                if (rightDoor != null && rightColor != rightDoorColor) {
                    val node = instance.getOrCreateNode(roomGridPosition + vectorOneZero, CatacombNodeType.Door)
                    node.mutateType(rightDoor)
                }

                val downDoorColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + downDoor])
                val downColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + Vector2i(0, roomSize + 1)])
                val downDoor = CatacombDoorType.getByColor(downDoorColor)
                if (downDoor != null && downColor != downDoorColor) {
                    val node = instance.getOrCreateNode(roomGridPosition + vectorZeroOne, CatacombNodeType.Door)
                    node.mutateType(downDoor)
                }
            }
        }

        CatacombWorldMatcher.matchData(rooms)
        if (MortemDevUtils.getDebugBoolean("hypixel_rotation")) {
            CatacombsManager.scanAllChunks()
        }
    }

    fun Catacomb.mergeNodes(position: Vector2i, oneOffset: Vector2i, twoOffset: Vector2i) {
        val room = getOrCreateNode(position - twoOffset, CatacombNodeType.Room)
        grid[position] = room
        grid[position - oneOffset] = room
        room.addPosition(position.copy() / 2)
    }

    override val name: Component = Text.of("Debug")
    override val position: Position = ConfigPosition(0, 0)
    override val bounds: Pair<Int, Int> = 20 to 20

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val catacomb = CatacombsManager.catacomb ?: return

        catacomb.grid.forEach { (pos, node) ->
            val isVerticalDoor = (pos.y() % 2 == 1)
            val isHorizontalDoor = (pos.x() % 2 == 1)
            val isDoor = isVerticalDoor xor isHorizontalDoor
            val isMiddle = isHorizontalDoor && isVerticalDoor
            val isRoom = !isDoor && !isMiddle
            val (x, y) = pos

            val width = min(if (isHorizontalDoor) 4 else 50, node.dimensions)
            val height = min(if (isVerticalDoor) 4 else 50, node.dimensions)

            val xOffset = (x / 2) * 54 + if (isHorizontalDoor) 50 else (50 - width) / 2
            val yOffset = (y / 2) * 54 + if (isVerticalDoor) 50 else (50 - height) / 2

            val roomNode = node as? RoomNode
            graphics.fill(
                xOffset,
                yOffset,
                xOffset + width,
                yOffset + height,
                if (roomNode?.backingData != null) ARGB.opaque(node.getColor())
                else if (isRoom) ARGB.color(255, node.getColor())
                else ARGB.greyscale(ARGB.color(125, node.getColor())),
            )

            val data = roomNode?.backingData
            if (data != null) {
                graphics.drawString(McFont.self, data.name, xOffset, yOffset, -1)
            }
            if (isRoom)
                graphics.drawString(
                    McFont.self,
                    when (roomNode?.rotation) {
                        Rotation.NONE -> "0°"
                        Rotation.CLOCKWISE_90 -> "90°"
                        Rotation.CLOCKWISE_180 -> "180°"
                        Rotation.COUNTERCLOCKWISE_90 -> "270°"
                        else -> "null"
                    },
                    xOffset, yOffset, -1,
                )
        }

        super.render(graphics, mouseX, mouseY)
    }

}
