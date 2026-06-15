package me.owdding.mortem.core.catacombs.roommatching

import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.*
import me.owdding.mortem.core.catacombs.nodes.CatacombNodeType
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.types.CatacombDoorType
import me.owdding.mortem.core.catacombs.types.CatacombMapColor
import me.owdding.mortem.core.catacombs.types.CatacombRoomCheckmark
import me.owdding.mortem.core.catacombs.types.CatacombRoomType
import me.owdding.mortem.core.catacombs.types.CatacombSize
import me.owdding.mortem.utils.Utils.vectorOneOne
import me.owdding.mortem.utils.Utils.vectorOneZero
import me.owdding.mortem.utils.Utils.vectorTwoTwo
import me.owdding.mortem.utils.Utils.vectorTwoZero
import me.owdding.mortem.utils.Utils.vectorZeroOne
import me.owdding.mortem.utils.Utils.vectorZeroTwo
import me.owdding.mortem.utils.extensions.copy
import net.minecraft.world.level.saveddata.maps.MapDecoration
import org.joml.*
import java.util.Optional

@Module
object CatacombMapMatcher {

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

                val checkmark = CatacombRoomCheckmark.getByColor( CatacombMapColor.getByPackedId(mapColors[mapPosition + halfRoom]))
                room.mutateCheckmark(checkmark)

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
    }

    fun Catacomb.mergeNodes(position: Vector2ic, oneOffset: Vector2ic, twoOffset: Vector2ic) {
        val room = getOrCreateNode(position - twoOffset, CatacombNodeType.Room)
        grid[position] = room
        grid[position - oneOffset] = room
        room.addPosition(position.copy() / 2)
    }

    fun updateDecorations(catacomb: Catacomb, decorations: MutableList<MapDecoration>) {
        val players = catacomb.playerList
        val alivePlayers = players.sumOf { 1.takeUnless { _ -> it == null } ?: 0 }
        if (alivePlayers != decorations.size) return
        players.forEachIndexed { index, player ->
            player?.updateDecoration(decorations.getOrNull(index) ?: return@forEachIndexed)
        }
    }
}
