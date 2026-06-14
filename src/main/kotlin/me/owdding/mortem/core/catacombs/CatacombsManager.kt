package me.owdding.mortem.core.catacombs

import me.owdding.ktmodules.Module
import me.owdding.lib.extensions.floor
import me.owdding.lib.extensions.shorten
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.roommatching.CatacombMapMatcher
import me.owdding.mortem.core.catacombs.roommatching.CatacombWorldMatcher
import me.owdding.mortem.core.catacombs.types.CatacombClass
import me.owdding.mortem.core.catacombs.types.StoredCatacombRoom
import me.owdding.mortem.core.event.CatacombJoinEvent
import me.owdding.mortem.core.event.CatacombLeaveEvent
import me.owdding.mortem.core.event.catacomb.CatacombNodeChangeEvent
import me.owdding.mortem.core.event.catacomb.CatacombRoomChangeEvent
import me.owdding.mortem.core.event.catacomb.CatacombsEndEvent
import me.owdding.mortem.generated.CodecUtils
import me.owdding.mortem.generated.MortemCodecs
import me.owdding.mortem.utils.Utils
import me.owdding.mortem.utils.Utils.post
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.extensions.endText
import me.owdding.mortem.utils.extensions.sendWithPrefix
import me.owdding.mortem.utils.extensions.toVector3dc
import me.owdding.mortem.utils.getFloatOrNull
import me.owdding.mortem.utils.getIntOrNull
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3d
import org.joml.Vector3dc
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.TimePassed
import tech.thatgravyboat.skyblockapi.api.events.chat.ChatReceivedEvent
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
import tech.thatgravyboat.skyblockapi.utils.extentions.parseDuration
import tech.thatgravyboat.skyblockapi.utils.json.Json.toJsonOrThrow
import tech.thatgravyboat.skyblockapi.utils.json.Json.toPrettyString
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
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
        eventFired = false
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

    /**
     * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     *                  Master Mode The Catacombs - Floor I
     *
     *                             Team Score: 174 (B)
     *                       ☠ Defeated Bonzo in 03m 55s
     *                              > EXTRA STATS <
     *                                    +20 Bits
     *                     +43,909.1 Catacombs Experience
     *                       +40,665.4 Berserk Experience
     * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     * extra: (/showextrastats)
     * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     *              Master Mode The Catacombs - Floor I Stats
     *
     *                             Team Score: 174 (B)
     *                       ☠ Defeated Bonzo in 03m 55s
     *
     *      Total Damage as Berserk: 1,048,511,251 (NEW RECORD!)
     *                   Enemies Killed: 106 (NEW RECORD!)
     *                                    Deaths: 0
     *                               Secrets Found: 0
     *
     * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */

    private val delimiterRegex = Regex("^▬{64}$")
    private val teamScoreRegex = Regex("^Team Score: (?<score>\\d{1,3}) \\((?<rating>.{1,2})\\)\\s*(\\(NEW RECORD!\\))?$")
    private val timeRegex = Regex("^☠ Defeated (?<boss>.+) in (?<time>.+)\\s*(\\(NEW RECORD!\\))?$")
    private val extraStatsRegex = Regex("^ {29}> EXTRA STATS <$")
    private val bitsRegex = Regex("^\\+(?<bits>[\\d,]+) Bits$")
    private val expRegex = Regex("^\\+(?<exp>[\\d,.]+) (?<type>\\w+) Experience$")
    private val damageRegex = Regex("^Total Damage as (?<class>\\w+): (?<damage>[\\d,.]+)\\s*(\\(NEW RECORD!\\))?$")
    private val enemiesKilledRegex = Regex("^Enemies Killed: (?<kills>[\\d,]+)\\s*(\\(NEW RECORD!\\))?$")
    private val personalSecretsFoundRegex = Regex("^Secrets Found: (?<secrets>[\\d,]+)$")

    private var inEndText = false
    private var waitingForHeader = false

    private var cachedEndData: CatacombEndData? = null
    private var eventFired = false

    @Subscription
    fun chatDetectEnd(event: ChatReceivedEvent.Pre) {
        if (eventFired) return
        val catacomb = catacomb ?: return
        val message = event.text.trim()

        if (delimiterRegex.matches(message)) {
            if (!inEndText) {
                waitingForHeader = true
            } else {
                inEndText = false
                waitingForHeader = false

                val cache = cachedEndData ?: return
                if (cache.ableToPost && !eventFired) {
                    eventFired = true
                    CatacombsEndEvent(cache).post()
                    cachedEndData = null
                }
            }
            return
        }

        if (waitingForHeader) {
            waitingForHeader = false

            if (message.startsWith(catacomb.floor.endText)) {
                val ownClass = catacomb.playerMap[McPlayer.uuid]?.dungeonClass ?: return

                inEndText = true
                if (!message.endsWith("Stats")) cachedEndData = CatacombEndData(catacomb.floor, catacombClass = ownClass)
            }

            return
        }

        if (!inEndText) return

        matchWhen(message) {
            case(teamScoreRegex, "score") { destructed ->
                val score = destructed["score"]?.toIntOrNull() ?: return@case
                cachedEndData?.score = score
            }
            case(timeRegex, "time") { destructed ->
                val time = destructed["time"].parseDuration() ?: return@case
                cachedEndData?.time = time
            }
            case(extraStatsRegex) { McClient.sendCommand("/showextrastats") }
            case(bitsRegex, "bits") { destructured ->
                val bits = destructured["bits"]?.getIntOrNull() ?: return@case
                cachedEndData?.bits = bits
            }
            case(expRegex, "exp", "type") { destructed ->
                val exp = destructed["exp"]?.getFloatOrNull() ?: return@case
                val type = destructed["type"] ?: return@case
                when (type) {
                    "Catacombs" -> {
                        cachedEndData?.catacombsExperience = exp
                    }
                    else -> {
                        val catacombClass = CatacombClass.byName(type) ?: return@case
                        cachedEndData?.classExperience?.put(catacombClass, exp)
                    }
                }
            }
            case(damageRegex, "class", "damage") { destructed ->
                val catacombClass = destructed["class"]?.let { CatacombClass.byName(it) } ?: return@case
                val damage = destructed["damage"]?.getIntOrNull() ?: return@case
                cachedEndData?.damageDealt = damage.toLong()
            }
            case(enemiesKilledRegex, "kills") {destructured ->
                val kills = destructured["kills"]?.getIntOrNull() ?: return@case
                cachedEndData?.enemiesKilled = kills
            }
            case(personalSecretsFoundRegex, "secrets") {destructed ->
                val found = destructed["secrets"]?.getIntOrNull() ?: return@case
                cachedEndData?.secretsFound = found
            }
        }
    }

    @Subscription
    fun onChatPost(event: ChatReceivedEvent.Post) {
        val catacomb = catacomb ?: return
        // TODO: cancel the messages if config option enabled
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
