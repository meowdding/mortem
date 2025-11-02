package me.owdding.mortem.core.catacombs.roommatching

import me.owdding.ktmodules.Module
import me.owdding.lib.overlays.ConfigPosition
import me.owdding.lib.overlays.Position
import me.owdding.mortem.core.catacombs.*
import me.owdding.mortem.core.catacombs.nodes.CatacombNodeType
import me.owdding.mortem.utils.MortemOverlay
import me.owdding.mortem.utils.Overlay
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import org.joml.*
import tech.thatgravyboat.skyblockapi.utils.text.Text
import kotlin.math.min

@Module
@Overlay
object CatacombMapMatcher : MortemOverlay {

    private val vectorZeroOne = Vector2i(0, 1)
    private val vectorOneZero = Vector2i(1, 0)
    private val vectorOneOne = Vector2i(1, 1)
    private val vectorZeroTwo = vectorZeroOne * 2
    private val vectorTwoZero = vectorOneZero * 2
    private val vectorTwoTwo = vectorOneOne * 2

    var data: ByteArray? = null
    var mapOverlay: ByteArray = ByteArray(128 * 128)

    operator fun ByteArray.get(x: Int, y: Int) = this.getOrNull(y * 128 + x)
    operator fun ByteArray.get(vector2i: Vector2i) = this.getOrNull(vector2i.y * 128 + vector2i.x)
    operator fun ByteArray.set(x: Int, y: Int, value: Byte) = this.set(y * 128 + x, value)
    operator fun ByteArray.set(vector2i: Vector2i, value: Byte) = this.set(vector2i.y * 128 + vector2i.x, value)

    fun updateInstance(instance: Catacomb, mapColors: ByteArray) {
        if (mapColors[0, 0] != CatacombMapColor.NONE.packedId) return

        data = mapColors
        mapOverlay = ByteArray(mapColors.size)

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
        mapOverlay[topLeft.y * 128 + topLeft.x] = CatacombMapColor.MINIBOSS.packedId

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

                val room = instance.getOrCreateNode(roomGridPosition, CatacombNodeType.ROOM)
                room.roomType = roomType

                mapOverlay[mapPosition] = CatacombMapColor.MINIBOSS.packedId
                mapOverlay[mapPosition + halfRoom] = CatacombMapColor.MINIBOSS.packedId
                val rightDoorColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + rightDoor])
                val rightColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + Vector2i(roomSize + 1, 0)])
                val rightDoor = CatacombDoorType.getByColor(rightDoorColor)
                if (rightDoor != null && rightColor != rightDoorColor) {
                    val node = instance.getOrCreateNode(roomGridPosition + vectorOneZero, CatacombNodeType.DOOR)
                    node.mutateType(rightDoor)
                }

                val downDoorColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + downDoor])
                val downColor = CatacombMapColor.getByPackedId(mapColors[mapPosition + Vector2i(0, roomSize + 1)])
                val downDoor = CatacombDoorType.getByColor(downDoorColor)
                if (downDoor != null && downColor != downDoorColor) {
                    val node = instance.getOrCreateNode(roomGridPosition + vectorZeroOne, CatacombNodeType.DOOR)
                    node.mutateType(downDoor)
                }
            }
        }
    }

    fun Catacomb.mergeNodes(position: Vector2i, oneOffset: Vector2i, twoOffset: Vector2i) {
        val room = getOrCreateNode(position - twoOffset, CatacombNodeType.ROOM)
        grid[position] = room
        grid[position - oneOffset] = room
    }

    override val name: Component = Text.of("Debug")
    override val position: Position = ConfigPosition(0, 0)
    override val bounds: Pair<Int, Int> = 20 to 20

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        /*val data = data ?: return
        val width = sqrt(data.size.toFloat()).toInt()

        for ((index, data) in data.withIndex()) {
            val x = index % width
            val y = index / width

            val color = MapColor.getColorFromPackedId(data.toInt())
            if (ARGB.alpha(color) > 0.1 && ((x % 2) == 0) xor ((y % 2) == 0)) {
                graphics.fill(x, y, x + 1, y + 1, color)
            }

            val overlayData = mapOverlay.getOrNull(index) ?: continue
            val overlay = MapColor.getColorFromPackedId(overlayData.toInt())
            if (ARGB.alpha(overlay) > 0.1) {
                graphics.fill(x, y, x + 1, y + 1, overlay)
            }
        }*/
        val catacomb = CatacombsManager.catacomb ?: return


        catacomb.grid.forEach { (pos, node) ->
            val isVerticalDoor = (pos.y % 2 == 1)
            val isHorizontalDoor = (pos.x % 2 == 1)
            val isDoor = isVerticalDoor xor isHorizontalDoor
            val isMiddle = isHorizontalDoor && isVerticalDoor
            val isRoom = !isDoor && !isMiddle
            val (x, y) = pos

            val width = min(if (isHorizontalDoor) 4 else 50, node.dimensions)
            val height = min(if (isVerticalDoor) 4 else 50, node.dimensions)

            val xOffset = (x / 2) * 54 + if (isHorizontalDoor) 50 else (50 - width) / 2
            val yOffset = (y / 2) * 54 + if (isVerticalDoor) 50 else (50 - height) / 2

            graphics.fill(xOffset, yOffset, xOffset + width, yOffset + height, if (isRoom) ARGB.opaque(node.getColor()) else ARGB.color(125, node.getColor()))
        }

        super.render(graphics, mouseX, mouseY)
    }

}
