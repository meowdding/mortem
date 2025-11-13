package me.owdding.mortem.core.catacombs.secrets

import com.mojang.serialization.Codec
import me.owdding.ktmodules.Module
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.event.CatacombJoinEvent
import me.owdding.mortem.generated.CodecUtils
import me.owdding.mortem.utils.Utils
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription

@Module
object CatacombsSecretManager {

    val backingRooms: MutableMap<String, CatacombsSecretRoom> = mutableMapOf()

    init {
        val rooms = Utils.loadRepoData<CatacombsSecretRoom, MutableMap<String, CatacombsSecretRoom>>("secrets") { CodecUtils.map(Codec.STRING, it) }
        backingRooms.putAll(rooms)

        val roomsWithSecrets = CatacombsManager.backingRooms.mapValues { it.value.secrets }.filterValues { it > 0 }
        val missingRooms = roomsWithSecrets.filterNot { (roomId, secrets) ->
            val mappedGroups = backingRooms[roomId]?.secretGroups ?: return@filterNot true
            val mappedSecrets = mappedGroups.flatMap { it.secrets }.count { it.type.isSecret }
            secrets != mappedSecrets
        }
        if (missingRooms.isNotEmpty()) {
            Mortem.error("Missing secrets for rooms: ${missingRooms.keys.joinToString()}")
        }
    }

    @Subscription(CatacombJoinEvent::class)
    fun resetClicked() = backingRooms.values.forEach(CatacombsSecretRoom::reset)

}
