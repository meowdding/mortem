package me.owdding.mortem.features.commands

import kotlin.collections.joinToString
import me.owdding.ktmodules.Module
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.catacombs.CatacombsManager.backingRooms
import me.owdding.mortem.core.catacombs.SecretDetails
import me.owdding.mortem.core.catacombs.StoredCatacombRoom
import me.owdding.mortem.core.catacombs.roommatching.CatacombWorldMatcher
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.extensions.mutableCopy
import me.owdding.mortem.utils.extensions.sendWithPrefix
import me.owdding.mortem.utils.extensions.toBlockPos
import me.owdding.mortem.utils.extensions.toVector3i
import me.owdding.mortem.utils.extensions.transpose
import me.owdding.mortem.utils.import_data.RoomData
import me.owdding.mortem.utils.import_data.Shape
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.Heightmap
import org.joml.Vector2i
import org.joml.Vector3i
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent.Companion.argument
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.command.EnumArgument
import tech.thatgravyboat.skyblockapi.utils.extentions.filterKeysNotNull
import tech.thatgravyboat.skyblockapi.utils.extentions.plus
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick

@Module
object PraseMapWorld {
    val roomData: List<RoomData> = Utils.loadRepoData<RoomData, List<RoomData>>("import_data") { it.listOf() }

    init {
        Mortem.warn(roomData.filter { it.shape == Shape.`1x3` || it.shape == Shape.`1x4` }.flatMap { it.id }.joinToString(" | "))
    }

    var iterator: Iterator<Pair<Pair<String, Int>, RoomData>>? = null
    var total: Int = 0

    @Subscription
    fun commands(event: RegisterCommandsEvent) {
        event.registerWithCallback("mortem dev parse_world") {
            iterator = roomData.flatMap { it.id.mapIndexed { index, string -> (string to index) to it } }.apply {
                total = size
            }.iterator()
        }
        event.registerWithCallback("mortem dev stupid_stuff") {
            val pos = this.source.position.toBlockPos().atY(0)
            Text.of("Click to copy room blocks!") {
                val clipboard = thingy().joinToString("\n") { it.toVector3i().toString() + " - " + CatacombWorldMatcher.blockAt(pos.plus(it)) }
                onClick { McClient.clipboard = clipboard }
            }.sendWithPrefix()
        }
        event.register("mortem dev horrible_stuff") {
            thenCallback("rotation", EnumArgument.create(Rotation::class.java)) {
                val pos = this.source.position.toBlockPos().atY(0)
                Text.of("Click to copy room blocks!") {
                    val clipboard = thingy().joinToString("\n") {
                        val vec3i = it.toVector3i()
                        val rotation = argument<Rotation>("rotation")
                        val room = when (rotation) {
                            Rotation.COUNTERCLOCKWISE_90 -> Vector3i(vec3i.z(), vec3i.y(), -vec3i.x())
                            Rotation.CLOCKWISE_180 -> Vector3i(-vec3i.x(), vec3i.y(), -vec3i.z())
                            Rotation.CLOCKWISE_90 -> Vector3i(-vec3i.z(), vec3i.y(), vec3i.x())
                            else -> vec3i.mutableCopy()
                        }
                        it.toVector3i().toString() + " - " + CatacombWorldMatcher.blockAt(room.add(pos.x, 0, pos.z).toBlockPos())
                    }
                    onClick { McClient.clipboard = clipboard }
                }.sendWithPrefix()
            }
        }
    }

    fun thingy(): Iterable<BlockPos> = BlockPos.betweenClosed(BlockPos(-15, 255, -15), BlockPos(15, 0, 15))

    var completedRooms = 0

    var current: Pair<Pair<String, Int>, RoomData>? = null

    var loadedFor = 0

    @Subscription
    fun onTick(event: TickEvent) {
        val iterator = iterator ?: return
        if (current == null && !iterator.hasNext()) {
            Text.of("Finished room parsing!").sendWithPrefix()
            this.iterator = null
            return
        }
        if (current == null) {
            loadedFor = 0
        }

        val current = current ?: iterator.next()
        this.current = current
        val (data, room) = current
        val (position, index) = data
        val (x, z) = position.split(",").map { it.toInt() }
        val id = if (room.id.size != 1) {
            room.name.lowercase() + "_" + index
        } else {
            room.name.lowercase()
        }.replace(" ", "_")

        val roomCorner = Vector2i(x, z)
        val centerOffset = when (room.shape) {
            Shape.`1x1` -> Vector2i(15, 15)
            Shape.`1x2` -> Vector2i(31, 15)
            Shape.`1x3` -> Vector2i(47, 15)
            Shape.`1x4` -> Vector2i(63, 15)
            Shape.L -> Vector2i(15, 47)
            Shape.`2x2` -> Vector2i(31, 31)
        }
        val center = roomCorner.add(centerOffset)
        val level = McClient.self.singleplayerServer!!.allLevels.first()
        level.players().forEach { it.teleportTo(center.x + 0.5, level.getHeight(Heightmap.Types.WORLD_SURFACE, center.x, center.y).toDouble(), center.y + 0.5) }
        if (McLevel.level.getChunk(SectionPos.blockToSectionCoord(center.x), SectionPos.blockToSectionCoord(center.y)).isEmpty) {
            Text.of("Waiting for chunks to load!").sendWithPrefix("mortem-action")
            Text.of("Progress ${completedRooms}/$total!").sendWithPrefix("mortem-status-progress")
            return
        }
        if (loadedFor <= 40) {
            loadedFor++
            return
        }
        this.current = null
        val hashes = CatacombWorldMatcher.createDirectionalHashes(BlockPos(center.x, 255, center.y))
        val centerHash = hashes[null]!!

        if (backingRooms[centerHash] != null) {
            Text.of("Room $id with hash already exists!", CatppuccinColors.Mocha.red).sendWithPrefix()
            Text.of("Progress ${++completedRooms}/$total!").sendWithPrefix("mortem-status-progress")
            return
        }

        val storedRoom = StoredCatacombRoom(
            room.name,
            id,

            centerHash,
            hashes.filterKeysNotNull().transpose(),

            room.journals,
            room.crypts,

            room.spiders,
            false,
            room.fairySoul,

            room.secretDetails ?: SecretDetails.EMPTY,
            room.type.type,
            room.shape.catacombShape,
        )
        storedRoom.markChange()
        backingRooms[centerHash] = storedRoom
        Text.of("Created new room ${room.name} ($id)!", CatppuccinColors.Mocha.green).sendWithPrefix("mortem-action")
        Text.of("Progress ${++completedRooms}/$total!").sendWithPrefix("mortem-status-progress")
    }

}
