package me.owdding.mortem.features.comands

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.owdding.ktmodules.Module
import me.owdding.lib.events.FinishRepoLoadingEvent
import me.owdding.mortem.config.category.CommandsConfig
import me.owdding.repo.RemoteRepo
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.json.Json.toData

@Module
object JoinCommand {

    private val dungeonsCommands = mutableMapOf<DungeonFloor, String>()
    //private val kuudraCommands = mutableMapOf<KuudraTier, String>()

    private val DUNGEON_FLOOR_CODEC: Codec<DungeonFloor> = Codec.STRING.xmap(
        { name -> DungeonFloor.getByName(name) },
        { floor -> floor?.name },
    )
    //private val KUUDRA_TIER_CODEC: Codec<KuudraTier> = Codec.STRING.xmap(
    //    { name -> KuudraTier.getByName(name) },
    //    { tier -> tier?.name },
    //)

    private val CODEC: Codec<Pair<Map<DungeonFloor, String>, Map<DungeonFloor, String>>> = RecordCodecBuilder.create {
        it.group(
            Codec.unboundedMap(DUNGEON_FLOOR_CODEC, Codec.STRING).fieldOf("dungeons").forGetter { it.first },
            Codec.unboundedMap(DUNGEON_FLOOR_CODEC, Codec.STRING).fieldOf("dungeons").forGetter { it.second },
            //Codec.unboundedMap(KUUDRA_TIER_CODEC, Codec.STRING).fieldOf("kuudra").forGetter { it.second },
        ).apply(it) { a, b ->
            dungeonsCommands.apply {
                clear()
                putAll(a)
            }
            //kuudraCommands.apply {
            //    clear()
            //    putAll(b)
            //}
            a to b
        }
    }

    @Subscription
    fun onCommand(event: RegisterCommandsEvent) {
        if (!CommandsConfig.join) return

        dungeonsCommands.forEach { (dungeon, command) ->
            event.registerWithCallback("join${dungeon.name.lowercase()}") {
                McClient.sendCommand("joininstance $command")
            }
        }
        // kuudraCommands.forEach { (tier, command) ->
        //     event.registerWithCallback("joinkuudra${tier.name.lowercase()}") {
        //         McClient.sendCommand("joininstance $command")
        //     }
        // }
    }

    @Subscription
    fun onRepo(event: FinishRepoLoadingEvent) {
        RemoteRepo.getFileContentAsJson("dungeons/joincommands.json")?.toData(CODEC)
    }

}
