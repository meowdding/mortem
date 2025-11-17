package me.owdding.mortem.core.catacombs

import me.owdding.ktmodules.Module
import me.owdding.lib.extensions.shorten
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.catacombs.nodes.RoomNode
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
import java.util.LinkedList
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.math.abs
import kotlin.math.floor
import me.owdding.mortem.core.event.catacomb.CatacombNodeChangeEvent
import me.owdding.mortem.core.event.catacomb.CatacombRoomChangeEvent
import tech.thatgravyboat.skyblockapi.utils.extentions.filterKeysNotNull
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick

@Module
object CatacombsManager {

    val backingRooms: MutableMap<String, StoredCatacombRoom> = mutableMapOf()

    init {
        val list: List<StoredCatacombRoom> = Utils.loadRepoData("rooms", CodecUtils::list)
        backingRooms.putAll(list.associateBy { it.centerHash })

        val actualRooms = setOf(
            "Entry",
            "Blood Room",
            "Fairy Room",
            "Ice Path",
            "Teleport Maze",
            "Ice Fill",
            "Creeper Beams",
            "Old Trap",
            "New Trap",
            "Midas",
            "Shadow Assassin",
            "Higher Blaze",
            "Lower Blaze",
            "Tic Tac Toe",
            "Tic Tac Toe",
            "Three Weirdos",
            "Boulder",
            "Water Board",
            "Quiz",
            "Dragon Miniboss",
            "Default",
            "Zodd",
            "Diagonal",
            "Quartz Knight",
            "Doors",
            "Cages",
            "Long Hall",
            "Grand Library",
            "Arrow Trap",
            "Skull",
            "Scaffolding",
            "Red Green",
            "Jumping Skulls",
            "Redstone Warrior",
            "Perch",
            "Sloth",
            "Purple Flags",
            "Painting",
            "Altar",
            "Grass Ruin",
            "Cathedral",
            "Basement",
            "Chambers",
            "Double Diamond",
            "Gravel",
            "Raccoon",
            "Hallway",
            "Golden Oasis",
            "Stairs",
            "Balcony",
            "Dip",
            "Dino Site",
            "Hall",
            "Mural",
            "Banners",
            "Pressure Plates",
            "Big Red Flag",
            "Melon",
            "Beams",
            "Red Blue",
            "Redstone Crypt",
            "Museum",
            "Redstone Key",
            "Market",
            "Steps",
            "Overgrown Chains",
            "Drawbridge",
            "Drop",
            "Chains",
            "Cage",
            "Spider",
            "Leaves",
            "Mines",
            "Spikes",
            "Double Stair",
            "Layers",
            "Crypt",
            "Flags",
            "Cell",
            "Catwalk",
            "Overgrown",
            "Sewer",
            "Admin",
            "Mossy",
            "Pedestal",
            "Mushroom",
            "Duncan",
            "Bridges",
            "Andesite",
            "Mage",
            "Small Waterfall",
            "Supertall",
            "Blue Skulls",
            "Temple",
            "Lava Skulls",
            "Well",
            "Knight",
            "Buttons",
            "Dome",
            "Deathmite",
            "Archway",
            "Lava Ravine",
            "Tomioka",
            "Rails",
            "Locked Away",
            "Sword",
            "Gold Mine",
            "Granite",
            "End",
            "Atlas",
            "Wizard",
            "Slime",
            "Waterfall",
            "Prison Cell",
            "Small Stairs",
            "Slabs",
            "Water",
            "Lots of Floors",
            "Carpets",
            "Withermancer",
            "Pit",
            "1x1 skulls",
            "Dueces",
            "Mirror",
            "Sarcophagus",
            "Logs",
            "Cobble Wall Pillar",
            "Criss Cross",
            "Black Flag",
            "Multicolored",
            "Quad Lava",
            "Admin 2",
            "Four Banner",
        )

        val rooms = backingRooms.map { it.value.name.lowercase() }
        actualRooms.filterNot { rooms.contains(it.lowercase()) }.forEach { Mortem.error("Missing room $it") }
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

    @Subscription(ServerChangeEvent::class, ServerDisconnectEvent::class)
    fun reset() {
        val instance = catacomb ?: return
        CatacombLeaveEvent(instance).post()
        catacomb = null
    }


    @Subscription
    fun command(event: RegisterCommandsEvent) {
        event.registerWithCallback("mortem dev column_hash") {
            val chunkPos = McPlayer.self!!.chunkPosition()
            val hash = CatacombWorldMatcher.hashColumn(McPlayer.self!!.blockPosition().atY(255))
            Text.of("Hash for current position is ") {
                append(hash) {
                    color = CatppuccinColors.Mocha.pink
                }
                color = CatppuccinColors.Mocha.lavender
            }.sendWithPrefix()
        }
        event.registerWithCallback("mortem dev create_full_hash") {
            val hashes = LinkedList<Pair<String, List<String>>>()

            val directions = CatacombWorldMatcher.createDirectionalHashes(McPlayer.self!!.blockPosition().atY(255)) { hash, blocks -> hashes.add(hash to blocks) }.filterKeysNotNull().values

            var sinceLastCool = 0
            hashes.forEach {
                Text.of("$sinceLastCool ${it.first}") {
                    color = when (sinceLastCool) {
                        0 -> CatppuccinColors.Mocha.yellow
                        1 -> CatppuccinColors.Mocha.green
                        2 -> CatppuccinColors.Mocha.teal
                        3 -> CatppuccinColors.Mocha.sky
                        4 -> CatppuccinColors.Mocha.sapphire
                        5 -> CatppuccinColors.Mocha.blue
                        6 -> CatppuccinColors.Mocha.lavender
                        7 -> CatppuccinColors.Mocha.text
                        8 -> CatppuccinColors.Mocha.subtext0
                        9 -> CatppuccinColors.Mocha.subtext1
                        else -> CatppuccinColors.Frappe.red
                    }
                    sinceLastCool++
                    if (it.first in directions) sinceLastCool = 0
                    onClick {
                        McClient.clipboard = it.second.joinToString(separator = "\n")
                    }
                }.sendWithPrefix()
            }
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
            rooms.resolve("${it.id}.json").writeText(it.toJsonOrThrow(MortemCodecs.getCodec()).toPrettyString())
        }
    }

    fun worldPosToGridPos(pos: BlockPos): Vector2i = worldPosToGridPos(Vector2i(pos.x, pos.z))

    fun worldPosToGridPos(pos: Vector2i): Vector2i {
        val chunkX = floor(pos.x / 16f).toInt()
        val chunkY = floor(pos.y / 16f).toInt()
        val chunkRelativeX = pos.x and 15
        val chunkRelativeY = pos.y and 15

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
