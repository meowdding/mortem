package me.owdding.mortem.utils

import me.owdding.ktmodules.Module
import me.owdding.mortem.core.event.ChunkEvent
import me.owdding.mortem.utils.Utils.post
import me.owdding.mortem.utils.extensions.sectionPos
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.ChunkPos
import org.spongepowered.asm.mixin.Mixin
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.level.PacketEvent
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import kotlin.reflect.KClass

@Module
object FmlHandler {
    init {
        ClientChunkEvents.CHUNK_LOAD.register { level, chunk -> ChunkEvent.ChunkLoadEvent(level, chunk).post(SkyBlockAPI.eventBus) }
        ClientChunkEvents.CHUNK_UNLOAD.register { level, chunk -> ChunkEvent.ChunkUnloadEvent(level, chunk).post(SkyBlockAPI.eventBus) }
    }

    @Subscription
    fun onChunkUpdate(event: PacketEvent) {
        val chunkPos: ChunkPos
        when (val packet = event.packet) {
            is ClientboundSectionBlocksUpdatePacket -> {
                chunkPos = packet.sectionPos.chunk()
            }
            is ClientboundBlockUpdatePacket -> {
                chunkPos = ChunkPos(packet.pos.x, packet.pos.y)
            }
            else -> return
        }

        runCatching {
            val level = McLevel.self ?: return
            val chunk = level.getChunk(chunkPos.x, chunkPos.z)
            ChunkEvent.ChunkUpdateEvent(level, chunk).post()
        }
    }
}
