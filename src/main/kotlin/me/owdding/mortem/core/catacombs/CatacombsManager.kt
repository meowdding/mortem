package me.owdding.mortem.core.catacombs

import me.owdding.ktmodules.Module
import me.owdding.lib.extensions.floor
import me.owdding.lib.extensions.shorten
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.roommatching.CatacombMapMatcher
import me.owdding.mortem.core.catacombs.roommatching.CatacombWorldMatcher
import me.owdding.mortem.core.catacombs.types.StoredCatacombRoom
import me.owdding.mortem.core.event.CatacombJoinEvent
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.generated.CodecUtils
import me.owdding.mortem.generated.MortemCodecs
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.Utils.post
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.extensions.sendWithPrefix
import me.owdding.mortem.utils.extensions.toVector3dc
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.joml.Vector2i
import org.joml.Vector3dc
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.TimePassed
import tech.thatgravyboat.skyblockapi.api.events.dungeon.DungeonEnterEvent
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.level.PacketReceivedEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.utils.json.Json.toJsonOrThrow
import tech.thatgravyboat.skyblockapi.utils.json.Json.toPrettyString
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextBuilder.append
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.math.abs
import kotlin.math.floor
import me.owdding.mortem.core.event.catacomb.CatacombNodeChangeEvent
import me.owdding.mortem.core.event.catacomb.CatacombRoomChangeEvent
import me.owdding.mortem.utils.extensions.isCrossHallway
import me.owdding.mortem.utils.extensions.isHorizontalHallway
import org.joml.Vector2ic
import org.joml.Vector3d

@Module
object CatacombsManager {

    val backingRooms: MutableMap<Int, StoredCatacombRoom> = mutableMapOf()

    init {
        val list: List<StoredCatacombRoom> = Utils.loadRepoData("rooms", CodecUtils::list)
        list.forEach { room ->
            backingRooms.putAll(room.hashes.associateWith { room })
        }
    }

    var catacomb: Catacomb? = null
        private set

    @Subscription
    private fun DungeonEnterEvent.onDungeonEnter() {
        if (catacomb != null) reset()
        val catacomb = Catacomb(floor)
        this@CatacombsManager.catacomb = catacomb
        val level = McLevel.self ?: return reset()

        val maxChunkX = -12 + (catacomb.size.boundaryX * 2)
        val maxChunkZ = -12 + (catacomb.size.boundaryY * 2)
        McClient.runNextTick {
            for (x in -12..maxChunkX) {
                for (y in -12..maxChunkZ) {
                    val chunk = level.getChunk(x, y, ChunkStatus.FULL, false) ?: continue
                    CatacombWorldMatcher.scanChunk(chunk)
                }
            }
        }

        CatacombJoinEvent(catacomb).post()
    }

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    private fun PacketReceivedEvent.onPacket() {
        val catacomb = catacomb ?: return
        val map = this.packet as? ClientboundMapItemDataPacket ?: return

        map.colorPatch.ifPresent {
            CatacombMapMatcher.updateInstance(catacomb, it.mapColors)
        }
        map.decorations().ifPresent {
            CatacombMapMatcher.updateDecorations(catacomb, it)
        }
    }

    @Subscription(ServerChangeEvent::class, ServerDisconnectEvent::class)
    fun reset() {
        val instance = catacomb ?: return
        CatacombLeaveEvent(instance).post()
        catacomb = null
    }


    @Subscription
    fun command(event: RegisterCommandsEvent) {
        event.registerWithCallback("mortem dev chunk_hash") {
            val chunkPos = McPlayer.self!!.chunkPosition()
            val chunk = McLevel.self!!.getChunk(chunkPos.x, chunkPos.z)
            val hash = CatacombWorldMatcher.hashChunk(chunk)
            Text.of("Hash for current position is ") {
                append(hash) {
                    color = CatppuccinColors.Mocha.pink
                }
                color = CatppuccinColors.Mocha.lavender
            }.sendWithPrefix()
        }
        fun format(coordinate: Vector3dc): Component = Text.of {
            append(coordinate.x().shorten(2)) { color = CatppuccinColors.Mocha.red }
            append(" ")
            append(coordinate.y().shorten(2)) { color = CatppuccinColors.Mocha.green }
            append(" ")
            append(coordinate.z().shorten(2)) { color = CatppuccinColors.Mocha.blue }
        }
        event.registerWithCallback("mortem dev room_pos") {
            val catacomb = catacomb ?: return@registerWithCallback
            val playerNode = catacomb.grid[worldPosToGridPos(McPlayer.self!!.blockPosition())]
            if (playerNode !is RoomNode) {
                Text.of("Not in any room!", CatppuccinColors.Mocha.red).sendWithPrefix()
                return@registerWithCallback
            }

            val roomPos = playerNode.worldToRoom(McPlayer.position!!.toVector3dc())
            Text.of("Current room pos: ") {
                color = CatppuccinColors.Mocha.text
                append(format(roomPos))
            }.sendWithPrefix()
        }
        event.registerWithCallback("mortem dev room_pos_test") {
            val catacomb = catacomb ?: return@registerWithCallback
            val playerNode = catacomb.grid[worldPosToGridPos(McPlayer.self!!.blockPosition())]
            val pos = McPlayer.position!!.toVector3dc()
            if (playerNode !is RoomNode) {
                Text.of("Not in any room!", CatppuccinColors.Mocha.red).sendWithPrefix()
                return@registerWithCallback
            }

            val roomPos = playerNode.worldToRoom(pos)
            Text.of("World: ") {
                color = CatppuccinColors.Mocha.text
                append(format(pos))
            }.sendWithPrefix()
            Text.of("World -> Room: ") {
                color = CatppuccinColors.Mocha.text
                append(format(roomPos))
            }.sendWithPrefix()
            Text.of("World -> Room -> World: ") {
                color = CatppuccinColors.Mocha.text
                append(format(playerNode.roomToWorld(roomPos)))
            }.sendWithPrefix()
        }
    }

    private val defaultPath: Path = McClient.config.resolve("mortem/data")

    @Subscription(TickEvent::class)
    fun tick() = catacomb?.tick()

    @Subscription
    fun onNodeSwitch(event: CatacombNodeChangeEvent<*, *>) {
        Text.of("Room switch, ${event.previous} -> ${event.current}").sendWithPrefix()
    }
    @Subscription
    fun onRoomSwitch(event: CatacombRoomChangeEvent) {
        Text.of("Room switch, ${event.previous?.backingData?.name} -> ${event.current.backingData?.name}").sendWithPrefix()
    }

    @Subscription(TickEvent::class)
    @TimePassed("5s")
    fun saveAll() {
        val rooms = defaultPath.resolve("rooms").createDirectories()
        this.backingRooms.values.forEach {
            if (!it.shouldSerialize) return@forEach
            it.shouldSerialize = false
            rooms.resolve("${it.name}.json").writeText(it.toJsonOrThrow(MortemCodecs.getCodec()).toPrettyString())
        }
    }

    fun worldPosToGridPos(pos: BlockPos): Vector2i = worldPosToGridPos(pos.x, pos.z)
    fun worldPosToGridPos(pos: Vector2ic): Vector2i = worldPosToGridPos(pos.x(), pos.y())
    fun worldPosToGridPos(pos: Vector3d): Vector2i = worldPosToGridPos(pos.x().floor(), pos.z().floor())


    fun worldPosToGridPos(scalar: Int): Int {
        val chunk = floor(scalar / 16f).toInt()
        val chunkRelative = scalar and 15

        val isHallway = abs(chunk) % 2 == 1
        val baseGridPos = chunk + 12

        return when {
            isHallway && chunkRelative > 7 -> baseGridPos + 1
            isHallway && chunkRelative < 7 -> baseGridPos - 1
            else -> baseGridPos
        }
    }
    fun worldPosToGridPos(x: Int, y: Int): Vector2i {
        return Vector2i(worldPosToGridPos(x), worldPosToGridPos(y))
    }

    fun gridPosToWorldPos(scalar: Int): Int = (scalar - 12) * 16 + 7
    fun gridPosToWorldPos(pos: Vector2ic): BlockPos {
        return BlockPos(gridPosToWorldPos(pos.x()), 0, gridPosToWorldPos(pos.y()))
    }

}
