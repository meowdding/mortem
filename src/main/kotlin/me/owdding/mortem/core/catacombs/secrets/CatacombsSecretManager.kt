package me.owdding.mortem.core.catacombs.secrets

import me.owdding.ktmodules.Module
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.event.CatacombJoinEvent
import me.owdding.mortem.generated.CodecUtils
import me.owdding.mortem.generated.MortemCodecs
import me.owdding.mortem.utils.Utils
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.TimePassed
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.json.Json.toJsonOrThrow
import tech.thatgravyboat.skyblockapi.utils.json.Json.toPrettyString
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@Module
object CatacombsSecretManager {

    val backingRooms: MutableMap<String, CatacombsSecretRoom> = mutableMapOf()

    fun getOrPut(roomId: String) = backingRooms.getOrPut(roomId) { CatacombsSecretRoom(roomId) }

    init {
        val rooms: List<CatacombsSecretRoom> = Utils.loadRepoData("secrets", CodecUtils::list)
        backingRooms.putAll(rooms.associateBy { it.id })

        val roomsWithSecrets = CatacombsManager.backingRooms.values.associateBy { it.id }.mapValues { it.value.secrets }.filterValues { it > 0 }
        val missingRooms = roomsWithSecrets.filter { (roomId, secrets) ->
            val mappedGroups = backingRooms[roomId]?.secretGroups ?: return@filter true
            val mappedSecrets = mappedGroups.flatMap { it.secrets }.count { it.type.isSecret }
            secrets != mappedSecrets
        }
        if (missingRooms.isNotEmpty()) {
            Mortem.error("Missing secrets for rooms: ${missingRooms.keys.joinToString()}")
        }
    }

    private val defaultPath: Path = McClient.config.resolve("mortem/data")

    @Subscription(TickEvent::class)
    @TimePassed("5s")
    fun onTick() {
        val rooms = defaultPath.resolve("secrets").createDirectories()
        this.backingRooms.values.forEach {
            if (!it.needsUpdate) return@forEach
            it.needsUpdate = false
            rooms.resolve("${it.id}.json").writeText(it.toJsonOrThrow(MortemCodecs.getCodec()).toPrettyString())
        }
    }


    @Subscription(CatacombJoinEvent::class)
    fun resetClicked() = backingRooms.values.forEach(CatacombsSecretRoom::reset)

}
