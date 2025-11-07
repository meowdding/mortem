package me.owdding.mortem.core.catacombs

import me.owdding.ktmodules.Module
import me.owdding.mortem.core.catacombs.roommatching.CatacombMapMatcher
import me.owdding.mortem.core.catacombs.roommatching.CatacombWorldMatcher
import me.owdding.mortem.core.event.CatacombJoinEvent
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.generated.CodecUtils
import me.owdding.mortem.generated.MortemCodecs
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.Utils.post
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.extensions.sendWithPrefix
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.joml.Vector2i
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.TimePassed
import tech.thatgravyboat.skyblockapi.api.events.dungeon.DungeonEnterEvent
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.level.PacketReceivedEvent
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

@Module
object CatacombsManager {

    val backingRooms: MutableMap<String, StoredCatacombRoom> = mutableMapOf()

    init {
        val list: List<StoredCatacombRoom> = Utils.loadRepoData("rooms", CodecUtils::list)
        backingRooms.putAll(list.associateBy { it.centerHash })
    }

    var catacomb: Catacomb? = null
        private set

    @Subscription
    private fun DungeonEnterEvent.onDungeonEnter() {
        if (catacomb != null) reset()
        val catacomb = Catacomb(floor)
        this@CatacombsManager.catacomb = catacomb

        val maxChunkX = -12 + (catacomb.size.boundaryX * 2)
        val maxChunkZ = -12 + (catacomb.size.boundaryY * 2)
        McClient.runNextTick {
            for (x in -12..maxChunkX) {
                for (y in -12..maxChunkZ) {
                    val chunk = McLevel.level.getChunk(x, y, ChunkStatus.FULL, false) ?: continue
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
    }

    @Subscription(ServerChangeEvent::class)
    fun reset() {
        val instance = catacomb ?: return
        CatacombLeaveEvent(instance).post()
        catacomb = null
    }


    @Subscription
    fun command(event: RegisterCommandsEvent) {
        event.registerWithCallback("mortem dev column_hash") {
            val chunkPos = McPlayer.self!!.chunkPosition()
            val chunk = McLevel.self.getChunk(chunkPos.x, chunkPos.z)
            val hash = CatacombWorldMatcher.hashColumn(chunk, McPlayer.self!!.blockPosition().atY(255))
            Text.of("Hash for current position is ") {
                append(hash) {
                    color = CatppuccinColors.Mocha.pink
                }
                color = CatppuccinColors.Mocha.lavender
            }.sendWithPrefix()
        }
    }

    private val defaultPath: Path = McClient.config.resolve("mortem/data")

    @Subscription(TickEvent::class)
    @TimePassed("5s")
    fun saveAll() {
        val rooms = defaultPath.resolve("rooms").createDirectories()
        this.backingRooms.values.forEach {
            if (!it.shouldSerialize) return@forEach
            it.shouldSerialize = false
            rooms.resolve("${it.id}.json").writeText(it.toJsonOrThrow(MortemCodecs.getCodec()).toPrettyString())
        }
    }

    fun worldPosToGridPos(pos: BlockPos): Vector2i {
        val chunkX = floor(pos.x / 16f).toInt()
        val chunkY = floor(pos.z / 16f).toInt()
        val chunkRelativeX = pos.x and 15
        val chunkRelativeY = pos.z and 15

        val isHallwayX = abs(chunkX) % 2 == 1
        val isHallwayY = abs(chunkY) % 2 == 1
        val baseGridPosX = chunkX + 12
        val baseGridPosY = chunkY + 12

        val gridPosX = when {
            isHallwayX && chunkRelativeX > 7 -> baseGridPosX + 1
            isHallwayX && chunkRelativeX < 7 -> baseGridPosX - 1
            else -> baseGridPosX
        }

        val gridPosY = when {
            isHallwayY && chunkRelativeY > 7 -> baseGridPosY + 1
            isHallwayY && chunkRelativeY < 7 -> baseGridPosY - 1
            else -> baseGridPosY
        }

        return Vector2i(gridPosX, gridPosY)
    }

}
